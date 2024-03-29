package com.winthier.connect.event;

import com.winthier.connect.payload.OnlinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public final class ConnectRemoteCommandEvent extends Event {
    final OnlinePlayer sender;
    final String server;
    final String[] args;

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
