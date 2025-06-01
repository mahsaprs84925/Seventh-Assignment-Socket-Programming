package Server;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final List<ClientHandler> allClients;
    private String username;

    public ClientHandler(Socket socket) throws IOException {

        this.socket = socket;
        this.allClients = Server.clients;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            boolean authenticated = false;
            while (!authenticated) {
                String firstLine = in.readUTF();
                if (!"LOGIN".equals(firstLine)) {
                    out.writeUTF("LOGIN-FAILED");
                    out.flush();
                    socket.close();
                    return;
                }
                String username = in.readUTF();
                String password = in.readUTF();
                handleLogin(username, password);
                authenticated = true;
            }
            System.out.println(username + " logged in");

            while (true) {
                String command = in.readUTF();
                switch (command) {
                    case "JOIN-CHAT" -> {
                        String username = in.readUTF();
                        broadcast(username + " joined");
                    }
                    case "CHAT" -> {
                        String message = in.readUTF();
                        broadcast(username + ": " + message);
                    }
                    case "CHAT-EXIT" -> {
                        broadcast(username + " left the chat");
                        out.writeUTF("CHAT-EXIT");
                        out.flush();
                        return;
                    }
                    case "UPLOAD" -> receiveFile();
                    case "DOWNLOAD" -> sendFile();
                    case "LIST-FILES" -> sendFileList();
                }
            }
        } catch (Exception e) {
            System.out.println(username + " disconnected: " + e.getMessage());
        } finally {
            Server.clients.remove(this);
            try {socket.close();} catch (IOException ignored) {}
        }
    }


    private void sendMessage(String msg){
        try {
            out.writeUTF(msg);
            out.flush();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private void broadcast(String msg) throws IOException {
        //TODO: send the message to every other user currently in the chat room
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                if (client != this) {
                    client.sendMessage(msg);
                }
            }
        }
        sendMessage(msg);
    }

    private void sendFileList(){
        String dir = "src/main/resources/Server/Files";
        File[] dirFile = new File(dir).listFiles();
        StringBuilder fileList = new StringBuilder();
        if (dirFile != null) {
            for (File file : dirFile) {
                if (file.isFile()) {
                    fileList.append(file.getName()).append(",");
                }
            }
        }
        else {
            System.out.println("No files found");
        }
        try {
            String filesToSend = fileList.toString();
            if (filesToSend.endsWith(",")) {
                filesToSend = filesToSend.substring(0, filesToSend.length()-1); // Remove final comma
            }
            out.writeUTF(filesToSend);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending file list: " + e.getMessage());
        }
    }
    private void sendFile(){
        try {
            String fileName = in.readUTF();
            File file = new File("src/main/resources/Server/Files", fileName);
            if (!file.exists()) {
                out.writeUTF("FILE_NOT_FOUND");
                out.flush();
                return;
            }
            out.writeUTF("READY");
            out.writeLong(file.length());
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[4096];
                int count;
                while ((count = fis.read(bytes)) > -1) {
                    out.write(bytes, 0, count);
                }
                out.flush();
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private void receiveFile()
    {
        try {
            String fileName = in.readUTF();
            out.writeUTF("READY");
            out.flush();
            long fileSize = in.readLong();

            try (FileOutputStream fos = new FileOutputStream(
                    "src/main/resources/Server/Files/" + fileName)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int count;
                while (totalRead < fileSize && (count = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) > 0) {
                    fos.write(buffer, 0, count);
                    totalRead += count;
                }
            }
            System.out.println("File " + fileName + " uploaded by " + username);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void saveUploadedFile(String filename, byte[] data) throws IOException {
        String dir = "src/main/resources/Server/Files";
        File outFile = new File(dir , filename);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
        }
    }

    private void handleLogin(String username, String password) throws IOException, ClassNotFoundException {
        if (Server.authenticate(username, password)) {
            this.username = username;
            out.writeUTF("LOGIN-SUCCESS");
            out.flush();
        }
        else {
            out.writeUTF("LOGIN-FAILED");
        }
    }

}