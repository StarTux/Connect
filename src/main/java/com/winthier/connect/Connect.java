package com.winthier.connect;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

@Getter
public final class Connect implements Runnable {
    private static final String KEY_SERVER_MAP = "cavetale.connect.server_map";
    private static final String KEY_SERVER_QUEUE = "cavetale.connect.server_queue";
    private static final String KEY_PLAYER_LIST = "cavetale.connect.player_list";
    private final String serverName;
    private final ConnectHandler handler;
    private final String messageQueue;
    private final JedisPool jedisPool;
    private volatile boolean shouldStop = false;
    private volatile boolean hasPlayerList = false;
    @Getter private static Connect instance = null;

    // --- Client

    public Connect(String serverName, ConnectHandler handler) {
        if (serverName == null) throw new NullPointerException("serverName cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        this.serverName = serverName;
        this.handler = handler;
        this.messageQueue = KEY_SERVER_QUEUE + "." + serverName;
        this.jedisPool = new JedisPool("localhost");
        instance = this;
    }

    public void stop() {
        this.shouldStop = true;
        broadcast("DISCONNECT", null, false);
        unregisterServerList();
    }

    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            registerServerList();
            broadcast("CONNECT", null, false);
            long lastRegister = Instant.now().getEpochSecond();
            while (!shouldStop) {
                try {
                    List<String> inp = jedis.brpop(1, this.messageQueue);
                    if (inp != null && inp.size() == 2) {
                        Message message = Message.deserialize(inp.get(1));
                        this.handler.handleMessage(message);
                        switch (message.channel) {
                        case "REMOTE":
                            if (message.payload instanceof String) {
                                RemoteCommand rcmd = RemoteCommand.deserialize((String)message.payload);
                                this.handler.handleRemoteCommand(rcmd.getSender(), message.from, rcmd.getArgs());
                            }
                            break;
                        case "CONNECT":
                            this.handler.handleRemoteConnect(message.from);
                            break;
                        case "DISCONNECT":
                            this.handler.handleRemoteDisconnect(message.from);
                            break;
                        default:
                            break;
                        }
                    }
                    long now = Instant.now().getEpochSecond();
                    if (now - lastRegister >= 10) {
                        lastRegister = now;
                        registerServerList();
                        if (hasPlayerList) keepPlayerListAlive();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void registerServerList() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset(KEY_SERVER_MAP, this.serverName, "" + Instant.now().getEpochSecond());
        }
    }

    void unregisterServerList() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hdel(KEY_SERVER_MAP, this.serverName);
        }
    }

    // --- Sending

    public List<String> listServers() {
        List<String> result = new ArrayList<>();
        try (Jedis jedis = this.jedisPool.getResource()) {
            Map<String, String> allServers = jedis.hgetAll(KEY_SERVER_MAP);
            long now = Instant.now().getEpochSecond();
            for (Map.Entry<String, String> entry: allServers.entrySet()) {
                Long seen = Long.parseLong(entry.getValue());
                String key = entry.getKey();
                if (now - seen > 60) {
                    jedis.hdel(KEY_SERVER_MAP, key);
                } else {
                    result.add(key);
                }
            }
        }
        return result;
    }

    public boolean send(String target, String channel, Object payload) {
        final Message message = new Message(channel, this.serverName, target, payload);
        final String rediskey = KEY_SERVER_QUEUE + "." + target;
        try (Jedis jedis = this.jedisPool.getResource()) {
            Transaction t = jedis.multi();
            t.lpush(rediskey, message.serialize());
            t.expire(rediskey, 10);
            t.exec();
        }
        return true;
    }

    // --- Broadcast

    private void broadcast(String channel, Object payload, boolean all) {
        for (String clientname: listServers()) {
            if (!all && clientname.equals(this.serverName)) continue;
            send(clientname, channel, payload);
        }
    }

    public void broadcast(String channel, Object payload) {
        broadcast(channel, payload, false);
    }

    public void broadcastAll(String channel, Object payload) {
        broadcast(channel, payload, true);
    }

    public void ping() {
        broadcast("PING", null, false);
    }

    public void broadcastRemoteCommand(OnlinePlayer sender, String[] args) {
        broadcast("REMOTE", new RemoteCommand(sender, args).serialize());
    }

    public void sendRemoteCommand(String target, OnlinePlayer sender, String[] args) {
        send(target, "REMOTE", new RemoteCommand(sender, args).serialize());
    }

    // --- Player List

    /**
     * Call whenever a player joins or quits.
     */
    public void updatePlayerList(Collection<OnlinePlayer> players) {
        this.hasPlayerList = true;
        if (players.isEmpty()) {
            removePlayerList();
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        for (OnlinePlayer player: players) {
            // Use names as values as they may theoretically not be unique.
            map.put(player.getUuid().toString(), player.getName());
        }
        final String key = KEY_PLAYER_LIST + "." + this.serverName;
        try (Jedis jedis = this.jedisPool.getResource()) {
            Transaction t = jedis.multi();
            t.del(key);
            t.hset(key, map);
            t.expire(key, 60);
            t.exec();
        }
    }

    private void keepPlayerListAlive() {
        final String key = KEY_PLAYER_LIST + "." + this.serverName;
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.expire(key, 60);
        }
    }

    private void removePlayerList() {
        final String key = KEY_PLAYER_LIST + "." + this.serverName;
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    public Map<String, List<OnlinePlayer>> listPlayers() {
        HashMap<String, List<OnlinePlayer>> result = new HashMap<>();
        try (Jedis jedis = this.jedisPool.getResource()) {
            for (String other: listServers()) {
                List<OnlinePlayer> playerList = new ArrayList<>();
                result.put(other, playerList);
                for (Map.Entry<String, String> playerEntry: jedis.hgetAll(KEY_PLAYER_LIST + "." + other).entrySet()) {
                    playerList.add(new OnlinePlayer(UUID.fromString(playerEntry.getKey()), playerEntry.getValue()));
                }
            }
        }
        return result;
    }

    public List<OnlinePlayer> getOnlinePlayers() {
        List<OnlinePlayer> result = new ArrayList<>();
        try (Jedis jedis = this.jedisPool.getResource()) {
            for (String other: listServers()) {
                for (Map.Entry<String, String> playerEntry: jedis.hgetAll(KEY_PLAYER_LIST + "." + other).entrySet()) {
                    result.add(new OnlinePlayer(UUID.fromString(playerEntry.getKey()), playerEntry.getValue()));
                }
            }
        }
        return result;
    }

    public OnlinePlayer findOnlinePlayer(String name) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            for (String other: listServers()) {
                for (Map.Entry<String, String> playerEntry: jedis.hgetAll(KEY_PLAYER_LIST + "." + other).entrySet()) {
                    if (playerEntry.getValue().equals(name)) return new OnlinePlayer(UUID.fromString(playerEntry.getKey()), playerEntry.getValue());
                }
            }
        }
        return null;
    }
}
