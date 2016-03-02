package com.winthier.connect.bukkit;

import com.winthier.connect.*;
import com.winthier.connect.bukkit.event.*;
import com.winthier.connect.packet.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
        getCommand("connect").setExecutor(new BukkitConnectCommand(this));
        getCommand("remote").setExecutor(new BukkitRemoteCommand(this));
    }

    @Override
    public void onDisable() {
        stopConnect();
    }

    OnlinePlayer onlinePlayer(Player player) {
        return new OnlinePlayer(player.getUniqueId(), player.getName());
    }

    List<OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> result = new ArrayList<>();
        for (Player player: getServer().getOnlinePlayers()) {
            result.add(onlinePlayer(player));
        }
        return result;
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

    @Override
    public void handleRemoteCommand(OnlinePlayer sender, String server, String[] args) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteCommandEvent(sender, server, args));
            }
        }.runTask(this);
    }
    
    // Event

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        connect.broadcastPlayerStatus(onlinePlayer(event.getPlayer()), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        connect.broadcastPlayerStatus(onlinePlayer(event.getPlayer()), false);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        connect.broadcastPlayerStatus(onlinePlayer(event.getPlayer()), false);
    }

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
        getLogger().info("Server Connect: " + event.getConnection().getName());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Server Connect: ") + event.getConnection().getName());
            }
        }
    }

    @EventHandler
    public void onConnectServerDisconnect(ConnectServerDisconnectEvent event) {
        getLogger().info("Server Disconnect: " + event.getConnection().getName());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Server Disconnect: ") + event.getConnection().getName());
            }
        }
    }

    @EventHandler
    public void onConnectClientConnect(ConnectClientConnectEvent event) {
        connect.send(event.getClient().getName(), "Connect", PlayerList.Type.LIST.playerList(onlinePlayers()).serialize());
        getLogger().info("Client Connect: " + event.getClient().getName());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Client Connect: ") + event.getClient().getName());
            }
        }
    }

    @EventHandler
    public void onConnectClientDisconnect(ConnectClientDisconnectEvent event) {
        getLogger().info("Client Disconnect: " + event.getClient().getName());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Client Disconnect: ") + event.getClient().getName());
            }
        }
    }
}
