package com.winthier.connect;

import com.winthier.connect.message.RemotePlayerCommandMessage;
import com.winthier.connect.payload.OnlinePlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class CoreConnect implements com.cavetale.core.connect.Connect {
    @Override
    public String getServerName() {
        return ConnectPlugin.instance.connect.getServerName();
    }

    @Override
    public void dispatchRemoteCommand(Player player, String command, String targetServer) {
        new RemotePlayerCommandMessage(player, command).send(targetServer);
    }

    @Override
    public Set<UUID> getOnlinePlayers() {
        Set<UUID> result = new HashSet<>();
        for (OnlinePlayer player : Connect.getInstance().getOnlinePlayers()) {
            result.add(player.getUuid());
        }
        return result;
    }
}
