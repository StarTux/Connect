package com.winthier.connect;

public abstract class AbstractConnectHandler implements ConnectHandler {
    @Override public void handleRemoteConnect(String name) { }
    @Override public void handleRemoteDisconnect(String name) { }
    @Override public void handleMessage(Message message) { }
    @Override public void handleRemoteCommand(OnlinePlayer sender, String server, String[] args) { }
}
