package com.winthier.connect;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @ToString
public final class Message {
    final String channel;
    final String from;
    final String to;
    final Object payload;
    final transient long created = System.currentTimeMillis();

    String serialize() {
        return new Gson().toJson(this);
    }

    static Message deserialize(String serial) {
        return new Gson().fromJson(serial, Message.class);
    }

    boolean tooOld() {
        return System.currentTimeMillis() - created > 1000 * 30;
    }
}
