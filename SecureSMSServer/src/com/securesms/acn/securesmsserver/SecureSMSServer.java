package com.securesms.acn.securesmsserver;

import javafx.application.Platform;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.TrayIcon;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class SecureSMSServer {
    String startString = ". . .";
    static final int socketServerPORT = 6323;
    static final int MSG_TIME = 3000;
    Socket socket = null;
    ServerSocket inputSocket = null;
    Thread thread = null;
    TrayIcon trayIcon = null;
    MenuItem connectedItem = null;
    DataOutputStream out = null;
    DataInputStream in = null;
    Image image;
    private Controller controller;

    /**
     * Create the application.
     */
    @SuppressWarnings("resource")
    public SecureSMSServer(Controller controller, Crypto crypto) {
        this.controller = controller;
        OpenServer(crypto);
    }


    public void OpenServer(Crypto crypto) {
        thread = new Thread(new ServerThread(crypto));
        thread.start();
    }

    class ServerThread implements Runnable
    {
        private Crypto crypto;
        ServerThread(Crypto crypto) {
            this.crypto = crypto;
        }
        @Override
        public void run()
        {
            try
            {
                inputSocket = new ServerSocket(socketServerPORT);
                while(true)
                {
                    try
                    {
                        socket = inputSocket.accept();
                        in = new DataInputStream(socket.getInputStream());
                        out = new DataOutputStream(socket.getOutputStream());
                        List<String> inputs = new ArrayList<String>();
                        String input;
                        boolean newMessage = false, newQRcode = false;

                        while((input = in.readUTF()) != null)
                        {
                            //if(input.equals("SMS END") || input.equals("QR CODE END"))
                            if(newMessage || newQRcode)
                                inputs.add(input);
                            if((newMessage && inputs.size() == 6) || (newQRcode && inputs.size() == 1))
                                break;
                            if(!newMessage && !newQRcode && input.equals("SMS BEGIN"))
                                newMessage = true;
                            if(!newMessage && !newQRcode && input.equals("QR CODE BEGIN"))
                                newQRcode = true;
                        }

                        if(newMessage && inputs.size() != 6)
                            continue;
                        if(newQRcode && inputs.size() == 1) {
                            String ip = inputs.get(0);
                            //System.out.println("QR code received: " + inputs.get(0));
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    controller.alreadyExists(ip, true);
                                    controller.loadMessageFrame();
                                }
                            });
                            continue;
                        }

                        final List<String> finalInputs = new ArrayList<>();

                        String msgNumberBase64 = inputs.get(0).trim();
                        crypto.setUpCrypto(msgNumberBase64);
                        for (int i = 1; i < inputs.size(); ++i){
                            finalInputs.add(crypto.decodeAndDecrypt(inputs.get(i).trim()));
                        }
                        SecureMessage message = new SecureMessage(finalInputs.get(2), finalInputs.get(0), finalInputs.get(1), finalInputs.get(3), finalInputs.get(4));

                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                controller.appendMessage(message);
                            }
                        });
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}