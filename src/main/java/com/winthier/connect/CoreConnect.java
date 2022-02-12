package com.winthier.connect;

import com.winthier.connect.message.RemotePlayerCommandMessage;
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
}
