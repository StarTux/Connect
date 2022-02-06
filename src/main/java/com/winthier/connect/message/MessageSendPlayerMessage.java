package com.winthier.connect.message;

import com.cavetale.core.util.Json;
import com.winthier.connect.Connect;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

@Data
public final class MessageSendPlayerMessage {
    public static final String CHANNEL = "connect:message_send_player";

    protected UUID uuid;
    protected String component;

    public MessageSendPlayerMessage() { }

    public MessageSendPlayerMessage(final UUID uuid, final Component component) {
        this.uuid = uuid;
        this.component = GsonComponentSerializer.gson().serialize(component);
    }

    public Component parseComponent() {
        return GsonComponentSerializer.gson().deserialize(component);
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
