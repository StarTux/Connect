package com.winthier.connect;

public enum ConnectionMessages {
    QUIT,
    PING;

    Message message(Client client) {
        return new Message(name(), Connect.getInstance().getName(), client.getName(), null);
    }
}
