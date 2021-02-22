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
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Jakob Mearns <me@samjakob.com>
 */
public class ChungusSocket {
    
    private MessageCallback onMessageReceived;
    private CommandCallback onCommandReceived;
    DisconnectCallback onDisconnect;
    PropertyUpdatedCallback onPropertyUpdated;
    TransferOfferCallback onTransferOffer;
    
    private Socket socket;
    private ChungusSocketThread delegate;

    private DataOutputStream out;
    
    public ChungusSocket(MessageCallback onMessageReceived, CommandCallback onCommandReceived, DisconnectCallback onDisconnect, PropertyUpdatedCallback onPropertyUpdated, TransferOfferCallback onTransferOffer){
        this.onMessageReceived = onMessageReceived;
        this.onCommandReceived = onCommandReceived;
        this.onDisconnect = onDisconnect;
        this.onPropertyUpdated = onPropertyUpdated;
        this.onTransferOffer = onTransferOffer;
    }
    
    public void connect(String host, int port, String username){
        try {
            socket = new Socket(InetAddress.getByName(host), port);
            out = new DataOutputStream(socket.getOutputStream());
            
            out.writeInt(ChungusProtocol.SYSTEM_HANDSHAKE);
            out.writeInt(ChungusProtocol.PROTOCOL_VERSION);
            
            out.writeInt(ChungusProtocol.USERNAME);
            out.writeUTF(username);
            
            delegate = new ChungusSocketThread(socket, this);
            delegate.start();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    MessageCallback getMessageReceivedCallback(){
        return onMessageReceived;
    }

    CommandCallback getCommandReceivedCallback(){
        return onCommandReceived;
    }
    
    TransferOfferCallback getTransferOfferedCallback(){
        return onTransferOffer;
    }
    
    public ChungusSocketThread getDelegate(){
        return this.delegate;
    }

    public void sendChatMessage(String message){
        try {
            out.writeInt(ChungusProtocol.CHAT_MESSAGE);
            out.writeUTF(message);
        }catch(Exception ex){
            if(ex instanceof SocketException){
                if(this.isConnected()) this.onDisconnect.execute();
                return;
            }
            
            ex.printStackTrace();
        }
    }

    public void sendCommand(int command){
        try {
            out.writeInt(command);
        }catch(Exception ex){
            if(ex instanceof SocketException){
                if(this.isConnected()) this.onDisconnect.execute();
                return;
            }
            
            ex.printStackTrace();
        }
    }

    public void changeUsername(String oldUsername, String username){
        try {
            out.writeInt(ChungusProtocol.USERNAME);
            out.writeUTF(username);

            out.writeInt(ChungusProtocol.SYSTEM_ANNOUNCEMENT);
            out.writeUTF(oldUsername + " has changed their name to " + username + ".");
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setMotd(String motd){
        try {
            out.writeInt(ChungusProtocol.COMMAND_SET_MOTD);
            out.writeUTF(motd);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public boolean isConnected(){
        return this.socket.isConnected() && !this.socket.isClosed();
    }
    
    public void close(){
        try {
            this.socket.close();
        }catch(IOException ex){
            System.err.println(ex);
        }
    }
    
}

class ChungusSocketThread extends Thread {
    
    private DataInputStream in;
    
    private final Socket socket;
    private ChungusSocket delegateFor;
    
    ChungusSocketThread(Socket socket, ChungusSocket delegateFor){
        this.socket = socket;
        this.delegateFor = delegateFor;
    }
    
    @Override
    public void run(){
        try {
            in = new DataInputStream(socket.getInputStream());
            
            while(socket.isConnected()){
                if(in.available() > 0){
                    int packetId = in.readInt();
                    
                    switch(packetId){
                        case ChungusProtocol.CHAT_MESSAGE:
                            String payload = in.readUTF();
                            this.delegateFor.getMessageReceivedCallback().execute(payload);
                            break;
                        case ChungusProtocol.COMMAND_USERS:
                            String userList = in.readUTF();
                            this.delegateFor.getCommandReceivedCallback().execute(ChungusProtocol.COMMAND_USERS, userList.split(Pattern.quote("|")));
                            break;
                            
                        case ChungusProtocol.PROTO_MASK_SILENT_COMMAND | ChungusProtocol.COMMAND_USERS:
                            userList = in.readUTF();
                            
                            this.delegateFor.onPropertyUpdated.execute(
                                PropertyUpdatedCallback.Property.USER_LIST,
                                userList.split(Pattern.quote("|"))
                            );
                            break;
                            
                        case ChungusProtocol.FT_OFFER:
                            String fileName = in.readUTF();
                            long fileSize = in.readLong();
                            
                            this.delegateFor.getTransferOfferedCallback().execute(fileName, fileSize);
                    }

                }
                
                Thread.sleep(50);
            }
            
            this.delegateFor.onDisconnect.execute();
            this.interrupt();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
}