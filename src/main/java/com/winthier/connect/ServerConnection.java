package com.winthier.connect;

import com.winthier.connect.packet.PlayerList;
import com.winthier.connect.packet.RemoteCommand;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ServerConnection implements Runnable {
    private final Server server;
    private final Socket socket;
    private String name = "Unknown";
    private int port = 0;
    private String password = "x";
    private DataInputStream in = null;
    private boolean shouldQuit = false;
    private ConnectionStatus status = ConnectionStatus.INIT;
    private final List<OnlinePlayer> onlinePlayers = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void run() {
        mainLoop();
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) { }
            in = null;
        }
        status = ConnectionStatus.STOPPED;
    }

    void mainLoop() {
        // Setup
        try {
            in = new DataInputStream(socket.getInputStream());
            name = in.readUTF();
            port = Integer.parseInt(in.readUTF());
            password = in.readUTF();
            if (!password.equals(server.getConnect().getPassword())) {
                System.err.println("Password mismatch: '" + password + "' != '" + server.getConnect().getPassword() + "'");
                status = ConnectionStatus.DISCONNECTED;
                return;
            }
            status = ConnectionStatus.CONNECTED;
            server.getConnect().getHandler().handleServerConnect(this);
            // Wake up client
            Client client = server.getConnect().getClient(name);
            if (client != null) {
                client.send(ConnectionMessages.PING.message(client));
                client.skipSleep();
            }
        } catch (IOException ioe) {
            status = ConnectionStatus.DISCONNECTED;
            return;
        }
        while (!shouldQuit) {
            try {
                String line = in.readUTF();
                Message message = Message.deserialize(line);
                if (message == null) {
                    continue;
                } else {
                    if (("Connect").equals(message.getChannel())) {
                        try {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>)message.getPayload();
                            @SuppressWarnings("unchecked") String packetId = (String)map.get("packetId");
                            if ("PlayerList".equals(packetId)) {
                                PlayerList playerList = PlayerList.deserialize(map);
                                switch (playerList.getType()) {
                                case LIST:
                                    onlinePlayers.clear();
                                    onlinePlayers.addAll(playerList.getPlayers());
                                    break;
                                case JOIN:
                                    onlinePlayers.addAll(playerList.getPlayers());
                                    break;
                                case QUIT:
                                    onlinePlayers.removeAll(playerList.getPlayers());
                                    break;
                                default:
                                    break;
                                }
                            } else if ("RemoteCommand".equals(packetId)) {
                                RemoteCommand remoteCommand = RemoteCommand.deserialize(map);
                                server.getConnect().getHandler().handleRemoteCommand(remoteCommand.getSender(), message.getFrom(), remoteCommand.getArgs());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    server.getConnect().getHandler().handleMessage(message);
                }
            } catch (IOException ioe) {
                status = ConnectionStatus.DISCONNECTED;
                server.getConnect().getHandler().handleServerDisconnect(this);
                onlinePlayers.clear();
                // Check client client
                Client client = server.getConnect().getClient(name);
                if (client != null) {
                    client.send(ConnectionMessages.PING.message(client));
                    client.skipSleep();
                }
                return;
            }
        }
    }

    void quit() {
        shouldQuit = true;
        try {
            socket.shutdownInput();
        } catch (IOException ioe) { }
    }
}
