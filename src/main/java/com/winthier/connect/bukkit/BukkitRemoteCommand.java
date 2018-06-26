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
        if (args.length == 0) return false;
        if (!(sender instanceof Player)) return false;
        Player player = (Player)sender;
        plugin.getConnect().broadcastRemoteCommand(new OnlinePlayer(player.getUniqueId(), player.getName()), args);
        return true;
    }
}
