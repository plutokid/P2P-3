package p2p.connection;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import p2p.util.Debug;

public class ConnectionManager {
    
    public static ConnectionManager instance = new ConnectionManager();
    
    public List<Connection> sockets;
    
    private ConnectionManager() {
        sockets = new CopyOnWriteArrayList<Connection>();
    }
    
    public void connect(InetAddress destIp, int socketPort, int destPort) {
        Debug.print("Opening connection to " + destIp.getHostAddress() + ":" + destPort);
        Connection c = new Connection(destIp, socketPort);
        sockets.add(c);
        new Thread(c).start();
    }
    
    void start(Connection c) {
        sockets.add(c);
        new Thread(c).start();
    }
}