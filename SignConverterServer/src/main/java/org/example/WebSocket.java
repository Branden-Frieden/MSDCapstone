package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


import java.io.IOException;


public class WebSocket {
    boolean done_ = false;

    public WebSocket(Socket socketToClient) throws IOException {

        DataInputStream webSocket = new DataInputStream(new BufferedInputStream(socketToClient.getInputStream()));
        //DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socketToClient.getOutputStream()));
        OutputStream out = socketToClient.getOutputStream();
        byte[] DECODED;

        while (!done_) {
            // gets the decoded message from the client web socket
            DECODED = DecodePayload(webSocket);

            // generates a response string based on what the client sends
            String outputString = generateResponse(DECODED);

            if(outputString.equals("left")){
                break;
            }

            // creates byte array of the outputString to be sent out
            byte[] output = generateOutput(outputString);

            // send the message

            out.write(output, 0, output.length);
            out.flush();
        }
    }

    private byte[] DecodePayload(DataInputStream webSocket) throws IOException {

        //initialize values
        boolean masked = false;
        boolean fin = false;
        int payloadLength;
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
            payloadLength = webSocket.readShort();
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

    private String generateResponse(byte[] DECODED) throws IOException {

        // initialize return variable
        String outputString;

        //get string from decoded byte array
        String str = new String(DECODED, StandardCharsets.UTF_8);
        System.out.println(str);
        if(str.equals("leave")){
            done_ = true;
            return "left";
        }
        byte[] decodedBytes;

        // pull image from string
        try {
            decodedBytes = Base64.getDecoder().decode(str.substring(str.indexOf(",") + 1));
            String decodedString = new String(decodedBytes);
            System.out.println("Decoded String: " + decodedString);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(decodedBytes));
            Image image = bufferedImage.getScaledInstance(800, 500, Image.SCALE_DEFAULT);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Base64-encoded string: " + e.getMessage());
        }

        // analyze the image

        /////////////////////////////// TO DO!

        // generate output string

        outputString = "{\"letter\" :\"" + "C" + "\",\"confidence\" :\"" + "94%" + "\"}";

        System.out.println(outputString);
        return outputString;
    }

    private byte[] generateOutput(String outputString) {

        // initialize output variables
        byte[] output;
        int startLocation = 2;

        // generate headers based on output size
        if (outputString.length() > (Short.MAX_VALUE - Short.MIN_VALUE)) {
            output = new byte[2 + outputString.length() + 8];
            output[1] = 0x7F;
            long payloadSize = outputString.length();
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
        System.arraycopy(temp, 0, output, startLocation, outputString.length());
        return output;
    }

}

