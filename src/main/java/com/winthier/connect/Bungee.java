package com.winthier.connect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Bungee {
    private Bungee() { }

    public static void register(Plugin plugin) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }

    public static void send(Plugin plugin, Player player, String serverName) {
        final byte[] pluginMessage;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeUTF("Connect");
            dataOutputStream.writeUTF(serverName);
            pluginMessage = byteArrayOutputStream.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        player.sendPluginMessage(plugin, "BungeeCord", pluginMessage);
    }
}
