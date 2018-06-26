package com.winthier.connect.bukkit;

import com.winthier.connect.Client;
import com.winthier.connect.ConnectionStatus;
import com.winthier.connect.OnlinePlayer;
import com.winthier.connect.Server;
import com.winthier.connect.ServerConnection;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

@RequiredArgsConstructor
public final class BukkitConnectCommand implements CommandExecutor {
    final BukkitConnectPlugin plugin;

    ChatColor color(ConnectionStatus status) {
        switch (status) {
        case INIT: return ChatColor.GRAY;
        case CONNECTED: return ChatColor.GREEN;
        case DISCONNECTED: return ChatColor.DARK_RED;
        case STOPPED: return ChatColor.YELLOW;
        default: return ChatColor.WHITE;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        String firstArg = args.length > 0 ? args[0] : null;
        if (firstArg == null) {
            return false;
        } else if ("status".equals(firstArg) && args.length == 1) {
            Server server = plugin.getConnect().getServer();
            if (server != null) {
                ConnectionStatus serverStatus = server.getStatus();
                sender.sendMessage(ChatColor.GREEN + "Server " + server.getName() + ChatColor.GREEN + " (" + server.getPort() + ") " + color(serverStatus) + serverStatus.name().toLowerCase());
                for (ServerConnection connection: server.getConnections()) {
                    ConnectionStatus status = connection.getStatus();
                    sender.sendMessage(" " + connection.getName() + " (" + connection.getPort() + ") " + color(status) + status.name().toLowerCase());
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Clients");
            for (Client client: plugin.getConnect().getClients()) {
                ConnectionStatus status = client.getStatus();
                sender.sendMessage(" " + client.getName() + " (" + client.getPort() + ") " + color(status) + status.name().toLowerCase() + ChatColor.DARK_GRAY + " [" + client.getQueueSize() + "]");
            }
        } else if ("reload".equals(firstArg) && args.length == 1) {
            plugin.reloadConfig();
            plugin.stopConnect();
            plugin.startConnect();
            sender.sendMessage("Configuration reloaded");
        } else if ("ping".equals(firstArg) && args.length == 1) {
            plugin.getConnect().broadcastAll("Connect", "Ping");
        } else if ("debug".equals(firstArg) && player != null) {
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
        } else if (("players".equals(firstArg)
                    || "who".equals(firstArg)
                    || "list".equals(firstArg))
                   && args.length == 1) {
            for (ServerConnection con: plugin.getConnect().getServer().getConnections()) {
                List<OnlinePlayer> players = new ArrayList<>(con.getOnlinePlayers());
                StringBuilder sb = new StringBuilder(ChatColor.GREEN + con.getName() + "(" + players.size() + ")" + ChatColor.RESET);
                for (OnlinePlayer onlinePlayer: players) sb.append(" ").append(onlinePlayer.getName());
                sender.sendMessage(sb.toString());
            }
        } else if ("packet".equals(firstArg) && args.length >= 3) {
            String name = args[1];
            String channel = args[2];
            Object payload;
            if (args.length >= 4) {
                StringBuilder sb = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i += 1) sb.append(" ").append(args[i]);
                payload = JSONValue.parse(sb.toString());
            } else {
                payload = null;
            }
            if (name.equals("*")) {
                for (Client client: plugin.getConnect().getClients()) {
                    boolean result = plugin.getConnect().send(client.getName(), channel, payload);
                    sender.sendMessage("Sent to " + client.getName() + ": " + result);
                }
            } else {
                boolean result = plugin.getConnect().send(name, channel, payload);
                sender.sendMessage("Sent to " + name + ": " + result);
            }
        } else {
            return false;
        }
        return true;
    }
}
