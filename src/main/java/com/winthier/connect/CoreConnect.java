package com.winthier.connect;

public final class CoreConnect implements com.cavetale.core.connect.Connect {
    @Override
    public String getServerName() {
        return ConnectPlugin.instance.connect.getServerName();
    }
}
