import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.PrimitiveIterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private static ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    public Server(){
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
            System.out.println("waiting For Clients.....");
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            try {
                shutdown();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    public static void broadcast(String message){
        for (ConnectionHandler ch: connections) {
            if (ch!= null){
                ch.SendMessage(message);
            }
        }
    }
    public void shutdown() throws IOException {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch: connections) {
                ch.shutdown();
            }
        }
        catch (IOException e){
            // ignore
        }
    }
    static class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please Enter Your NickName: ");
                nickname = in.readLine();
                System.out.println(nickname+"Connected!");
                broadcast(nickname + " has joined the chat");
                String message;
                while((message = in.readLine())!= null){
                    if(message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" ",2);
                        if(messageSplit.length == 2){
                            broadcast(nickname + " Renamed themselves "+ messageSplit[1]);
                            System.out.println(nickname + " Renamed themselves "+ messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed their nickname to "+messageSplit[1]);
                        }
                        else
                            out.println("NO Nickname was provided!");
                    }
                    else if(message.startsWith("/quit")){
                        broadcast(nickname + " left the chat");
                        shutdown();
                        //System.out.println(nickname + " Has left the chat");
                    }
                    else{
                        broadcast(nickname + ": "+message);
                    }
                }
            }
             catch (IOException e){
                shutdown();
            }
        }
        public void SendMessage(String message){
            out.println(message);
        }
        public void shutdown(){
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }
            catch (IOException e){
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}