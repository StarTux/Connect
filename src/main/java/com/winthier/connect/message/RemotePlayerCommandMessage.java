package com.winthier.connect.message;

import com.cavetale.core.util.Json;
import com.winthier.connect.Connect;
import java.util.UUID;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
public final class RemotePlayerCommandMessage {
    public static final String CHANNEL = "connect:remote_player_command";

    protected UUID uuid;
    protected String name;
    protected String originServerName;
    protected String command;

    public RemotePlayerCommandMessage() { }

    public RemotePlayerCommandMessage(final Player player, final String command) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.originServerName = Connect.getInstance().getServerName();
        this.command = command;
    }

    public void send(String targetServer) {
        Connect.getInstance().send(targetServer, CHANNEL, Json.serialize(this));
    }
}
