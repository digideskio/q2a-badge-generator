package net.taunova.q2a.badge;

import java.util.HashMap;
import java.util.Map;
import net.taunova.q2a.badge.BadgeGenerator.User;

/**
 * Holds user attributes.
 */
public class UserData {
    
    private final String name;
    private final Map<Object, String> attributes;

    public UserData(String name) {
        this.name = name;
        this.attributes = new HashMap<>();
    }

    public String getName() {
        return name;
    }
    
    public void putAttr(Object key, String value) {
        attributes.put(key, value);
    }
    
    public String getAttr(Object key) {
        return attributes.get(key);
    }
    
    public boolean containsAttr(Object key) {
        return attributes.containsKey(key);
    }
    
    public int getUserScore() {
        String points = attributes.get(User.POINTS);
        return Integer.parseInt(points.replace(",", ""));
    }    
    
    @Override
    public String toString() {
        return "User: #" + attributes.get(User.RANK) + "  " + name;
    }
}
