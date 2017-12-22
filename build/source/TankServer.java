import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;

public class TankServer{
    private int portNum, count;
    private final int MAX_PLAYERS = 2;
    private final int BUFFER_SIZE = 1024;
    private DatagramChannel d1;
    private ArrayList<SocketAddress> clients;


    public TankServer(int port){
        portNum = port;
        clients = new ArrayList<>();
        count = 0;
        runServer();
    }

    public void runServer(){
        try{
            d1 = DatagramChannel.open();
            d1.bind(new InetSocketAddress(portNum));
            System.out.println("Server connected.");

            while(true){
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                SocketAddress currentAddress = d1.receive(buffer);
                System.out.println("Packet received");
                //Limit the number of players to the max.
                if (clients.size() < MAX_PLAYERS){
                    clients.add(currentAddress);
                }
                for (SocketAddress addr : clients){
                    if (addr != currentAddress){
                        d1.send(buffer, addr);
                        System.out.println("Packet sent: " + addr);
                    }
                }
            }

        }
        catch(Exception e){
            System.out.println("Error in runServer: " + e);
        }
    }

    public static void main(String[] args){
        Console cons = System.console();
        String portString = cons.readLine("Port num: ");
        int portNum = Integer.parseInt(portString);
        TankServer t = new TankServer(portNum);

    }
}