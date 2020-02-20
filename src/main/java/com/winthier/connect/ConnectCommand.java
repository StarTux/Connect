package com.winthier.connect;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class ConnectCommand implements CommandExecutor {
    final ConnectPlugin plugin;
    Gson gson = new Gson();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        return onCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean onCommand(CommandSender sender, String cmd, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        switch (cmd) {
        case "status":
        case "info": {
            if (args.length != 0) return false;
            sender.sendMessage(ChatColor.GREEN + "Server: " + plugin.getConnect().getServerName());
            for (String remote: plugin.getConnect().listServers()) {
                sender.sendMessage("Remote: " + remote);
            }
            return true;
        }
        case "reload": {
            if (args.length != 0) return false;
            plugin.reloadConfig();
            plugin.stopConnect();
            plugin.startConnect();
            sender.sendMessage("Configuration reloaded");
            return true;
        }
        case "ping": {
            if (args.length != 0) return false;
            plugin.getConnect().ping();
            return true;
        }
        case "debug": {
            if (player == null) {
                plugin.setDebug(!plugin.isDebug());
                plugin.getLogger().info("Debug mode: " + plugin.isDebug());
                return true;
            }
            if (args.length == 1) {
                plugin.getDebugPlayers().remove(player.getUniqueId());
                player.sendMessage("Debug mode disabled");
            } else if (args.length == 2) {
                String chan = args[1];
                plugin.getDebugPlayers().put(player.getUniqueId(), chan);
                player.sendMessage("Debugging Connect channel " + chan);
            } else {
                return false;
            }
            return true;
        }
        case "players":
        case "who":
        case "list": {
            if (args.length != 0) return false;
            for (Map.Entry<String, List<OnlinePlayer>> serverEntry
                     : plugin.getConnect().listPlayers().entrySet()) {
                StringBuilder sb = new StringBuilder(serverEntry.getKey())
                    .append("(")
                    .append(serverEntry.getValue().size())
                    .append(")");
                for (OnlinePlayer onlinePlayer: serverEntry.getValue()) {
                    sb.append(" ").append(onlinePlayer.getName());
                }
                sender.sendMessage(sb.toString());
            }
            return true;
        }
        case "packet": {
            if (args.length < 2) return false;
            String name = args[0];
            String channel = args[1];
            Object payload;
            if (args.length >= 3) {
                StringBuilder sb = new StringBuilder(args[2]);
                for (int i = 3; i < args.length; i += 1) sb.append(" ").append(args[i]);
                payload = gson.fromJson(sb.toString(), Object.class);
            } else {
                payload = null;
            }
            if (name.equals("*")) {
                for (String remote: plugin.getConnect().listServers()) {
                    boolean result = plugin.getConnect().send(remote, channel, payload);
                    sender.sendMessage("Sent to " + remote + ": " + result);
                }
            } else {
                boolean result = plugin.getConnect().send(name, channel, payload);
                sender.sendMessage("Sent to " + name + ": " + result);
            }
            return true;
        }
        default: return false;
        }
    }
}
