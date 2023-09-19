import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Room {

    private Room( String roomName ){
        roomName_ = roomName;
    }

    public synchronized static Room getRoom( String name ){
        Room room = null;

        // check all rooms for one with the same name, return that room, or create a new one
        // if it doesn't already exist

        for(int i = 0; i < rooms_.size(); i++){
            if(rooms_.get(i).roomName_.equals( name )){
                room = rooms_.get( i );
                return room;
            }
        }
        room = new Room( name );

        // add room to list of all rooms and return the new room
        rooms_.add(room);
        return room;
    }

    public synchronized void addClient(String name, Socket clientSocket) throws IOException {

        // add new client to the list of all clients names and sockets
        clients_.add( clientSocket );
        clientNames_.add( name );

        // chatch new client up with all previous messages from server
        for(int i = 0; i < storedMessages_.size(); i++){
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            out.write(storedMessages_.get(i), 0, storedMessages_.get(i).length);
            out.flush();
        }
    }
    public synchronized void removeClient(Socket clientSocket){

        // remove client socket and corresponding name
        int i = clients_.indexOf(clientSocket);
        clients_.remove(clientSocket);
        clientNames_.remove( i );
    }
    public synchronized void sendMessages( byte[] output ) throws IOException {

        // add message to stored list for this room
        storedMessages_.add( output );

        /// send the message to all clients 1 by 1
        for(int i = 0; i< clients_.size(); i++){
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clients_.get( i ).getOutputStream()));
            out.write(output, 0, output.length);
            out.flush();
        }
    }

    static ArrayList<Room> rooms_ = new ArrayList<>();
    ArrayList<byte[]> storedMessages_ = new ArrayList<>();
    ArrayList<String> clientNames_ = new ArrayList<>();
    ArrayList<Socket> clients_ = new ArrayList<>();
    String roomName_;
}
