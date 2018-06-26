package com.winthier.connect.packet;

import com.winthier.connect.OnlinePlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

@Value
public final class PlayerList {
    public enum Type {
        LIST, JOIN, QUIT;
        public PlayerList playerList(List<OnlinePlayer> playerList) {
            return new PlayerList(this, playerList);
        }
    }

    private Type type;
    private List<OnlinePlayer> players;

    public Object serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("packetId", "PlayerList");
        result.put("type", type.name());
        Map<String, String> playerMap = new HashMap<>();
        result.put("players", playerMap);
        for (OnlinePlayer onlinePlayer: this.players) playerMap.put(onlinePlayer.getUuid().toString(), onlinePlayer.getName());
        return result;
    }

    @SuppressWarnings("unchecked")
    public static PlayerList deserialize(Map<String, Object> map) {
        try {
            Type type = Type.valueOf((String)map.get("type"));
            List<OnlinePlayer> players = new ArrayList<>();
            for (Map.Entry<String, String> mapEntry: ((Map<String, String>)map.get("players")).entrySet()) {
                players.add(new OnlinePlayer(UUID.fromString(mapEntry.getKey()), mapEntry.getValue()));
            }
            return new PlayerList(type, players);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
