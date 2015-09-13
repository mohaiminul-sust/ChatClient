/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mcliclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 *
 * @author Andromeda
 */
public class client_form extends javax.swing.JFrame {

    String username, address = "localhost", fname, sender, fpath;
    ArrayList<String> users = new ArrayList();
    int port = 2610, fport;
    Boolean isConnected = false;
    Socket socket, filesock;
    BufferedReader reader;
    PrintWriter writer;
    OutputStream os;
    DefaultListModel listModel;
    ServerSocket fileservsock;
    InputStream is;
    OutputStream fos;
    boolean recflag = true, fnamehas = false;

    public void listenThread() {
        Thread clientReader = new Thread(new ClientReader());
        clientReader.start();
    }

    public void addUser(String name) {
        if (users.contains(name)) {
            users.remove(name);
        }
        if (listModel.contains(name)) {
            listModel.removeElement(name);
        }
        users.add(name);
        listModel.addElement(name);
    }

    public void removeUser(String name) {
        users.remove(name);
        listModel.removeElement(name);
//        clientStatus.append(name + " is now offline.\n");
    }

    public void writeUsers() {
        String[] tempList = new String[users.size()];
        users.toArray(tempList);

//        for(String temp:tempList){
//            clientStatus.append(temp+"\n");
//        }
    }

    public void sendDisconnect() {
        try {
            writer.println(username + ": :Disconnect");
            writer.flush();
        } catch (Exception e) {
            clientStatus.append("Failed to send disconnect command!!");
        }
    }

    public void disconnect() {
        try {
            clientStatus.append("Disconnected.\n");
            socket.close();
        } catch (Exception e) {
            clientStatus.append("Failed to disconnect!");
        }
        listModel.removeAllElements();
        isConnected = false;
        usernameTextField.setEditable(true);
    }

    public client_form() {
        listModel = new DefaultListModel();
        listModel.addElement("ALL");
        String[] tempList = new String[users.size()];
        users.toArray(tempList);

        for (String temp : tempList) {
            listModel.addElement(temp);
        }

        initComponents();
    }

    public class ClientReader implements Runnable {

        @Override
        public void run() {
            String[] messageParts;
            String inputMessage;

            try {
                while (!(inputMessage = reader.readLine()).isEmpty()) {
                    messageParts = inputMessage.split(":");

                    if (messageParts[2].equals("Chat")) {
                        clientStatus.append(messageParts[0] + ":" + messageParts[1] + "\n");
                        //clientStatus.setCaretPosition(clientStatus.getDocument().getLength());
                    } else if (messageParts[2].equals("Connect")) {
                        clientStatus.removeAll();
                        addUser(messageParts[0]);
                    } else if (messageParts[2].equals("Disconnect")) {
                        removeUser(messageParts[0]);
                    } else if (messageParts[2].equals("Done")) {
                        writeUsers();
                    } else if (messageParts[2].equals("Send")) {
                        int retval = JOptionPane.showConfirmDialog(null, "Do you want to receive file from " + messageParts[0] + "?");
                        sender = messageParts[0];
                        if (retval == JOptionPane.YES_OPTION) {
                            System.out.println("Doing");
                            Thread receiveThread = new Thread(new Receiver());
                            receiveThread.start();
                            System.out.println("Done");

                        }
                    } else if (messageParts[2].equals("File")) {

                        fport = Integer.parseInt(messageParts[1]);
                        sender = messageParts[0];
                        clientStatus.append("received port for file transfer : " + fport + "\n");
                        JFileChooser chooser = new JFileChooser();

                        chooser.setApproveButtonText("SEND");
                        chooser.setControlButtonsAreShown(true);
                        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                        int retval = chooser.showOpenDialog(client_form.this.getParent());

                        if (retval == JFileChooser.APPROVE_OPTION) {
                            fname = chooser.getSelectedFile().getName();
                            fpath = chooser.getSelectedFile().getPath();
                            clientStatus.append("Selected file : " + fpath + "\n");
                            writer.println(username + ":" + fname + ":Fname:" + sender);
                            clientStatus.append("Sending file name : " + fname + "\n");
                            writer.flush();
                        }
                    } else if (messageParts[2].equals("Fname")) {
                        fname = messageParts[1];
                        sender = messageParts[0];
                        clientStatus.append("Got Filename : " + fname + "\n");
                        clientStatus.append("Got Sender : " + sender + "\n");

                        //String message = username +":"+sender+":Go";
                        writer.println(username + ":" + sender + ":Got");
                        writer.flush();
                    } else if (messageParts[2].equals("Got")) {
                        System.out.println("Doing");
                        Thread sendThread = new Thread(new Sender());
                        sendThread.start();
                        System.out.println("Done");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Reader failed!");
            }
        }
    }

    public class Sender implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            clientStatus.append("Transfering.....\n");
            // localhost for testing
            try {
                filesock = new Socket(address, fport);
            } catch (IOException ex) {
                clientStatus.append("Socket failure!");
                Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                fos = filesock.getOutputStream();
            } catch (IOException ex) {
                clientStatus.append("Output Stream  failure!");
                Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {

                try {
                    sendFile(fos);
                } catch (Exception e) {
                    clientStatus.append("Sending failure!");
                    Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, e);
                }
            } catch (Exception ex) {

                clientStatus.append("Sending failure!");
                Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
            }

            long end = System.currentTimeMillis();
            System.out.println(end - start);

            try {
                filesock.close();
                clientStatus.append("Sending Done!!\n");
                clientStatus.append("Transfer time " + (end - start) + " ms\n");
            } catch (IOException ex) {

                clientStatus.append("Socket can't be closed!");
                Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    private void sendFile(OutputStream os) throws Exception {

        File myFile = new File(fpath);
        byte[] mybytearray = new byte[(int) myFile.length() + 1];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(mybytearray, 0, mybytearray.length);
        clientStatus.append("Sending...\n");
        os.write(mybytearray, 0, mybytearray.length);
        os.flush();

    }

    public class Receiver implements Runnable {

        @Override
        public void run() {

            fport = 13267;

            try {

                fileservsock = new ServerSocket(fport);
                clientStatus.append("Waiting...\n");
//                filesock.setSoTimeout(port);
                writer.println(username + ":" + fport + ":" + sender + ":Confd");
                clientStatus.append("Sending port : " + fport + "\n");
                writer.flush();
            } catch (IOException ex) {
                clientStatus.append("Server failed!\n");
                Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
            }
            recflag = true;
            while (recflag) {

                try {
                    filesock = fileservsock.accept();
                    clientStatus.append("Connected!!\n");
                } catch (IOException ex) {
                    clientStatus.append("Socket failed!\n");
                    Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Accepted connection : " + filesock);

                try {

                    is = filesock.getInputStream();
                } catch (IOException ex) {
                    Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    clientStatus.append("Receiving....\n");
                    receiveFile(is);
                } catch (Exception ex) {
                    Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    filesock.close();

                    clientStatus.append("File Received....\n");
                } catch (IOException ex) {
                    Logger.getLogger(client_form.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void receiveFile(InputStream is) throws Exception {
        int filesize = 6022386;
        int bytesRead;
        int current = 0;
        byte[] mybytearray = new byte[filesize];
        String path = System.getProperty("user.dir");
        FileOutputStream fos = new FileOutputStream(path + "/" + fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bytesRead = is.read(mybytearray, 0, mybytearray.length);
        current = bytesRead;

        do {
            bytesRead = is.read(mybytearray, current,
                    (mybytearray.length - current));
            if (bytesRead >= 0) {
                current += bytesRead;
            }
        } while (bytesRead > -1);

        bos.write(mybytearray, 0, current);
        bos.flush();
        bos.close();
        recflag = false;
        fileservsock.close();
        clientStatus.append("Exiting receive mode...\nDone\n");

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        usernameTextField = new javax.swing.JTextField();
        portTextField = new javax.swing.JTextField();
        addressTextField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        disconnectButton = new javax.swing.JButton();
        anonloginButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        clientStatus = new javax.swing.JTextArea();
        chatTextField = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        userList = new javax.swing.JList();
        fileButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        jLabel1.setText("Address : ");

        jLabel2.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        jLabel2.setText("Port : ");

        usernameTextField.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N

        portTextField.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        portTextField.setText("2610");
        portTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portTextFieldActionPerformed(evt);
            }
        });

        addressTextField.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        addressTextField.setText("localhost");

        connectButton.setFont(new java.awt.Font("Segoe UI Light", 1, 14)); // NOI18N
        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        disconnectButton.setFont(new java.awt.Font("Segoe UI Light", 1, 14)); // NOI18N
        disconnectButton.setText("Disconnect");
        disconnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectButtonActionPerformed(evt);
            }
        });

        anonloginButton.setFont(new java.awt.Font("Segoe UI Light", 1, 14)); // NOI18N
        anonloginButton.setText("Anonymous");
        anonloginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                anonloginButtonActionPerformed(evt);
            }
        });

        clientStatus.setColumns(20);
        clientStatus.setFont(new java.awt.Font("Segoe UI Light", 0, 14)); // NOI18N
        clientStatus.setRows(5);
        clientStatus.setToolTipText("Status box");
        jScrollPane1.setViewportView(clientStatus);

        chatTextField.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        chatTextField.setToolTipText("chat box");
        chatTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chatTextFieldActionPerformed(evt);
            }
        });

        sendButton.setFont(new java.awt.Font("Segoe UI Light", 1, 14)); // NOI18N
        sendButton.setText("SEND");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        userList.setFont(new java.awt.Font("Segoe UI Light", 1, 12)); // NOI18N
        userList.setModel(listModel);
        userList.setToolTipText("User List");
        jScrollPane2.setViewportView(userList);

        fileButton.setFont(new java.awt.Font("Segoe UI Light", 1, 14)); // NOI18N
        fileButton.setText("Send File");
        fileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileButtonActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI Light", 0, 18)); // NOI18N
        jLabel5.setText("Username : ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(29, 29, 29)
                                .addComponent(addressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(1, 1, 1)
                                .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(anonloginButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(connectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jScrollPane1)
                    .addComponent(chatTextField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(disconnectButton, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                    .addComponent(fileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(addressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(connectButton)
                            .addComponent(disconnectButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(anonloginButton)
                            .addComponent(fileButton))))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chatTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sendButton)))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        // TODO add your handling code here:
        if (isConnected == false) {
            username = usernameTextField.getText();
            usernameTextField.setEditable(false);
            address = addressTextField.getText();
            port = Integer.parseInt(portTextField.getText());
            try {
                socket = new Socket(address, port);
                InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(inputReader);
                writer = new PrintWriter(socket.getOutputStream());
                writer.println(username + ": has connected.:Connect");
                writer.flush();
                isConnected = true;
            } catch (Exception e) {
                clientStatus.append("Can't connect! Try again!\n");
                usernameTextField.setEditable(true);
                isConnected = false;
            }

            listenThread();
        } else if (isConnected == true) {
            clientStatus.append("You are already connected!");
        }
    }//GEN-LAST:event_connectButtonActionPerformed

    private void disconnectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectButtonActionPerformed
        sendDisconnect();
        disconnect();
    }//GEN-LAST:event_disconnectButtonActionPerformed

    private void anonloginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_anonloginButtonActionPerformed

        usernameTextField.setText("");

        if (isConnected == false) {
            String aname = "ano";
            Random randGen = new Random();

            int random = randGen.nextInt(999) + 1;
            String temp = String.valueOf(random);
            aname = aname.concat(temp);
            username = aname;

            usernameTextField.setText(aname);
            usernameTextField.setEditable(false);

            try {
                socket = new Socket(address, port);
                InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(inputReader);
                writer = new PrintWriter(socket.getOutputStream());
                writer.println(aname + ":has connected.:Connect");
                writer.flush();
                isConnected = true;
            } catch (Exception e) {
                clientStatus.append("Can't connect! Try again!\n");
                usernameTextField.setEditable(true);
                isConnected = false;
            }

            listenThread();
        } else if (isConnected = true) {
            clientStatus.append("You are already cponnected!\n");
        }
    }//GEN-LAST:event_anonloginButtonActionPerformed

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
       if (userList.getSelectedValue() == null) {

            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {
                    writer.println(username + ":" + chatTextField.getText() + ":Chat");
                    writer.flush();
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();

        } else if(userList.getSelectedValue() != null && userList.getSelectedValue().equals("ALL")){
            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {
                    writer.println(username + ":" + chatTextField.getText() + ":Chat");
                    writer.flush();
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();
        
        }else if (userList.getSelectedValue() != null && !userList.getSelectedValue().equals("ALL")) {

            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {

                    writer.println(username + ":" + chatTextField.getText() + ":Chat2:" + userList.getSelectedValue());
                    writer.flush();
                    clientStatus.append(username + ":" + chatTextField.getText() + "\n");
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();
        }


    }//GEN-LAST:event_sendButtonActionPerformed

    private void chatTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chatTextFieldActionPerformed
        // TODO add your handling code here:
        if (userList.getSelectedValue() == null) {

            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {
                    writer.println(username + ":" + chatTextField.getText() + ":Chat");
                    writer.flush();
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();

        } else if(userList.getSelectedValue() != null && userList.getSelectedValue().equals("ALL")){
            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {
                    writer.println(username + ":" + chatTextField.getText() + ":Chat");
                    writer.flush();
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();
        
        }else if (userList.getSelectedValue() != null && !userList.getSelectedValue().equals("ALL")) {

            if (chatTextField.getText().equals("")) {
                chatTextField.setText("");
                chatTextField.requestFocus();
            } else {
                try {

                    writer.println(username + ":" + chatTextField.getText() + ":Chat2:" + userList.getSelectedValue());
                    writer.flush();
                    clientStatus.append(username + ":" + chatTextField.getText() + "\n");
                } catch (Exception e) {
                    clientStatus.append("Message not send!");
                }
            }
            chatTextField.setText("");
            chatTextField.requestFocus();
        }

    }//GEN-LAST:event_chatTextFieldActionPerformed

    private void fileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileButtonActionPerformed
        // TODO add your handling code here:
        if (userList.getSelectedValue() != null) {

            //connection
            try {
                writer.println(username + ":" + userList.getSelectedValue() + ":Send");
                writer.flush();
            } catch (Exception e) {
                clientStatus.append("Connection message not sent!!\n");
            }

            //File section
        } else {
            clientStatus.append("Please select a user!!\n");
        }
    }//GEN-LAST:event_fileButtonActionPerformed

    private void portTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_portTextFieldActionPerformed


    

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.noire.NoireLookAndFeel");
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(client_form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(client_form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(client_form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(client_form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new client_form().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addressTextField;
    private javax.swing.JButton anonloginButton;
    private javax.swing.JTextField chatTextField;
    private javax.swing.JTextArea clientStatus;
    private javax.swing.JButton connectButton;
    private javax.swing.JButton disconnectButton;
    private javax.swing.JButton fileButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField portTextField;
    private javax.swing.JButton sendButton;
    private javax.swing.JList userList;
    private javax.swing.JTextField usernameTextField;
    // End of variables declaration//GEN-END:variables
}
