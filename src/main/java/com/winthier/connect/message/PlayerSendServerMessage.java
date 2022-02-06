package com.winthier.connect.message;

import com.cavetale.core.util.Json;
import com.winthier.connect.Connect;
import java.util.UUID;
import lombok.Data;

@Data
public final class PlayerSendServerMessage {
    public static final String CHANNEL = "connect:player_send_server";

    protected UUID uuid;
    protected String targetServerName;

    public PlayerSendServerMessage() { }

    public PlayerSendServerMessage(final UUID uuid) {
        this.uuid = uuid;
        this.targetServerName = Connect.getInstance().getServerName();
    }

    public PlayerSendServerMessage(final UUID uuid, final String targetServerName) {
        this.uuid = uuid;
        this.targetServerName = targetServerName;
    }

    public void send(String targetServer) {
        Connect.getInstance().send(targetServer, CHANNEL, Json.serialize(this));
    }

    public void broadcast() {
        Connect.getInstance().broadcast(CHANNEL, Json.serialize(this));
    }

    public void broadcastAll() {
        Connect.getInstance().broadcastAll(CHANNEL, Json.serialize(this));
    }
}
