package com.winthier.connect;

public abstract class AbstractConnectHandler implements ConnectHandler {
    @Override
    public void runThread(Runnable runnable) {}

    @Override
    public void handleClientConnect(Client client) {}
    @Override
    public void handleClientDisconnect(Client client) {}

    @Override
    public void handleServerConnect(ServerConnection connection) {}
    @Override
    public void handleServerDisconnect(ServerConnection connection) {}

    @Override
    public void handleMessage(Message message) {}

    @Override
    public void handleRemoteCommand(OnlinePlayer sender, String server, String[] args) {}
}
