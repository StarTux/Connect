package com.winthier.connect;

public interface ConnectHandler {
    void handleRemoteConnect(String name);
    void handleRemoteDisconnect(String name);
    void handleMessage(Message message);
    void handleRemoteCommand(OnlinePlayer sender, String server, String[] args);
}
