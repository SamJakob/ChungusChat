/*
 * The MIT License
 *
 * Copyright 2019 Sam Jakob Mearns <me@samjakob.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.samjakob.chunguschat.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sam Jakob Mearns <me@samjakob.com>
 */
public class ChungusSocketServer {
    
    // ChungusChat Port
    public static final int CHUNGUS_CHAT_PORT = 42069;
    // ChungusChat File Transfer Port
    public static final int CHUNGUS_CHAT_PORT_FT = 42070;
    
    private static List<ChungusCommunicationDelegate> chungusClients;
    ServerSocket socket;

    String motd;
    
    public ChungusSocketServer(){
        chungusClients = new ArrayList();
        try {
            socket = new ServerSocket(CHUNGUS_CHAT_PORT);
        }catch(IOException ex){
            System.out.println("Failed to initialize server.");
        }
    }
    
    public void host(){
        ChungusSocketServer chungusSocketServer = this;
        
        Thread hostThread = new Thread(() -> {
            try {
                while (!socket.isClosed()){
                    Socket clientConnection = socket.accept();

                    ChungusCommunicationDelegate chungusCommunicationDelegate
                            = new ChungusCommunicationDelegate(clientConnection, chungusSocketServer);
                    chungusCommunicationDelegate.start();
                    chungusClients.add(chungusCommunicationDelegate);
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }
        });
        hostThread.start();
        
        // File Transfer Socket
        try {
            ServerSocket ftSocket = new ServerSocket(CHUNGUS_CHAT_PORT_FT);
            new Thread(() -> {
                try {
                    while (!ftSocket.isClosed()){
                        Socket clientConnection = ftSocket.accept();

                        DataInputStream input = new DataInputStream(clientConnection.getInputStream());
                        
                        String user = input.readUTF();
                        String fileName = input.readUTF();
                        long fileLength = input.readLong();
                        
                        System.out.println("Downloading: " + fileName + " (" + fileLength + " bytes)");
                        
                        File downloadsDirectory = new File("F:\\Downloads");
                        if(!downloadsDirectory.exists()){
                            downloadsDirectory = new File(System.getProperty("user.home") + "/Downloads");
                        }
                        
                        File file = new File(downloadsDirectory, fileName);
                        FileOutputStream fileOut = new FileOutputStream(file);
                        
                        long startTime = System.currentTimeMillis();
                        
                        for(int i = 0; i < fileLength; i++) {
                            fileOut.write(input.read());
                        }
                        fileOut.flush();
                        fileOut.close();
                        
                        long endTime = System.currentTimeMillis();
                        
                        System.out.println("Downloaded in " + (endTime - startTime) + "ms");
                        broadcastFile(user, fileName, fileLength);
                    }
                }catch(IOException ex){
                    ex.printStackTrace();
                }
            }).start();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
     public static void broadcastFile(String user, String name, long fileSize){
        for(ChungusCommunicationDelegate chungusClient : chungusClients){
            chungusClient.sendMessage(user + " has uploaded a file: " + name + " [" + fileSize + " bytes]");
            chungusClient.offerFile(name, fileSize);
        }
    }
    
    public static void broadcastChatMessage(String message){
        for(ChungusCommunicationDelegate chungusClient : chungusClients){
            chungusClient.sendMessage(message);
        }
    }
    
    public static void broadcastSystemMessage(String message){
        for(ChungusCommunicationDelegate chungusClient : chungusClients){
            chungusClient.sendSystemMessage(message);
        }
    }

    public static List<String> getConnectedUsers(){
        List<String> users = new ArrayList<>();

        for(ChungusCommunicationDelegate chungusClient : chungusClients){
            users.add(chungusClient.getUsername());
        }

        return users;
    }

    public void setMotd(String motd) { this.motd = motd; }
    public String getMotd(){ return this.motd; }
    
    public static void remove(ChungusCommunicationDelegate client){
        chungusClients.remove(client);
        broadcastSystemMessage(client.getUsername() + " has disconnected.");
        client.interrupt();
    }
    
}

class ChungusCommunicationDelegate extends Thread {
    
    private final Socket socket;
    private final ChungusSocketServer delegateFor;
    
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private boolean handshakeCompleted;
    
    public ChungusCommunicationDelegate(Socket socket, ChungusSocketServer delegateFor){
        this.socket = socket;
        this.delegateFor = delegateFor;
        this.handshakeCompleted = false;
    }
    
    @Override
    public void run(){
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            connectionLoop: while(socket.isConnected()){
                try {
                    int packetId = in.readInt();
                    int rawPacketId = packetId;
                    
                    boolean isSilentCommand = (packetId >>> 7) > 0;
                    packetId &= 0b01111111;

                    switch(packetId){
                        case ChungusProtocol.SYSTEM_HANDSHAKE:
                            int clientProtocolVersion = in.readInt();
                            out.writeInt(ChungusProtocol.SYSTEM_HANDSHAKE);
                            out.writeInt(ChungusProtocol.PROTOCOL_VERSION);

                            this.handshakeCompleted = clientProtocolVersion == ChungusProtocol.PROTOCOL_VERSION;
                    }

                    if(handshakeCompleted) switch(packetId) {

                        case ChungusProtocol.CHAT_MESSAGE:
                            String payload = in.readUTF();
                            this.delegateFor.broadcastChatMessage(payload);
                            break;

                        case ChungusProtocol.USERNAME:
                            username = in.readUTF();
                            break;

                        case ChungusProtocol.COMMAND_USERS:
                            out.writeInt(rawPacketId);
                            out.writeUTF(String.join("|", this.delegateFor.getConnectedUsers()));
                            break;

                        case ChungusProtocol.SYSTEM_ANNOUNCEMENT:
                            String announcement = in.readUTF();
                            this.delegateFor.broadcastChatMessage("[SYSTEM] " + announcement);
                            break;

                        case ChungusProtocol.COMMAND_SET_MOTD:
                            String motd = in.readUTF();
                            this.delegateFor.setMotd(motd);
                            break;

                        case ChungusProtocol.MOTD:
                            out.writeInt(ChungusProtocol.CHAT_MESSAGE);
                            String serverMotd = this.delegateFor.getMotd();

                            if(serverMotd != null) out.writeUTF("[MOTD] " + this.delegateFor.getMotd());
                            else out.writeUTF("[MOTD] Welcome to " + this.delegateFor.getConnectedUsers().get(0) + "'s server.");
                            break;
                            
                        default:
                            System.out.println("Received unknown command: " + packetId);
                    } else {
                        out.writeInt(ChungusProtocol.CHAT_MESSAGE);
                        out.writeUTF("[SYSTEM] You are not on the correct version. This server is running " + 
                                ChungusProtocolVersionMap.forProtocolVersion(ChungusProtocol.PROTOCOL_VERSION).getVersionName()
                                + ".");
                        out.flush();
                        
                        while(in.available() > 0) in.read();
                        
                        ChungusSocketServer.remove(this);
                        this.socket.close();
                        break connectionLoop;
                    }

                    Thread.sleep(50);
                }catch(Exception ex){
                    if(ex instanceof SocketException){
                        while(in.available() > 0) in.read();
                        
                        ChungusSocketServer.remove(this);
                        this.socket.close();
                        break connectionLoop;
                    }
                    
                    ex.printStackTrace();
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void sendMessage(String message){
        try {
            out.writeInt(ChungusProtocol.CHAT_MESSAGE);
            out.writeUTF(message);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void sendSystemMessage(String message){
        try {
            out.writeInt(ChungusProtocol.CHAT_MESSAGE);
            out.writeUTF("[SYSTEM] " + message);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public String getUsername(){
        return username;
    }
    
    public void offerFile(String name, long size){
        try {
            out.writeInt(ChungusProtocol.FT_OFFER);
            out.writeUTF(name);
            out.writeLong(size);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
}
