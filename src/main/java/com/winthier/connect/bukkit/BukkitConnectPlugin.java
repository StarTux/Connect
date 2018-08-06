package com.winthier.connect.bukkit;

import com.winthier.connect.Client;
import com.winthier.connect.Connect;
import com.winthier.connect.ConnectHandler;
import com.winthier.connect.Message;
import com.winthier.connect.OnlinePlayer;
import com.winthier.connect.ServerConnection;
import com.winthier.connect.bukkit.event.ConnectClientConnectEvent;
import com.winthier.connect.bukkit.event.ConnectClientDisconnectEvent;
import com.winthier.connect.bukkit.event.ConnectMessageEvent;
import com.winthier.connect.bukkit.event.ConnectRemoteCommandEvent;
import com.winthier.connect.bukkit.event.ConnectServerConnectEvent;
import com.winthier.connect.bukkit.event.ConnectServerDisconnectEvent;
import com.winthier.connect.packet.PlayerList;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONValue;

@Getter
public final class BukkitConnectPlugin extends JavaPlugin implements ConnectHandler, Listener {
    private Connect connect = null;
    private final Map<UUID, String> debugPlayers = new HashMap<>();

    // JavaPlugin

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        startConnect();
        new BukkitRunnable() {
            @Override public void run() {
                connect.pingAll();
            }
        }.runTaskTimer(this, 200, 200);
        getCommand("connect").setExecutor(new BukkitConnectCommand(this));
        final BukkitRemoteCommand remoteCommand = new BukkitRemoteCommand(this);
        getCommand("remote").setExecutor(remoteCommand);
        getCommand("game").setExecutor(remoteCommand);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
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
                syncHandleMessage(message);
            }
        }.runTask(this);
    }

    private void syncHandleMessage(Message message) {
        // Debug Players
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            if (entry.getValue().equals(message.getChannel())) {
                Player player = getServer().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(String.format(ChatColor.translateAlternateColorCodes('&', "[&7C&r] &8from(&r%s&8) to(&r%s&8) payload(&r%s&8)"), message.getFrom(), message.getTo(), message.getPayload()));
                }
            }
        }
        handleSpecialMessages(message);
        // EventHandler
        getServer().getPluginManager().callEvent(new ConnectMessageEvent(message));
    }

    private void handleSpecialMessages(Message message) {
        switch (message.getChannel()) {
        case "DEBUG":
            getLogger().info(String.format("Debug message from=%s to=%s payload=%s", message.getFrom(), message.getTo(), message.getPayload()));
            break;
        case "PLAYER_MESSAGE":
            if (message.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> map = (Map<String, Object>)message.getPayload();
                try {
                    String target = (String)map.get("target");
                    if (target == null) return;
                    Player player;
                    try {
                        UUID uuid = UUID.fromString(target);
                        player = getServer().getPlayer(uuid);
                    } catch (IllegalArgumentException iae) {
                        player = getServer().getPlayerExact(target);
                    }
                    if (player == null) return;
                    Object chat = (Object)map.get("chat");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "minecraft:tellraw " + player.getName() + " " + JSONValue.toJSONString(chat));
                } catch (RuntimeException re) {
                    re.printStackTrace();
                }
            }
            break;
        case "SEND_PLAYER_SERVER":
            if (message.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, String> map = (Map<String, String>)message.getPayload();
                String playerName = map.get("player");
                String serverName = map.get("server");
                if (playerName == null || serverName == null) return;
                if (serverName.equals(connect.getName())) return;
                Player player;
                try {
                    UUID uuid = UUID.fromString(playerName);
                    player = getServer().getPlayer(uuid);
                } catch (IllegalArgumentException iae) {
                    player = getServer().getPlayerExact(playerName);
                }
                if (player != null) {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    try {
                        out.writeUTF("Connect");
                        out.writeUTF(serverName);
                        player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            break;
        default:
            break;
        }
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
        event.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        connect.broadcastPlayerStatus(onlinePlayer(event.getPlayer()), false);
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        connect.broadcastPlayerStatus(onlinePlayer(event.getPlayer()), false);
        event.setLeaveMessage(null);
    }

    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        Message message = event.getMessage();
        if (message.getChannel().equals("BUNGEE_PLAYER_JOIN")) {
            Map<String, Object> map = (Map<String, Object>) message.getPayload();
            String name = (String)map.get("name");
            for (Player player: getServer().getOnlinePlayers()) {
                player.sendMessage(ChatColor.GRAY + name + " joined the game");
            }
        } else if (message.getChannel().equals("BUNGEE_PLAYER_QUIT")) {
            Map<String, Object> map = (Map<String, Object>) message.getPayload();
            String name = (String)map.get("name");
            for (Player player: getServer().getOnlinePlayers()) {
                player.sendMessage(ChatColor.GRAY + name + " left the game");
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
