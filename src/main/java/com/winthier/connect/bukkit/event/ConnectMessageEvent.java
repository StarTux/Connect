package com.winthier.connect.bukkit.event;

import com.winthier.connect.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@Getter
@RequiredArgsConstructor
public class ConnectMessageEvent extends Event {
    final Message message;

    Client getFrom() {
        return Connect.getInstance().getClient(message.getFrom());
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
