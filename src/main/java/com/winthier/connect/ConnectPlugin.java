package com.winthier.connect;

import com.cavetale.core.util.Json;
import com.winthier.connect.event.ConnectMessageEvent;
import com.winthier.connect.event.ConnectRemoteCommandEvent;
import com.winthier.connect.event.ConnectRemoteConnectEvent;
import com.winthier.connect.event.ConnectRemoteDisconnectEvent;
import com.winthier.connect.message.MessageSendPlayerMessage;
import com.winthier.connect.message.PlayerOpenBookMessage;
import com.winthier.connect.message.PlayerSendServerMessage;
import com.winthier.connect.message.RemotePlayerCommandMessage;
import com.winthier.connect.payload.OnlinePlayer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@Getter
public final class ConnectPlugin extends JavaPlugin implements ConnectHandler, Listener {
    @Getter protected static ConnectPlugin instance;
    protected Connect connect = null;
    @Setter private boolean debug = false;
    private Map<UUID, AwaitingPlayer> awaitingPlayerMap = new HashMap<>();
    protected Map<String, Map<UUID, RemotePlayerCommandMessage>> pendingRemoteCommandMap = new HashMap<>();
    private final CoreConnect coreConnect = new CoreConnect(this);
    private BukkitTask task;

    @Override
    public void onLoad() {
        instance = this;
        createConnect();
        coreConnect.register();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startConnect();
        new ConnectCommand(this).enable();
        final RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor(this);
        getCommand("remote").setExecutor(remoteCommandExecutor);
        Bungee.register(this);
        getServer().getPluginManager().registerEvents(this, this);
        connect.updatePlayerList(localOnlinePlayers());
    }

    @Override
    public void onDisable() {
        stopConnect();
        coreConnect.unregister();
    }

    private List<OnlinePlayer> localOnlinePlayers() {
        List<OnlinePlayer> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            result.add(new OnlinePlayer(player.getUniqueId(), player.getName(), connect.getServerName()));
        }
        return result;
    }

    // Connect

    protected void createConnect() {
        String serverName = getConfig().getString("ServerName");
        debug = getConfig().getBoolean("Debug");
        if (getConfig().isConfigurationSection("Server")) {
            String host = getConfig().getString("Server.Host");
            String user = getConfig().getString("Server.User");
            int port = getConfig().getInt("Server.Port");
            String password = getConfig().getString("Server.Password");
            getLogger().info("Using server configuration:"
                             + " host='" + host + "'"
                             + " port=" + port
                             + " user='" + user + "'"
                             + " password='" + password + "'");
            connect = new Connect(serverName, this, host, port, user, password);
        } else {
            connect = new Connect(serverName, this);
        }
    }

    protected void startConnect() {
        task = getServer().getScheduler().runTaskAsynchronously(this, connect);
    }

    void stopConnect() {
        if (connect == null) return;
        connect.stop();
        connect = null;
        task.cancel();
    }

    // ConnectHandler

    @Override
    public void handleRemoteConnect(String remote) {
        if (!isEnabled()) return;
        new BukkitRunnable() {
            @Override public void run() {
                getServer().getPluginManager().callEvent(new ConnectRemoteConnectEvent(remote));
                Map<UUID, RemotePlayerCommandMessage> pendingMap = pendingRemoteCommandMap.remove(remote);
                if (pendingMap != null) {
                    for (RemotePlayerCommandMessage message : pendingMap.values()) {
                        message.send(remote);
                    }
                }
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
        handleSpecialMessages(message);
        new ConnectMessageEvent(message).callEvent();
        new com.cavetale.core.event.connect
            .ConnectMessageEvent(message.getChannel(),
                                 message.getPayload(),
                                 message.getFrom(),
                                 message.getTo(),
                                 new Date(message.getCreated())).callEvent();
    }

    private void handleSpecialMessages(Message message) {
        switch (message.getChannel()) {
        case "DEBUG":
            getLogger().info(String.format("Debug message from=%s to=%s payload=%s",
                                           message.getFrom(), message.getTo(),
                                           message.getPayload()));
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
        final List<OnlinePlayer> list = localOnlinePlayers();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
                connect.updatePlayerList(list);
            });
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = localOnlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        connect.updatePlayerList(list);
                    });
            });
    }

    @EventHandler
    void onPlayerKick(PlayerKickEvent event) {
        getServer().getScheduler().runTask(this, () -> {
                final List<OnlinePlayer> list = localOnlinePlayers();
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        connect.updatePlayerList(list);
                    });
            });
    }

    @EventHandler
    void onConnectRemoteConnect(ConnectRemoteConnectEvent event) {
        getLogger().info("Remote Connect: " + event.getRemote());
    }

    @EventHandler
    void onConnectRemoteDisconnect(ConnectRemoteDisconnectEvent event) {
        getLogger().info("Remote Disconnect: " + event.getRemote());
    }

    @EventHandler
    protected void onConnectMessage(ConnectMessageEvent event) {
        Message message = event.getMessage();
        switch (message.getChannel()) {
        case "connect:runall": {
            String cmd = message.getPayload();
            getLogger().info("Received runall: " + cmd);
            Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                });
            break;
        }
        case RemotePlayerCommandMessage.CHANNEL: {
            RemotePlayerCommandMessage payload = Json.deserialize(message.getPayload(), RemotePlayerCommandMessage.class);
            ConnectRemotePlayer connectRemotePlayer = new ConnectRemotePlayer(payload.getUuid(),
                                                                              payload.getName(),
                                                                              payload.getOriginServerName());
            getLogger().info(payload.getName() + " (" + payload.getOriginServerName()
                             + ") sent remote command: " + payload.getCommand());
            Bukkit.dispatchCommand(connectRemotePlayer, payload.getCommand());
            break;
        }
        case PlayerSendServerMessage.CHANNEL: {
            PlayerSendServerMessage payload = Json.deserialize(message.getPayload(), PlayerSendServerMessage.class);
            Player player = Bukkit.getPlayer(payload.getUuid());
            if (player == null) return;
            Bungee.send(this, player, payload.getTargetServerName());
            break;
        }
        case MessageSendPlayerMessage.CHANNEL: {
            MessageSendPlayerMessage payload = Json.deserialize(message.getPayload(), MessageSendPlayerMessage.class);
            Player player = Bukkit.getPlayer(payload.getUuid());
            if (player == null) return;
            player.sendMessage(payload.parseComponent());
        }
        case PlayerOpenBookMessage.CHANNEL: {
            PlayerOpenBookMessage payload = Json.deserialize(message.getPayload(), PlayerOpenBookMessage.class);
            Player player = Bukkit.getPlayer(payload.getTarget());
            if (player == null) return;
            player.openBook(payload.parseBook());
        }
        default: break;
        }
    }

    private record AwaitingPlayer(Plugin plugin, Location location, Consumer<Player> callback) { }

    protected void bringAndAwait(@NonNull UUID uuid,
                                 @NonNull Plugin plugin,
                                 @NonNull String originServerName,
                                 @NonNull Location location,
                                 Consumer<Player> callback) {
        new PlayerSendServerMessage(uuid, connect.getServerName()).send(originServerName);
        AwaitingPlayer rec = new AwaitingPlayer(plugin, location, callback);
        awaitingPlayerMap.put(uuid, rec);
        Bukkit.getScheduler().runTaskLater(this, () -> {
                if (awaitingPlayerMap.get(uuid) != rec) return;
                awaitingPlayerMap.remove(uuid);
                if (callback != null) {
                    callback.accept(null);
                }
            }, 60L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void onAwaitingPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        AwaitingPlayer awaitingPlayer = awaitingPlayerMap.get(event.getPlayer().getUniqueId());
        if (awaitingPlayer == null) return;
        event.setSpawnLocation(awaitingPlayer.location());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    protected void onAwaitingPlayerJoin(PlayerJoinEvent event) {
        AwaitingPlayer awaitingPlayer = awaitingPlayerMap.remove(event.getPlayer().getUniqueId());
        if (awaitingPlayer == null || awaitingPlayer.callback() == null) return;
        awaitingPlayer.callback().accept(event.getPlayer());
    }

    @EventHandler
    protected void onPluginDisable(PluginDisableEvent event) {
        awaitingPlayerMap.entrySet().removeIf(it -> it.getValue().plugin == event.getPlugin());
    }
}
