package com.winthier.connect.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.winthier.connect.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public final class ConnectMessageEvent extends Event {
    private final Message message;

    // Event

    private static HandlerList handlerList = new HandlerList();

    public static HandlerList getHandlers() {
        return handlerList;
    }
}
