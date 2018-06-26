package com.winthier.connect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

@Value
public final class OnlinePlayer {
    private UUID uuid;
    private String name;

    public Map<String, String> serialize() {
        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid.toString());
        result.put("name", name);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static OnlinePlayer deserialize(Object o) {
        try {
            Map<String, String> map = (Map<String, String>)o;
            UUID uuid = UUID.fromString(map.get("uuid"));
            String name = map.get("name");
            return new OnlinePlayer(uuid, name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
