import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class TankServer{
    int portNum;
    public TankServer(int port){
        portNum = port;
        runServer();
    }

    public void runServer(){
        try{
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.bind(new InetSocketAddress(portNum));
            System.out.println("Server connected.");

            while(true){
                SocketChannel sockChan = channel.accept();

                //Start a thread for each client (max 2)
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        //Code to run in thread
                        runThread(sockChan);
                    }
                });
                t.start();
            }
        }
        catch(Exception e){
            System.out.println("Error in runServer: " + e);
        }
    }

    public void runThread(SocketChannel sc){
        SocketChannel sockChan = sc;
        System.out.println("Thread created.");
    }


    public static void main(String[] args){
        Console cons = System.console();
        String portString = cons.readLine("Port num: ");
        int portNum = Integer.parseInt(portString);
        TankServer t = new TankServer(portNum);

    }
}
