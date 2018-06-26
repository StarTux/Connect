package com.winthier.connect.bukkit.event;

import com.winthier.connect.Client;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@Getter
@RequiredArgsConstructor
public final class ConnectClientDisconnectEvent extends Event {
    final Client client;

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
