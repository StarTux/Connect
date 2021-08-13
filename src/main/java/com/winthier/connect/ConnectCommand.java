package com.winthier.connect;

import com.cavetale.core.command.CommandNode;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class ConnectCommand implements TabExecutor {
    private final ConnectPlugin plugin;
    private final Gson gson = new Gson();
    private CommandNode rootNode;

    protected void enable() {
        rootNode = new CommandNode("connect");
        rootNode.addChild("status").denyTabCompletion()
            .description("Server Status")
            .senderCaller(this::status);
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload configuration")
            .senderCaller(this::reload);
        rootNode.addChild("ping").denyTabCompletion()
            .description("Ping servers")
            .senderCaller(this::ping);
        rootNode.addChild("debug").arguments("[channel]")
            .description("Toggle debug mode")
            .senderCaller(this::debug);
        rootNode.addChild("who").denyTabCompletion()
            .description("List players per server")
            .senderCaller(this::who);
        rootNode.addChild("packet").arguments("<server> <channel> [payload]")
            .description("Send packet")
            .senderCaller(this::packet);
        rootNode.addChild("runall").arguments("<command>")
            .description("Run command on all servers")
            .senderCaller(this::runall);
        plugin.getCommand("connect").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return rootNode.call(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return rootNode.complete(sender, command, label, args);
    }

    boolean status(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(ChatColor.GREEN + "Server: " + plugin.getConnect().getServerName());
        for (String remote: plugin.getConnect().listServers()) {
            sender.sendMessage("Remote: " + remote);
        }
        return true;
    }

    boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.reloadConfig();
        plugin.stopConnect();
        plugin.startConnect();
        sender.sendMessage("Configuration reloaded");
        return true;
    }

    boolean ping(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.getConnect().ping();
        return true;
    }

    boolean debug(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            plugin.setDebug(!plugin.isDebug());
            plugin.getLogger().info("Debug mode: " + plugin.isDebug());
            return true;
        }
        if (args.length == 0) {
            plugin.getDebugPlayers().remove(player.getUniqueId());
            player.sendMessage("Debug mode disabled");
        } else if (args.length == 1) {
            String chan = args[0];
            plugin.getDebugPlayers().put(player.getUniqueId(), chan);
            player.sendMessage("Debugging Connect channel " + chan);
        }
        return true;
    }

    boolean who(CommandSender sender, String[] args) {
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

    boolean packet(CommandSender sender, String[] args) {
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

    boolean runall(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        String cmd = String.join(" ", args);
        plugin.getConnect().broadcastAll("connect:runall", cmd);
        sender.sendMessage(Component.text("Triggering command on all servers: /" + cmd, NamedTextColor.YELLOW));
        return true;
    }
}
