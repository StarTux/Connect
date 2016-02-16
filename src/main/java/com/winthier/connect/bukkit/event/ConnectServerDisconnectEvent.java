package com.winthier.connect.bukkit.event;

import com.winthier.connect.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@Getter
@RequiredArgsConstructor
public class ConnectServerDisconnectEvent extends Event {
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
