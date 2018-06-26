package com.winthier.connect.packet;

import com.winthier.connect.OnlinePlayer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public final class RemoteCommand {
    private OnlinePlayer sender;
    private String[] args;

    public Object serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("packetId", "RemoteCommand");
        result.put("sender", sender.serialize());
        result.put("args", Arrays.asList(args));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static RemoteCommand deserialize(Map<String, Object> map) {
        try {
            OnlinePlayer sender = OnlinePlayer.deserialize(map.get("sender"));
            String[] args = ((List<String>)map.get("args")).toArray(new String[0]);
            return new RemoteCommand(sender, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
