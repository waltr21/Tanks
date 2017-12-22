import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class TankServer{
    int portNum;
    DatagramChannel d1;
    int count;

    public TankServer(int port){
        portNum = port;
        count = 0;
        runServer();
    }

    public void runServer(){
        try{
            d1 = DatagramChannel.open();
            d1.bind(new InetSocketAddress(portNum));
            System.out.println("Server connected.");

            while(true){
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                SocketAddress address = d1.receive(buffer);
                System.out.println("Packet received");
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