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
        getCommand("connect").setExecutor(new ConnectCommand(this));
        final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor(this);
        getCommand("remote").setExecutor(remoteCommandExecutor);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        this.connect.updatePlayerList(onlinePlayers());
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
        this.debug = getConfig().getBoolean("Debug");
        this.connect = new Connect(serverName, this);
        getServer().getScheduler().runTaskAsynchronously(this, this.connect);
    }

    void stopConnect() {
        if (this.connect == null) return;
        this.connect.stop();
        this.connect = null;
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
                    getServer().dispatchCommand(getServer().getConsoleSender(), "minecraft:tellraw " + player.getName() + " " + gson.toJson(chat));
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
                if (serverName.equals(this.connect.getServerName())) return;
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
        final List<OnlinePlayer> list = onlinePlayers();
        getServer().getScheduler().runTaskAsynchronously(this, () -> this.connect.updatePlayerList(list));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> this.connect.updatePlayerList(list));
            });
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> this.connect.updatePlayerList(list));
            });
    }

    @EventHandler
    public void onConnectRemoteConnect(ConnectRemoteConnectEvent event) {
        getLogger().info("Remote Connect: " + event.getRemote());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Remote Connect: ") + event.getRemote());
            }
        }
    }

    @EventHandler
    public void onConnectRemoteDisconnect(ConnectRemoteDisconnectEvent event) {
        getLogger().info("Remote Disconnect: " + event.getRemote());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&7C&r] Remote Disconnect: ") + event.getRemote());
            }
        }
    }
}
