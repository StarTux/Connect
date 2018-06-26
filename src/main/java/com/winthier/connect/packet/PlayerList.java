package com.winthier.connect.packet;

import com.winthier.connect.OnlinePlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

@Value
public class PlayerList {
    public static enum Type {
        LIST, JOIN, QUIT;
        public PlayerList playerList(List<OnlinePlayer> players) {
            return new PlayerList(this, players);
        }
    }

    Type type;
    List<OnlinePlayer> players;

    public Object serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("packetId", "PlayerList");
        result.put("type", type.name());
        Map<String, String> players = new HashMap<>();
        result.put("players", players);
        for (OnlinePlayer onlinePlayer: this.players) players.put(onlinePlayer.getUuid().toString(), onlinePlayer.getName());
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
