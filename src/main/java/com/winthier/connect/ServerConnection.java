package com.winthier.connect;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ServerConnection implements Runnable {
    final Server server;
    final Socket socket;
    String name = "Unknown";
    int port = 0;
    String password = "x";
    DataInputStream in = null;
    boolean shouldQuit = false;
    ConnectionStatus status = ConnectionStatus.INIT;

    @Override
    public void run() {
        mainLoop();
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {}
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
                client.send(ConnectionMessages.PING.name());
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
                    // TODO
                } else {
                    server.getConnect().getHandler().handleMessage(message);
                }
            } catch (IOException ioe) {
                status = ConnectionStatus.DISCONNECTED;
                server.getConnect().getHandler().handleServerDisconnect(this);
                // Check client client
                Client client = server.getConnect().getClient(name);
                if (client != null) {
                    client.send(ConnectionMessages.PING.name());
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
        } catch (IOException ioe) {}
    }
}
