package com.winthier.connect.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public final class ConnectRemoteDisconnectEvent extends Event {
    private final String remote;

    // Event

    private static HandlerList handlerList = new HandlerList();

    public static HandlerList getHandlers() {
        return handlerList;
    }
}
