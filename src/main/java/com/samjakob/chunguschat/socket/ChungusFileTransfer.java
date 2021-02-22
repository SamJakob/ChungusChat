package com.samjakob.chunguschat.socket;


import com.samjakob.chunguschat.socket.ChungusSocketServer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author samuel.mearns
 */
public class ChungusFileTransfer extends Thread {
    
    String username;
    String host;
    File file;
    long fileSize;
    
    public ChungusFileTransfer(String username, String host, File file, long fileSize){
        this.username = username;
        this.host = host;
        this.file = file;
        this.fileSize = fileSize;
    }
    
    @Override
    public void run(){
        try {
            Socket socket = new Socket(InetAddress.getByName(host), ChungusSocketServer.CHUNGUS_CHAT_PORT_FT);
            
            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                output.writeUTF(username);
                output.writeUTF(file.getName());
                output.writeLong(fileSize);
                output.write(Files.readAllBytes(file.toPath()));
            } catch (IOException ex){
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
