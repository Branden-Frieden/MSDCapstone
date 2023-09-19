import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.nio.file.*;
import java.awt.image.*;
import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.modality.*;
import ai.djl.modality.cv.*;
import ai.djl.modality.cv.util.*;
import ai.djl.modality.cv.transform.*;
import ai.djl.modality.cv.translator.*;
import ai.djl.repository.zoo.*;
import ai.djl.translate.*;
import ai.djl.training.util.*;

public class WebSocket {

    Room room_ = null;
    boolean done_ = false;

    String fullName_ = "";
    int fullNameSpaces_ = 0;

    public WebSocket(Socket socketToClient) throws IOException {

        DataInputStream webSocket = new DataInputStream(new BufferedInputStream(socketToClient.getInputStream()));

        byte[] DECODED;

        while (!done_) {

            
            // gets the decoded message from the client web socket
            DECODED = DecodePayload(webSocket);

            // generates a response string based on what the client sends
            String outputString = generateResponse(DECODED, socketToClient);

            // creates byte array of the outputString to be sent out
            byte[] output = generateOutput(outputString);

            // sends the message to all the clients in the room
            room_.sendMessages(output);
        }
    }

    private byte[] DecodePayload(DataInputStream webSocket) throws IOException {

        //initialize values
        boolean masked = false;
        boolean fin = false;
        int payloadLength = 0;
        byte[] DECODED;

        //read first two bytes from web socket
        byte b0 = webSocket.readByte();
        byte b1 = webSocket.readByte();

        // check if final message (not implemented yet)
        if ((b0 & 0x80) != 0) {
            fin = true;
        }
        // pull out opCode (not implemented yet)
        byte opCode = (byte) (b0 & 0x0F);

        //check masking bit
        if ((b1 & 0x80) != 0) {
            masked = true;
        }

        // find the length, if over 125, check next bits as appropriate
        byte guessLength = (byte) (b1 & 0x7F);

        if (guessLength == 0x7E) {
            short extendedPayloadLength = webSocket.readShort();
            payloadLength = extendedPayloadLength;
        } else if (guessLength == 0x7F) {
            long extendedPayloadLength = webSocket.readLong();
            payloadLength = (int) extendedPayloadLength;
        } else {
            payloadLength = guessLength;
        }

        if (masked) {

            // get mask from webSocket
            byte[] MASK = webSocket.readNBytes(4);
            // get encoded payload from webSocket
            byte[] ENCODED = webSocket.readNBytes(payloadLength);

            // decode (unmask the payload)
            DECODED = new byte[ENCODED.length];
            for (int i = 0; i < ENCODED.length; i++) {
                DECODED[i] = (byte) (ENCODED[i] ^ MASK[i % 4]);
            }
        } else {
            // if not masked (should always be masked) just pull the data from websocket as decoded
            DECODED = webSocket.readNBytes(payloadLength);
        }
        return DECODED;
    }

    private String generateResponse(byte[] DECODED, Socket socketToClient) throws IOException {

        // initialize return variable
        String outputString = "";

        //get string from decoded byte array
        String str = new String(DECODED, StandardCharsets.UTF_8);

        // pull image from string
        byte[] imageData = Base64.getDecoder().decode(str.substring(str.indexOf(",") + 1));
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
        //ImageIO.write(bufferedImage, "png", new File("img.png"));

        Image image = bufferedImage.getScaledInstance(800, 500, Image.SCALE_DEFAULT);





        /*//get individual words from string
        String[] arr = s.split(" ");

        if (arr[0].equals("join")) {
            // create room on join
            room_ = Room.getRoom(arr[arr.length - 1]);

            // check for name greater than one word
            if (arr.length > 3) {
                for (int i = 1; i < arr.length - 1; i++) {
                    fullName_ += " " + arr[i];
                    fullNameSpaces_++;
                }
                fullName_ = fullName_.substring(1);
            } else {
                fullName_ = arr[1];
                fullNameSpaces_ = 1;
            }
            // add client to room
            room_.addClient(fullName_, socketToClient);

            // create return variable based on name and room name given
            outputString = "{\"type\" :\"join\", \"user\" :\"" + fullName_ + "\",\"room\" :\"" + room_.roomName_
                    + "\"}";

        } else if (arr[0].equals("leave")) {
            // create return variable for user that wants to leave
            outputString = "{\"type\" :\"leave\", \"user\" :\"" + fullName_ + "\",\"room\" :\"" + room_.roomName_
                    + "\"}";

            // remove from room and set looping boolean to false
            room_.removeClient(socketToClient);
            done_ = true;
        } else {
            // initialize name given to websocket
            String givenName = "";
            for(int j = 0; j < fullNameSpaces_; j++){
                givenName += " " +arr[j];
            }
            givenName = givenName.substring(1);


            for (int i = 0; i < room_.clientNames_.size(); i++) {
                // check that the name given matches a name in the server
                if (givenName.equals(room_.clientNames_.get(i))) {

                    //generate return string
                    outputString = "{\"type\" :\"message\", \"user\" :\"" + fullName_ + "\",\"room\" :\"" + room_.roomName_
                            + "\", \"message\" :\"";

                    // add message piece by piece
                    for (int j = fullNameSpaces_; j < arr.length; j++) {
                        outputString += " " + arr[j];
                    }
                    outputString += "\"}";
                }
            }
        }*/
        return outputString;
    }

    private byte[] generateOutput(String outputString) {

        // initialize output variables
        byte[] output = null;
        int startLocation = 2;

        // generate headers based on output size
        if (outputString.length() > (Short.MAX_VALUE - Short.MIN_VALUE)) {
            output = new byte[2 + outputString.length() + 8];
            output[1] = 0x7F;
            long payloadSize = Long.valueOf(outputString.length());
            for (int i = 9; i > 1; i--) {
                output[i] = (byte) payloadSize;
                payloadSize >>= 8;
            }
            startLocation = 10;
        } else if (outputString.length() > 124) {
            output = new byte[2 + outputString.length() + 2];
            output[1] = 0x7E;
            int payloadSize = outputString.length();
            for (int i = 3; i > 1; i--) {
                output[i] = (byte) payloadSize;
                payloadSize >>= 8;
            }
            startLocation = 4;
        } else {
            output = new byte[2 + outputString.length()];
            output[1] = (byte) (outputString.length());
        }

        // set first byte to 1000 0001 for text message
        output[0] = (byte) 0x81;

        // get byte array from output string and put into output after the headers
        byte[] temp = outputString.getBytes();
        for (int i = 0; i < outputString.length(); i++) {
            output[i + startLocation] = temp[i];
        }
        return output;
    }

}

