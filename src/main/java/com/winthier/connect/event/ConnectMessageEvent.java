package com.winthier.connect.event;

import com.winthier.connect.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@Getter @RequiredArgsConstructor
public final class ConnectMessageEvent extends Event {
    private final Message message;

    // Event

    @Getter private static HandlerList handlerList = new HandlerList();

    @Override public HandlerList getHandlers() {
        return handlerList;
    }
}
