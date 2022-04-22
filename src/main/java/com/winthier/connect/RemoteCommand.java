package com.winthier.connect;

import com.google.gson.Gson;
import lombok.Value;

@Value
public final class RemoteCommand {
    private final OnlinePlayer sender;
    private final String[] args;

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static RemoteCommand deserialize(String json) {
        return new Gson().fromJson(json, RemoteCommand.class);
    }
}
