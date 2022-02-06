package com.winthier.connect;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.perm.Perm;
import com.winthier.connect.message.MessageSendPlayerMessage;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

/**
 * Represents a player sending a command from a different server.
 */
@Getter @RequiredArgsConstructor
public final class ConnectRemotePlayer implements RemotePlayer {
    private final UUID uuid;
    private final String name;
    private final String originServerName;

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public void bring(Plugin plugin, Consumer<PlayerSpawnLocationEvent> callback) {
        ConnectPlugin.instance.bringAndAwait(uuid, plugin, originServerName, callback);
    }

    @Override
    public void sendMessage(String msg) {
        sendMessage(Component.text(msg));
    }

    @Override
    public void sendMessage(Identity identity, Component component) {
        sendMessage(component);
    }

    @Override
    public void sendMessage(Identified identified, Component component) {
        sendMessage(component);
    }

    @Override
    public void sendMessage(Component component) {
        new MessageSendPlayerMessage(uuid, component).send(originServerName);
    }

    @Override
    public Component name() {
        return Component.text(name);
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public boolean isPermissionSet(String permission) {
        return Perm.get().getPerms(uuid).containsKey(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return Perm.get().has(uuid, permission);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return isPermissionSet(permission.getName());
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return hasPermission(permission.getName());
    }
}
