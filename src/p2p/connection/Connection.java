package p2p.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import p2p.util.Action;
import p2p.util.Data;
import p2p.util.Debug;

public class Connection implements Runnable {
    
    public static final ConnectionManager MANAGER = ConnectionManager.instance;
    
    private ConnectionFactory connectionAccept;
    private Socket socket;
    private InetAddress addr;
    private int port;
    private NetworkProcess currentProcess;
    
    boolean sentConfirm = false;
    
    public Connection(InetAddress s, int p) {
        addr = s;
        port = p;
    }
    
    public Connection(Socket s, ConnectionFactory c) {
        socket = s;
        connectionAccept = c;
        addr = s.getInetAddress();
        port = s.getPort();
    }
    
    void processFinished() {
        currentProcess = null;
    }
    
    public void send(Data d, NetworkProcess p) {
        if (currentProcess != null)
            Debug.print("Error: currentProcess in connection not reset.");
        currentProcess = p;
        send(d);
    }
    
    public void send(Data d) {
        send(d, 0);
    }
    
    public void send(Data d, int delay) {
        new Thread(() -> {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted!? " + e);
                }
            }
            Debug.print("Sending packet: " + d);
            if (d.type().equals(Data.CONFIRM_JOIN))
                sentConfirm = true;
            try {
                socket.getOutputStream().write(d.buf);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send data: " + e);
            }
        }).start();
    }
    
    @Override
    public void run() {
        if (socket == null) {
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(addr, port));
            } catch (IOException e) {
                throw new RuntimeException("Failed to connect socket: " + e);
            }
            
            Debug.print("Connected to " + addr.getHostAddress() + ":" + port);
            
            send(new Data(Data.FIRST_CONNECTION, String.valueOf(Connection.MANAGER.sockets.size())), 1000);
        }
        
        InputStream recvData;
        byte[] recvBuf = new byte[Data.MAX_BUFFER];
        
        try {
            recvData = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get input stream: " + e);
        }
        
        Debug.print("Ready to recieve.");
        
        while(true) {
            try {
                recvData.read(recvBuf);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get read from stream: " + e);
            }
            
            Data d = new Data(recvBuf);
            Debug.print("Recieved: " + d);
            
            Map<String, String> data = d.interperet();
            switch(data.get(Data.TYPE)) {
                case Data.FIRST_CONNECTION:
                    int numConnections = Integer.valueOf(data.get(Data.NUM_CONNECTIONS));
                    connectionAccept.setConnections(numConnections);
                    break;
                case Data.CONFIRM_JOIN:
                    int actionResult;
                    try {
                        actionResult = Action.suggestAction(new Action(Action.Type.ADD_NEW,
                                InetAddress.getByName(data.get(Data.NEW_IP)),
                                Integer.valueOf(data.get(Data.NEW_PORT)), null));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException("What?! " + e);
                    }
                    if (actionResult == -1)
                        send(new Data(Data.NO_ACKNOWLEDGE));
                    else
                        send(new Data(Data.ACKNOWLEDGE));
                    break;
                case Data.ACKNOWLEDGE:
                    if(currentProcess == null) {
                        Debug.print("Error: Recieved ACK without process.");
                        continue;
                    }
                    currentProcess.response(this, true);
                    break;
                case Data.NO_ACKNOWLEDGE:
                    if(currentProcess == null) {
                        Debug.print("Error: Recieved NACK without process.");
                        continue;
                    }
                    currentProcess.response(this, false);
                    break;
            }
        }
    }
}