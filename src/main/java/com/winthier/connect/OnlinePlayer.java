package com.winthier.connect;

import java.util.UUID;
import lombok.Value;

@Value
public final class OnlinePlayer {
    private UUID uuid;
    private String name;
}
