package com.winthier.connect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class Server implements Runnable {
    private final Connect connect;
    private final String name;
    private final int port;
    private final String displayName;
    private ServerSocket serverSocket = null;
    private boolean shouldQuit = false;
    private ConnectionStatus status = ConnectionStatus.INIT;
    private final List<ServerConnection> connections = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void run() {
        mainLoop();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioe) { }
            serverSocket = null;
        }
        for (ServerConnection connection: connections) {
            connection.quit();
        }
        connections.clear();
        status = ConnectionStatus.STOPPED;
    }

    void sleep(int seconds) {
        for (int i = 0; i < seconds; ++i) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) { }
            if (shouldQuit) return;
        }
    }

    void mainLoop() {
        // Create, bind
        while (serverSocket == null) {
            if (shouldQuit) return;
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout(1000 * 10);
                serverSocket.bind(new InetSocketAddress(port));
                status = ConnectionStatus.CONNECTED;
            } catch (IOException ioe) {
                status = ConnectionStatus.DISCONNECTED;
                System.err.println("Server " + name + " " + port + " " + displayName);
                ioe.printStackTrace();
                serverSocket = null;
            }
            if (serverSocket == null) sleep(10);
        }
        // Accept connections
        while (!shouldQuit) {
            try {
                Socket socket = serverSocket.accept();
                ServerConnection serverConnection = new ServerConnection(this, socket);
                connections.add(serverConnection);
                connect.getHandler().runThread(serverConnection);
            } catch (IOException ioe) { }
            Iterator<ServerConnection> iter = connections.iterator();
            while (iter.hasNext()) {
                ServerConnection connection = iter.next();
                if (connection.getStatus() == ConnectionStatus.STOPPED) {
                    connection.quit();
                    iter.remove();
                }
            }
        }
    }

    void quit() {
        shouldQuit = true;
    }
}
