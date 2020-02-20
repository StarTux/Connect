package com.winthier.connect;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class RemoteCommandExecutor implements CommandExecutor {
    final ConnectPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        switch (label) {
        case "remote":
            if (args.length == 0) return false;
            OnlinePlayer op = new OnlinePlayer(player.getUniqueId(), player.getName());
            plugin.getConnect().broadcastRemoteCommand(op, args);
            break;
        default:
            plugin.getLogger().warning(getClass().getName() + ": Unexpected label: " + label);
            break;
        }
        return true;
    }
}
