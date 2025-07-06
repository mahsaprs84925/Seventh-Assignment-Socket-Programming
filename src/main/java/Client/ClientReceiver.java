package Client;


import java.io.DataInputStream;

public class ClientReceiver implements Runnable {
    private DataInputStream in;
    public ClientReceiver(DataInputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = in.readUTF();
                System.out.println("Server - Other: " + message);
                if (message.equals("EXIT")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("DISCONNECTED");
        }
    }

}