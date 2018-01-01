package com.nooga.lor1k.io;

import com.nooga.lor1k.devices.UART;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TerminalServer implements IOListener {
    private final UART uart;
    Thread serverThread;
    OutputStream toClient;
    int port;

    public TerminalServer(UART u, int port) {
        this.port = port;
        this.uart = u;
    }

    void handleBytesFromClient(byte[] bs) {
        uart.write(bs);
    }

    public void start() {

        Runnable serverTask = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("TerminalServer: Waiting for clients to connect...");
                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    toClient = clientSocket.getOutputStream();
                    InputStream fromClient = clientSocket.getInputStream();

                    while(!clientSocket.isClosed()) {
                        if(fromClient.available() > 0) {
                            byte[] bs = new byte[fromClient.available()];
                            fromClient.read(bs);
                            handleBytesFromClient(bs);
                        }
                    }

                    toClient = null;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        };

        serverThread = new Thread(serverTask);
        serverThread.start();
    }

    void stop() {
        serverThread.stop();
    }

    @Override
    public void put(byte[] bs) {
        if(toClient != null) {
            try {
                toClient.write(bs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
