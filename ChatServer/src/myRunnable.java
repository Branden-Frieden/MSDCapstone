import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class myRunnable implements Runnable {
    private Socket socketToClient;
    myRunnable(Socket clientSocket){
        socketToClient = clientSocket;
    }

    @Override
    public void run() {
        try {

            HTTPRequest req = new HTTPRequest(socketToClient);
            new HTTPResponse(req.HTTPgetFileName(), req.HTTPgetClientSocket(), req.HTTPgetProperties());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
