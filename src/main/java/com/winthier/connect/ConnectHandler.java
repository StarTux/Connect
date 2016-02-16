package com.winthier.connect;

public interface ConnectHandler {
    void runThread(Runnable runnable);

    void handleClientConnect(Client client);
    void handleClientDisconnect(Client client);

    void handleServerConnect(ServerConnection connection);
    void handleServerDisconnect(ServerConnection connection);

    void handleMessage(Message message);
}
