package com.winthier.connect;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONValue;

@Getter
@RequiredArgsConstructor
public final class Message {
    final String channel;
    final String from;
    final String to;
    final Object payload;
    final transient long created = System.currentTimeMillis();

    String serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("channel", channel);
        result.put("from", from);
        result.put("to", to);
        result.put("payload", payload);
        return JSONValue.toJSONString(result);
    }

    static Message deserialize(String serial) {
        Object o = JSONValue.parse(serial);
        if (!(o instanceof Map)) return null;
        Map<?, ?> map = (Map<?, ?>)o;
        String channel = map.get("channel").toString();
        String from = map.get("from").toString();
        String to = map.get("to").toString();
        Object payload = map.get("payload");
        if (from == null || to == null) return null;
        return new Message(channel, from, to, payload);
    }

    boolean tooOld() {
        return System.currentTimeMillis() - created > 1000 * 30;
    }
}
