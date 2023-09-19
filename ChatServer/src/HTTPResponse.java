import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;

public class HTTPResponse {

    public HTTPResponse(String filename, Socket socketToClient, HashMap<String,String> properties) throws IOException, InterruptedException, NoSuchAlgorithmException {

        OutputStream outputStream = socketToClient.getOutputStream();

        PrintWriter pw = new PrintWriter(outputStream);
        if (properties.get("Connection").contains("Upgrade")) {

            // send websocket handshake headers
            pw.println("HTTP/1.1 101 Switching Protocols");
            pw.println("Upgrade: websocket");
            pw.println("Connection: keep-alive, Upgrade");
            String accept = properties.get("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

            // create key for web socket header
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            byte[] data = accept.getBytes();
            String webSocketAccept = Base64.getEncoder().encodeToString(md.digest( data ));

            pw.println("Sec-WebSocket-Accept: " + webSocketAccept);
            pw.println("");
            pw.flush();

            // create web socket handler( has a loop that will run until left)
            new WebSocket(socketToClient);
            socketToClient.close();

        } else {

            try {
                // open the requested file ( filename)
                File file = new File(filename);
                String result;
                if (file.isFile()) {
                    result = "200 OK";
                } else {
                    result = "404 not found";
                }

                // send response header
                pw.println("HTTP/1.1 " + result);

                // send back content-type
                // determine file extension (html or css) and use it here
                pw.println("Content-Type: text/" + filename.substring(filename.lastIndexOf(".") + 1) + "; charset=UTF-8");
                pw.println("Content-Length: " + file.length());
                pw.println(); // ends the response header with blank line

                FileInputStream fileInput = new FileInputStream(file);
                Scanner fileReader = new Scanner(fileInput);

                while (fileReader.hasNextLine()) {
                    pw.println(fileReader.nextLine());
                }

                // send the data from the file
                pw.close();
                socketToClient.close();
            } catch (IOException e) {
                String result = "404 not found";
                System.out.println(e.getMessage());
                pw.println("HTTP/1.1 " + result);
                pw.println(e.getMessage());
                pw.println();
                pw.close();
                socketToClient.close();
            }
        }
    }


}