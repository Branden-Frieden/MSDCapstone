import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HTTPRequest {

    String filename;

    Socket socketToClient;

    HashMap<String, String> properties_;

    public HTTPRequest(Socket clientSocket) throws IOException {

        socketToClient = clientSocket;
        InputStream inputstream = socketToClient.getInputStream();

        Scanner sc = new Scanner(inputstream);

        //only read the 1st line once

        // break line into 3 pieces
        sc.next();
        filename = sc.next();
        if(filename.equals("/"))
        {
            filename = "index.html";
        } else
        {
            filename = filename.substring(1);
        }

        String line = sc.nextLine();
        properties_ = new HashMap<String, String>();
        while(!line.isEmpty()) {

            // read header line
            line = sc.nextLine();

            //break ‘line’ into key: value pairs
            String[] pairs = line.split(": ");

            if(pairs.length == 2) {
                // store in hash map
                properties_.put(pairs[0], pairs[1]);
            }

        }
    }
    public String HTTPgetFileName(){
        return filename;
    }

    public Socket HTTPgetClientSocket(){
        return socketToClient;
    }

    public HashMap<String, String> HTTPgetProperties(){
        return properties_;
    }
}
