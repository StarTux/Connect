package com.winthier.connect;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class Client implements Runnable {
    private final Connect connect;
    private final String name;
    private final int port;
    private final String displayName;
    private DataOutputStream out = null;
    private boolean shouldQuit = false;
    private boolean shouldSkipSleep = false;
    private LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private String current = null;
    private ConnectionStatus status = ConnectionStatus.INIT;

    @Override
    public void run() {
        mainLoop();
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) { }
            out = null;
        }
        status = ConnectionStatus.STOPPED;
    }

    void sleep(int seconds) {
        for (int i = 0; i < seconds; ++i) {
            if (shouldSkipSleep) {
                shouldSkipSleep = false;
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) { }
            if (shouldQuit) return;
        }
    }

    void mainLoop() {
        while (!shouldQuit) {
            Message message = null;
            try {
                message = queue.poll(10, TimeUnit.SECONDS);
            } catch (InterruptedException ie) { }
            if (shouldQuit) return;
            if (message != null) {
                sendLoop(message);
            }
        }
    }

    void sendLoop(Message message) {
        while (!shouldQuit) {
            cleanQueue();
            DataOutputStream dos = getOut();
            if (dos == null) continue;
            try {
                if (message.tooOld()) return;
                dos.writeUTF(message.serialize());
                dos.flush();
            } catch (IOException ioe) {
                status = ConnectionStatus.DISCONNECTED;
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException ioe2) { }
                }
                this.out = null;
                connect.getHandler().handleClientDisconnect(this);
                continue;
            }
            return;
        }
    }

    void cleanQueue() {
        synchronized (queue) {
            for (Iterator<Message> iter = queue.iterator(); iter.hasNext();) {
                if (iter.next().tooOld()) iter.remove();
            }
        }
    }

    DataOutputStream getOut() {
        while (!shouldQuit) {
            if (out == null) {
                try {
                    Socket socket = new Socket("localhost", port);
                    if (socket.isConnected()) {
                        out = new DataOutputStream(socket.getOutputStream());
                        Server server = connect.getServer();
                        out.writeUTF(connect.getName());
                        if (server != null) {
                            out.writeUTF("" + server.getPort());
                        } else {
                            out.writeUTF("-1");
                        }
                        out.writeUTF(connect.getPassword());
                        out.flush();
                        status = ConnectionStatus.CONNECTED;
                        connect.getHandler().handleClientConnect(this);
                    }
                } catch (IOException ioe) {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ioe2) { }
                    }
                    out = null;
                }
                if (out == null) {
                    status = ConnectionStatus.DISCONNECTED;
                    sleep(10);
                    return null;
                }
            }
            break;
        }
        return out;
    }

    void quit() {
        shouldQuit = true;
        queue.offer(ConnectionMessages.QUIT.message(this));
    }

    void skipSleep() {
        shouldSkipSleep = true;
    }

    void send(Message msg) {
        queue.offer(msg);
    }

    public int getQueueSize() {
        return queue.size();
    }
}
