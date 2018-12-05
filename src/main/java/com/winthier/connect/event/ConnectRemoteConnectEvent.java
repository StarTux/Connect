package com.winthier.connect.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@Getter @RequiredArgsConstructor
public final class ConnectRemoteConnectEvent extends Event {
    private final String remote;

    // Event

    @Getter private static HandlerList handlerList = new HandlerList();

    @Override public HandlerList getHandlers() {
        return handlerList;
    }
}
