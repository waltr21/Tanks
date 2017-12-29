import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Random;

public class TankServer{
    private int portNum, count;
    private final int MAX_PLAYERS = 2;
    private final int BUFFER_SIZE = 1024;
    private DatagramChannel d1;
    private ArrayList<SocketAddress> clients;
    private Random r = new Random();
    private long pastTime;
    private int waitTime;


    public TankServer(int port){
        portNum = port;
        clients = new ArrayList<>();
        count = 0;
        pastTime = System.currentTimeMillis();
        waitTime = r.nextInt(15000) + 15000;
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
                buffer.flip();
                //System.out.println("Packet received");
                //Limit the number of players to the max.
                if (clients.size() < 1){
                    clients.add(currentAddress);
                    System.out.println("Client added.");
                    ByteBuffer firstBuff = ByteBuffer.wrap("F".getBytes());
                    d1.send(firstBuff, currentAddress);
                }
                else if (clients.size() < MAX_PLAYERS){
                    if (clients.get(0) != (currentAddress)){
                        clients.add(currentAddress);
                        System.out.println("Client added.");
                    }
                }
                boolean changed = false;
                int newType = r.nextInt(2);

                //Send the packets to the clients.
                for (SocketAddress addr : clients){
                    if (!addr.equals(currentAddress)){
                        buffer.position(0);
                        d1.send(buffer, addr);
                        //System.out.println("Packet sent: " + addr);
                    }
                    //Send a power up every x seconds.
                    if (System.currentTimeMillis() - pastTime > waitTime
                        && clients.size() == MAX_PLAYERS){
                        changed = true;
                        String tempPow = "1," + newType;
                        ByteBuffer powerBuff = ByteBuffer.wrap(tempPow.getBytes());
                        d1.send(powerBuff, addr);
                    }
                }
                if (changed)
                    pastTime = System.currentTimeMillis();
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