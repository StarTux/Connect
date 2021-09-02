package com.winthier.connect;

import com.cavetale.core.command.AbstractCommand;
import com.google.gson.Gson;
import com.winthier.connect.payload.OnlinePlayer;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class ConnectCommand extends AbstractCommand<ConnectPlugin> {
    private final Gson gson = new Gson();

    protected ConnectCommand(final ConnectPlugin plugin) {
        super(plugin, "connect");
    }

    @Override
    protected void onEnable() {
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
    }

    protected boolean status(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(Component.text("Server: " + plugin.getConnect().getServerName(), NamedTextColor.GREEN));
        for (String remote: plugin.getConnect().listServers()) {
            sender.sendMessage(Component.text("Remote: " + remote, NamedTextColor.WHITE));
        }
        return true;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.reloadConfig();
        plugin.stopConnect();
        plugin.startConnect();
        sender.sendMessage("Configuration reloaded");
        return true;
    }

    protected boolean ping(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.getConnect().ping();
        return true;
    }

    protected boolean debug(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        plugin.setDebug(!plugin.isDebug());
        plugin.getLogger().info("Debug mode: " + plugin.isDebug());
        return true;
    }

    protected boolean who(CommandSender sender, String[] args) {
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

    protected boolean packet(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String name = args[0];
        String channel = args[1];
        String payload;
        if (args.length >= 3) {
            StringBuilder sb = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; i += 1) sb.append(" ").append(args[i]);
            payload = sb.toString();
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

    protected boolean runall(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        String cmd = String.join(" ", args);
        plugin.getConnect().broadcastAll("connect:runall", cmd);
        sender.sendMessage(Component.text("Triggering command on all servers: /" + cmd, NamedTextColor.YELLOW));
        return true;
    }
}
