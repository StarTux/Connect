package com.winthier.connect.bukkit;

import com.winthier.connect.OnlinePlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class BukkitRemoteCommand implements CommandExecutor {
    final BukkitConnectPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player)sender;
        switch (label) {
        case "remote":
            if (args.length == 0) return false;
            plugin.getConnect().broadcastRemoteCommand(new OnlinePlayer(player.getUniqueId(), player.getName()), args);
            break;
        case "game":
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = label;
            for (int i = 0; i < args.length; i += 1) newArgs[i + 1] = args[i];
            plugin.getConnect().broadcastRemoteCommand(new OnlinePlayer(player.getUniqueId(), player.getName()), newArgs);
            break;
        default:
            plugin.getLogger().warning(getClass().getName() + ": Unexpected label: " + label);
            break;
        }
        return true;
    }
}
