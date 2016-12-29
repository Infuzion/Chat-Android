/*
 *  Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package android.infuzion.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Client implements Runnable {
    private final String ip;
    private final int port;
    private final String username;
    private final List<Runnable> scheduled;
    private DataInputStream input;
    private DataOutputStream output;
    private Map<UUID, String> uuidStringMap = new HashMap<>();
    private volatile boolean disconnected = false;
    private volatile boolean disconnectHandled = false;
    private Timer heartbeat;
    private ChatMessageListener messageListener;

    Client(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        scheduled = new ArrayList<>();
    }

    private void disconnection(Throwable throwable) {
    }

    public void sendMessage(String message) {
        if (message.startsWith("/")) {
            sendData(message, DataType.Command);
        } else {
            sendData(message, DataType.Message);
        }

    }

    @SuppressWarnings("Duplicates")
    public void sendData(String data, DataType type) {
        try {
            output.writeByte(type.byteValue);
            output.writeUTF(data);
            output.writeByte(DataType.EndOfData.byteValue);
        } catch (IOException e) {
            e.printStackTrace();
            disconnection(e);
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            Socket sock = new Socket(ip, port);
            input = new DataInputStream(sock.getInputStream());
            output = new DataOutputStream(sock.getOutputStream());
            output.writeByte(DataType.ClientHello.byteValue);
            output.writeUTF(username);
            output.writeByte(DataType.EndOfData.byteValue);

            heartbeat = new Timer();
            heartbeat.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        output.writeByte(DataType.Heartbeat.byteValue);
                        output.writeUTF("heart");
                        output.writeByte(DataType.EndOfData.byteValue);
                    } catch (IOException e) {
                        this.cancel();
                        e.printStackTrace();
                        disconnection(e);
                    }
                }
            }, 10, 5000);

            try {
                while (true) {
                    synchronized (scheduled) {
                        Iterator<Runnable> it = scheduled.iterator();
                        while (it.hasNext()) {
                            it.next().run();
                            it.remove();
                        }
                    }
                    if (input.available() <= 0) {
                        continue;
                    }
                    byte messageType = input.readByte();
                    String message = input.readUTF();
                    byte end = input.readByte();
                    if (end != DataType.EndOfData.byteValue) {
                        continue;
                    }
                    DataType mType = DataType.valueOf(messageType);
                    if (mType == null) {
                        continue;
                    }

                    if (mType.equals(DataType.Message)) {
                        System.out.println(message);
                        alertListener(message);
                    }
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                disconnection(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            disconnection(e);
        }
    }

    public void scheduleTask(Runnable runnable) {
        synchronized (scheduled) {
            scheduled.add(runnable);
        }
    }

    private void alertListener(String message) {
        if (messageListener != null) {
            messageListener.run(message);
        }
    }

    public void setMessageListener(ChatMessageListener messageListener) {
        this.messageListener = messageListener;
    }
}