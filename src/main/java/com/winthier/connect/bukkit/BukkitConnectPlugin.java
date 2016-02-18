package com.winthier.connect.bukkit;

import com.winthier.connect.*;
import com.winthier.connect.bukkit.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitConnectPlugin extends JavaPlugin implements ConnectHandler, Listener {
    Connect connect = null;
    final Map<UUID, String> debugPlayers = new HashMap<>();

    // JavaPlugin

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        startConnect();
        new BukkitRunnable() {
            @Override public void run() {
                connect.pingAllConnected();
            }
        }.runTaskTimer(this, 200, 200);
    }

    @Override
    public void onDisable() {
        stopConnect();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        String firstArg = args.length > 0 ? args[0] : null;
        if (firstArg == null) {
            return false;
        } else if ("status".equals(firstArg) && args.length == 1) {
            Server server = connect.getServer();
            if (server != null) {
                ConnectionStatus serverStatus = server.getStatus();
                sender.sendMessage("Server " + server.getName() + " (" + server.getPort() + ") " + color(serverStatus) + serverStatus.name().toLowerCase());
                for (ServerConnection connection: server.getConnections()) {
                    ConnectionStatus status = connection.getStatus();
                    sender.sendMessage("- " + connection.getName() + " (" + connection.getPort() + ") " + color(status) + status.name().toLowerCase());
                }
            }
            sender.sendMessage("Clients");
            for (Client client: connect.getClients()) {
                ConnectionStatus status = client.getStatus();
                sender.sendMessage("- " + client.getName() + " (" + client.getPort() + ") " + color(status) + status.name().toLowerCase());
            }
        } else if ("reload".equals(firstArg) && args.length == 1) {
            reloadConfig();
            stopConnect();
            startConnect();
            sender.sendMessage("Configuration reloaded");
        } else if ("ping".equals(firstArg) && args.length == 1) {
            connect.broadcastAll("Connect", "Ping");
        } else if ("debug".equals(firstArg) && player != null) {
            if (args.length == 1) {
                debugPlayers.remove(player.getUniqueId());
                player.sendMessage("Debug mode disabled");
            } else if (args.length == 2) {
                String chan = args[1];
                debugPlayers.put(player.getUniqueId(), chan);
                player.sendMessage("Debugging Connect channel " + chan);
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    // Util

    ChatColor color(ConnectionStatus status) {
        switch (status) {
        case INIT: return ChatColor.GRAY;
        case CONNECTED: return ChatColor.GREEN;
        case DISCONNECTED: return ChatColor.RED;
        case STOPPED: return ChatColor.YELLOW;
        default: return ChatColor.WHITE;
        }
    }

    // Connect

    void startConnect() {
        stopConnect();
        String serverName = getConfig().getString("ServerName", "test");
        String path = getConfig().getString("ServerConfig", "/home/mc/public/config/Connect/servers.txt");
        File serverFile = path.startsWith("/") ? new File(path) : new File(getDataFolder(), path);
        connect = new Connect(serverName, serverFile, this);
        connect.start();
    }

    void stopConnect() {
        if (connect == null) return;
        connect.stop();
        connect = null;
    }

    // ConnectHandler

    @Override
    public void runThread(final Runnable runnable) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void handleClientConnect(Client client) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectClientConnectEvent(client));
            }
        }.runTask(this);
    }
    
    @Override
    public void handleClientDisconnect(Client client) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectClientDisconnectEvent(client));
            }
        }.runTask(this);
    }

    @Override
    public void handleServerConnect(ServerConnection connection) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectServerConnectEvent(connection));
            }
        }.runTask(this);
    }
    
    @Override
    public void handleServerDisconnect(ServerConnection connection) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectServerDisconnectEvent(connection));
            }
        }.runTask(this);
    }

    @Override
    public void handleMessage(Message message) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectMessageEvent(message));
            }
        }.runTask(this);
    }

    // Event

    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            if (entry.getValue().equals(event.getMessage().getChannel())) {
                Player player = getServer().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(String.format(ChatColor.translateAlternateColorCodes('&', "[&7C&r] &8from(&r%s&8) to(&r%s&8) payload(&r%s&8)"), event.getMessage().getFrom(), event.getMessage().getTo(), event.getMessage().getPayload()));
                }
            }
        }
    }

    @EventHandler
    public void onConnectServerConnect(ConnectServerConnectEvent event) {
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage("Server Connect: " + event.getConnection().getName());
            }
        }
    }

    @EventHandler
    public void onConnectServerDisconnect(ConnectServerDisconnectEvent event) {
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage("Server Disconnect: " + event.getConnection().getName());
            }
        }
    }

    @EventHandler
    public void onConnectClientConnect(ConnectClientConnectEvent event) {
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage("Client Connect: " + event.getClient().getName());
            }
        }
    }

    @EventHandler
    public void onConnectClientDisconnect(ConnectClientDisconnectEvent event) {
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage("Client Disconnect: " + event.getClient().getName());
            }
        }
    }
}
