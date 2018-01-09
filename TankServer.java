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
    private ServerController control;
    boolean running = true;


    public TankServer(int port, ServerController c){
        portNum = port;
        control = c;
        clients = new ArrayList<>();
        count = 0;
        pastTime = System.currentTimeMillis();
        waitTime = r.nextInt(15000) + 15000;

        Thread myThread = new Thread(new Runnable() {
            public void run() {
                runServer();
            }
        });
        myThread.start();
    }

    public void endServer(){
        try{
            running = false;
            d1.disconnect();
            d1.close();

        }
        catch(Exception e){
            control.addMessage("Error in endServer: " + e);
        }
    }

    public int getClientSize(){
        return clients.size();
    }

    public void runServer(){
        try{
            d1 = DatagramChannel.open();
            d1.bind(new InetSocketAddress(portNum));
            control.addMessage("Server connected.");

            while(running){
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                SocketAddress currentAddress = d1.receive(buffer);
                buffer.flip();
                //System.out.println("Packet received");
                //Limit the number of players to the max.
                if (clients.size() < 1){
                    clients.add(currentAddress);
                    control.addMessage("Client added.");
                    ByteBuffer firstBuff = ByteBuffer.wrap("F".getBytes());
                    d1.send(firstBuff, currentAddress);
                }
                else if (clients.size() < MAX_PLAYERS){
                    if (clients.get(0) != (currentAddress)){
                        clients.add(currentAddress);
                        control.addMessage("Client added.");
                    }
                }
                boolean changed = false;

                //Send the packets to the clients.
                for (SocketAddress addr : clients){
                    if (!addr.equals(currentAddress)){
                        buffer.position(0);
                        d1.send(buffer, addr);
                        //System.out.println("Packet sent: " + addr);
                    }
                    //Send a power up every x seconds.
                    if (System.currentTimeMillis() - pastTime > waitTime){
                        changed = true;
                        waitTime = r.nextInt(15000) + 15000;
                        int newType = r.nextInt(5);
                        String tempPow = "1," + newType;
                        ByteBuffer powerBuff = ByteBuffer.wrap(tempPow.getBytes());
                        d1.send(powerBuff, addr);
                    }
                }
                if (changed)
                    pastTime = System.currentTimeMillis();
            }
            control.addMessage("Server disconnected.");

        }
        catch(Exception e){
            control.addMessage("Error in runServer: " + e);
        }
    }
}
