import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static void main(String[] args) throws IOException {

        // added feature to store messages while server is live
        // when a client is added, they get the entire backlog of all messages
        // join, leave, and messages all back logged


        // create server port
        ServerSocket servSocket = new ServerSocket(8080);
        boolean done = false;

        while (!done) {
            // wait for client to connect
            Socket socketToClient = servSocket.accept();

            // once client connects, create a thread for that client and continue waiting
            Thread newThread = new Thread(new myRunnable( socketToClient ));
            connections_.add( newThread );
            newThread.start();
        }
    }
    static ArrayList<Thread> connections_ = new ArrayList<>();
}
