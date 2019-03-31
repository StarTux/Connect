package com.winthier.connect.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.winthier.connect.OnlinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public final class ConnectRemoteCommandEvent extends Event {
    final OnlinePlayer sender;
    final String server;
    final String[] args;

    // Event

    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }
}
