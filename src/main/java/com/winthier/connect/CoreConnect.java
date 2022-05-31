package com.winthier.connect;

import com.cavetale.core.command.RemotePlayer;
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
}
