package com.winthier.connect.bukkit.event;

import com.winthier.connect.Client;
import com.winthier.connect.Connect;
import com.winthier.connect.ServerConnection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public final class ConnectServerConnectEvent extends Event {
    final ServerConnection connection;

    Client getClient() {
        return Connect.getInstance().getClient(connection.getName());
    }

    // Event
    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
