package com.winthier.connect;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.TextFormat;
import com.winthier.connect.event.ConnectMessageEvent;
import com.winthier.connect.event.ConnectRemoteCommandEvent;
import com.winthier.connect.event.ConnectRemoteConnectEvent;
import com.winthier.connect.event.ConnectRemoteDisconnectEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.minidev.json.JSONValue;

@Getter
public final class ConnectPlugin extends PluginBase implements ConnectHandler, Listener {
    private Connect connect = null;
    private final Map<UUID, String> debugPlayers = new HashMap<>();
    @Setter private boolean debug = false;

    // PluginBase

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startConnect();
        ((PluginCommand)getCommand("connect")).setExecutor(new ConnectCommand(this));
        // final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor(this);
        // ((PluginCommand)getCommand("remote")).setExecutor(remoteCommandExecutor);
        // ((PluginCommand)getCommand("game"))).setExecutor(remoteCommandExecutor);
        // getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
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
        for (Player player: getServer().getOnlinePlayers().values()) {
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
        getServer().getScheduler().scheduleTask(this, this.connect, true);
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
        new NukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteConnectEvent(remote));
            }
        }.runTask(this);
    }

    @Override
    public void handleRemoteDisconnect(String remote) {
        if (!isEnabled()) return;
        new NukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteDisconnectEvent(remote));
            }
        }.runTask(this);
    }

    @Override
    public void handleMessage(Message message) {
        if (!isEnabled()) return;
        if (debug) getLogger().info("Message received: " + message.serialize());
        new NukkitRunnable() {
            @Override public void run() {
                syncHandleMessage(message);
            }
        }.runTask(this);
    }

    private void syncHandleMessage(Message message) {
        // Debug Players
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            if (entry.getValue().equals(message.getChannel())) {
                Player player = getServer().getPlayer(entry.getKey()).orElse(null);
                if (player != null) {
                    player.sendMessage(String.format(TextFormat.colorize("[&7C&r] &8from(&r%s&8) to(&r%s&8) payload(&r%s&8)"), message.getFrom(), message.getTo(), message.getPayload()));
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
                        player = getServer().getPlayer(uuid).orElse(null);
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
            // if (message.getPayload() instanceof Map) {
            //     @SuppressWarnings("unchecked")
            //     final Map<String, String> map = (Map<String, String>)message.getPayload();
            //     String playerName = map.get("player");
            //     String serverName = map.get("server");
            //     if (playerName == null || serverName == null) return;
            //     if (serverName.equals(this.connect.getServerName())) return;
            //     Player player;
            //     try {
            //         UUID uuid = UUID.fromString(playerName);
            //         player = getServer().getPlayer(uuid).orElse(null);
            //     } catch (IllegalArgumentException iae) {
            //         player = getServer().getPlayerExact(playerName);
            //     }
            //     if (player != null) {
            //         ByteArrayOutputStream b = new ByteArrayOutputStream();
            //         DataOutputStream out = new DataOutputStream(b);
            //         try {
            //             out.writeUTF("Connect");
            //             out.writeUTF(serverName);
            //             player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            //         } catch (IOException ex) {
            //             ex.printStackTrace();
            //         }
            //     }
            // }
            break;
        default:
            break;
        }
    }

    @Override
    public void handleRemoteCommand(OnlinePlayer sender, String server, String[] args) {
        if (!isEnabled()) return;
        new NukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteCommandEvent(sender, server, args));
            }
        }.runTask(this);
    }

    // Event

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final List<OnlinePlayer> list = onlinePlayers();
        getServer().getScheduler().scheduleTask(this, () -> this.connect.updatePlayerList(list), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        getServer().getScheduler().scheduleTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().scheduleTask(this, () -> this.connect.updatePlayerList(list), true);
            });
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        getServer().getScheduler().scheduleTask(this, () -> {
                final List<OnlinePlayer> list = onlinePlayers();
                getServer().getScheduler().scheduleTask(this, () -> this.connect.updatePlayerList(list), true);
            });
    }

    @EventHandler
    public void onConnectRemoteConnect(ConnectRemoteConnectEvent event) {
        getLogger().info("Remote Connect: " + event.getRemote());
        for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey()).orElse(null);
            if (player != null) {
                player.sendMessage(TextFormat.colorize("[&7C&r] Remote Connect: ") + event.getRemote());
            }
        }
    }

    // @EventHandler
    // public void onConnectRemoteDisconnect(ConnectRemoteDisconnectEvent event) {
    //     getLogger().info("Remote Disconnect: " + event.getRemote());
    //     for (Map.Entry<UUID, String> entry: debugPlayers.entrySet()) {
    //         Player player = getServer().getPlayer(entry.getKey()).orElse(null);
    //         if (player != null) {
    //             player.sendMessage(TextFormat.colorize("[&7C&r] Remote Disconnect: ") + event.getRemote());
    //         }
    //     }
    // }
}
