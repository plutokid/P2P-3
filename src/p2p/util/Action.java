package p2p.util;

import java.net.InetAddress;
import java.util.Objects;
import p2p.connection.Connection;

public class Action {
    
    public enum Type {
        ADD_NEW;
    }
    
    public static final int ADDED = 1;
    public static final int EXISTED = 0;
    public static final int REJECTED = -1;
    
    private static Action current = null;
    
    public final Type type;
    public final Connection conn;
    public final InetAddress ip;
    public final int port;
    
    public Action(Type a, InetAddress i, int p, Connection c) {
        type = a;
        ip = i;
        port = p;
        conn = c;
    }
    
    public static int suggestAction(Action a) {
        if (a == null) {
            current = null;
            return ADDED;
        }
        if (current == null) {
            current = a;
            return ADDED;
        } else if (current.equals(a)) {
            return EXISTED;
        } else {
            return REJECTED;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Action))
            return false;
        Action a = (Action) o;
        return type.equals(a.type) && ip.equals(a.ip) && port==a.port;
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.type);
        hash = 83 * hash + Objects.hashCode(this.ip);
        hash = 83 * hash + this.port;
        return hash;
    }
}
