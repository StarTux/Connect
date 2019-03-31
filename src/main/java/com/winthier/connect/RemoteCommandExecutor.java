package com.winthier.connect;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RemoteCommandExecutor implements CommandExecutor {
    final ConnectPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player)sender;
        switch (label) {
        case "remote":
            if (args.length == 0) return false;
            plugin.getConnect().broadcastRemoteCommand(new OnlinePlayer(player.getUniqueId(), player.getName()), args);
            break;
        case "game": case "games":
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = label;
            for (int i = 0; i < args.length; i += 1) newArgs[i + 1] = args[i];
            plugin.getConnect().sendRemoteCommand("daemon", new OnlinePlayer(player.getUniqueId(), player.getName()), newArgs);
            break;
        default:
            plugin.getLogger().warning(getClass().getName() + ": Unexpected label: " + label);
            break;
        }
        return true;
    }
}
