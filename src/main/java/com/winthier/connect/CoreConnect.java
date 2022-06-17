package com.winthier.connect;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.connect.ServerGroup;
import com.winthier.connect.message.RemotePlayerCommandMessage;
import com.winthier.connect.payload.OnlinePlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class CoreConnect implements com.cavetale.core.connect.Connect {
    private final ConnectPlugin plugin;

    @Override
    public ConnectPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getServerName() {
        return plugin.connect.getServerName();
    }

    @Override
    public void sendMessage(String targetServer, String channel, String payload) {
        plugin.connect.send(targetServer, channel, payload);
    }

    @Override
    public void broadcastMessage(String channel, String payload) {
        plugin.connect.broadcast(channel, payload);
    }

    @Override
    public void broadcastMessage(ServerGroup group, String channel, String payload) {
        NetworkServer thisServer = NetworkServer.current();
        for (String server : plugin.connect.listServers()) {
            NetworkServer thatServer = NetworkServer.of(server);
            if (thisServer != thatServer && thatServer.group == group) {
                plugin.connect.send(server, channel, payload);
            }
        }
    }

    @Override
    public void broadcastMessageToAll(String channel, String payload) {
        plugin.connect.broadcastAll(channel, payload);
    }

    @Override
    public void broadcastMessageToAll(ServerGroup group, String channel, String payload) {
        for (String server : plugin.connect.listServers()) {
            NetworkServer thatServer = NetworkServer.of(server);
            if (thatServer.group == group) {
                plugin.connect.send(server, channel, payload);
            }
        }
    }

    @Override
    public void dispatchRemoteCommand(Player player, String command, String targetServer) {
        RemotePlayerCommandMessage message = new RemotePlayerCommandMessage(player, command);
        if (plugin.connect.listServers().contains(targetServer)) {
            message.send(targetServer);
        } else {
            plugin.pendingRemoteCommandMap.computeIfAbsent(targetServer, t -> new HashMap<>())
                .put(player.getUniqueId(), message);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Redis.lpush("cavetale.server_wake." + targetServer, "wake_up", 30L);
                });
            player.sendMessage(text("Please wait while server is starting up...", YELLOW));
        }
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        Set<UUID> result = new HashSet<>();
        for (OnlinePlayer player : Connect.getInstance().getOnlinePlayers()) {
            result.add(player.getUuid());
        }
        return result;
    }

    @Override
    public List<RemotePlayer> getRemotePlayers() {
        List<RemotePlayer> result = new ArrayList<>();
        Connect.getInstance().listPlayers().forEach((server, list) -> {
                for (OnlinePlayer online : list) {
                    result.add(new ConnectRemotePlayer(online.getUuid(), online.getName(), server));
                }
            });
        return result;
    }

    @Override
    public RemotePlayer getRemotePlayer(UUID uuid) {
        OnlinePlayer player = plugin.connect.findOnlinePlayer(uuid);
        return player != null
            ? new ConnectRemotePlayer(player.getUuid(), player.getName(), player.getServer())
            : null;
    }

    @Override
    public Set<String> getOnlineServerNames() {
        return Set.copyOf(plugin.connect.listServers());
    }
}
