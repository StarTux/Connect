package com.winthier.connect;

import com.google.gson.Gson;
import com.winthier.connect.event.ConnectMessageEvent;
import com.winthier.connect.event.ConnectRemoteCommandEvent;
import com.winthier.connect.event.ConnectRemoteConnectEvent;
import com.winthier.connect.event.ConnectRemoteDisconnectEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class ConnectPlugin extends JavaPlugin implements ConnectHandler, Listener {
    private Connect connect = null;
    private final Map<UUID, String> debugPlayers = new HashMap<>();
    @Setter private boolean debug = false;
    private Gson gson = new Gson();

    // JavaPlugin

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startConnect();
        new ConnectCommand(this).enable();
        final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor(this);
        getCommand("remote").setExecutor(remoteCommandExecutor);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        connect.updatePlayerList(onlinePlayers());
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
        reloadConfig();
        String serverName = getConfig().getString("ServerName");
        debug = getConfig().getBoolean("Debug");
        connect = new Connect(serverName, this);
        getServer().getScheduler().runTaskAsynchronously(this, connect);
    }

    void stopConnect() {
        if (connect == null) return;
        connect.stop();
        connect = null;
    }

    // ConnectHandler

    @Override
    public void handleRemoteConnect(String remote) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteConnectEvent(remote));
            }
        }.runTask(this);
    }

    @Override
    public void handleRemoteDisconnect(String remote) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteDisconnectEvent(remote));
            }
        }.runTask(this);
    }

    @Override
    public void handleMessage(Message message) {
        if (!isEnabled()) return;
        if (debug) getLogger().info("Message received: " + message.serialize());
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
                    String msg = format("[&7C&r] &8from(&r%s&8) to(&r%s&8) payload(&r%s&8)",
                                        message.getFrom(), message.getTo(), message.getPayload());
                    player.sendMessage(msg);
                }
            }
        }
        handleSpecialMessages(message);
        getServer().getPluginManager().callEvent(new ConnectMessageEvent(message));
    }

    private void handleSpecialMessages(Message message) {
        switch (message.getChannel()) {
        case "DEBUG":
            getLogger().info(String.format("Debug message from=%s to=%s payload=%s",
                                           message.getFrom(), message.getTo(),
                                           message.getPayload()));
            break;
        case "PLAYER_MESSAGE":
            if (message.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> map = (Map<String, Object>) message.getPayload();
                try {
                    String target = (String) map.get("target");
                    if (target == null) return;
                    Player player;
                    try {
                        UUID uuid = UUID.fromString(target);
                        player = getServer().getPlayer(uuid);
                    } catch (IllegalArgumentException iae) {
                        player = getServer().getPlayerExact(target);
                    }
                    if (player == null) return;
                    Object chat = (Object) map.get("chat");
                    String cmd = "minecraft:tellraw "
                        + player.getName() + " " + gson.toJson(chat);
                    getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
                } catch (RuntimeException re) {
                    re.printStackTrace();
                }
            }
            break;
        case "SEND_PLAYER_SERVER":
            if (message.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, String> map = (Map<String, String>) message.getPayload();
                String playerName = map.get("player");
                String serverName = map.get("server");
                if (playerName == null || serverName == null) return;
                if (serverName.equals(connect.getServerName())) return;
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
                ConnectRemoteCommandEvent ev = new ConnectRemoteCommandEvent(sender, server, args);
                getServer().getPluginManager().callEvent(ev);
            }
        }.runTask(this);
    }

    // Event

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        final List<OnlinePlayer> list = onlinePlayers();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
                connect.updatePlayerList(list);
            });
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        connect.updatePlayerList(list);
                    });
            });
    }

    @EventHandler
    void onPlayerKick(PlayerKickEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        connect.updatePlayerList(list);
                    });
            });
    }

    @EventHandler
    void onConnectRemoteConnect(ConnectRemoteConnectEvent event) {
        getLogger().info("Remote Connect: " + event.getRemote());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(colorize("[&7C&r] Remote Connect: ") + event.getRemote());
            }
        }
    }

    @EventHandler
    void onConnectRemoteDisconnect(ConnectRemoteDisconnectEvent event) {
        getLogger().info("Remote Disconnect: " + event.getRemote());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(colorize("[&7C&r] Remote Disconnect: ") + event.getRemote());
            }
        }
    }

    @EventHandler
    void onConnectMessage(ConnectMessageEvent event) {
        Message message = event.getMessage();
        switch (message.getChannel()) {
        case "connect:runall": {
            String cmd = message.getPayload().toString();
            getLogger().info("Received runall: " + cmd);
            Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                });
            break;
        }
        default: break;
        }
    }

    // Utility

    private static String colorize(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    private static String format(String str, Object... args) {
        str = ChatColor.translateAlternateColorCodes('&', str);
        return String.format(str, args);
    }
}
