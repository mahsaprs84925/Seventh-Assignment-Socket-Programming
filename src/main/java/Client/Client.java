package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static DataInputStream in;
    private static DataOutputStream out;
    private static String username;
    public static void main(String[] args) throws Exception {

        try (Socket socket = new Socket("localhost", 12345)) {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);

            System.out.println("===== Welcome to CS Music Room =====");


            boolean loggedIn = false;
            while (!loggedIn) {
                System.out.print("Username: ");
                String inputUsername = scanner.nextLine();
                System.out.print("Password: ");
                String password = scanner.nextLine();


                sendLoginRequest(inputUsername, password);


                String response = in.readUTF();
                if ("LOGIN-SUCCESS".equals(response)) {
                    loggedIn = true;
                    username = inputUsername;
                    System.out.println("Logged in successfully");
                }
                else {
                    System.out.println("Login failed");
                }
            }

            // --- ACTION MENU LOOP ---
            while (true) {
                printMenu();
                System.out.print("Enter choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1" -> enterChat(scanner);
                    case "2" -> uploadFile(scanner);
                    case "3" -> requestDownload(scanner);
                    case "0" -> {
                        System.out.println("Exiting...");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Enter chat box");
        System.out.println("2. Upload a file");
        System.out.println("3. Download a file");
        System.out.println("0. Exit");
    }

    private static void sendLoginRequest(String username, String password) throws IOException {

        try {
            out.writeUTF("LOGIN");
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private static void enterChat(Scanner scanner) throws IOException {
        System.out.println("You have entered the chat ");



        out.writeUTF("JOIN-CHAT");
        out.writeUTF(username);
        out.flush();

        Thread recieverThread = new Thread(new ClientReceiver(in));
        recieverThread.start();

        while (true) {
            String message = scanner.nextLine();
            if (message.equals("CHAT-EXIT")) {
                out.writeUTF("CHAT-EXIT");
                out.flush();
                break;
            }
            else {
                out.writeUTF("CHAT");
                out.writeUTF(message);
            }
        }
    }

    private static void sendChatMessage(String message_to_send) throws IOException {
        out.writeUTF(username + ": " + message_to_send);
        out.flush();
    }

    private static void uploadFile(Scanner scanner) throws IOException {


        String userDir = "src/main/resources/Client/" + username;
        File[] files = new File(userDir).listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files to upload.");
            return;
        }

        System.out.println("Select a file to upload:");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i].getName());
        }

        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        if (choice < 0 || choice >= files.length) {
            System.out.println("Invalid choice.");
            return;
        }

        File file = files[choice];

        out.writeUTF("UPLOAD");
        out.writeUTF(file.getName());
        out.flush();

        String serverResponse = in.readUTF();
        if (!serverResponse.equals("READY")) {
            System.out.println("server is not ready for file");
            return;
        }

        out.writeLong(file.length());
        out.flush();

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            long sent = 0;
            int count;
            while ((count = fileInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, count);
                sent += count;
            }
            out.flush();
            System.out.println("Uploaded " + sent + " bytes");
        }

    }

    private static void requestDownload(Scanner scanner) throws IOException {
        out.writeUTF("LIST-FILES");
        out.flush();
        String fileList = in.readUTF();
        if (fileList.isEmpty()) {
            System.out.println("No files to request.");
            return;
        }

        String[] files = fileList.split(",");
        System.out.println("Available files: ");
        for (int i = 0; i < files.length; i++) {
            System.out.println((i + 1) + ". " + files[i]);
        }
        System.out.print("Enter file number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        }
        catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }
        if (choice < 0 || choice >= files.length) {
            System.out.println("Invalid choice.");
            return;
        }

        String file = files[choice];
        out.writeUTF("DOWNLOAD");
        out.writeUTF(file);
        out.flush();

        String serverResponse = in.readUTF();
        if (!serverResponse.equals("READY")) {
            System.out.println("server could not send file");
            return;
        }

        long fileSize = in.readLong();
        String dir = "src/main/resources/Client/" + username;
        File outfile = new File(dir , file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(outfile)) {
            byte[] buffer = new byte[4096];
            long received = 0;
            int count;
            while (received < fileSize && (count = in.read(buffer , 0 , (int) Math.min(buffer.length , fileSize - received))) > 0) {
                fileOutputStream.write(buffer, 0, count);
                received += count;
            }
            System.out.println("downloaded " + received + " bytes");
        }
    }
}