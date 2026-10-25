package framework;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Session {
    private final String id;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final long creationTime;
    private volatile long lastAccessTime;
    private volatile boolean valid = true;

    public Session(String id) {
        this.id = Objects.requireNonNull(id, "id");
        this.creationTime = System.currentTimeMillis();
        this.lastAccessTime = creationTime;
    }

    public String getId() {
        return id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void touch() {
        lastAccessTime = System.currentTimeMillis();
    }

    public Object get(String name) {
        return attributes.get(name);
    }

    public <T> T get(String name, Class<T> type) {
        Object value = attributes.get(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void set(String name, Object value) {
        Objects.requireNonNull(name, "name");
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    public void put(String name, Object value) {
        set(name, value);
    }

    public void remove(String name) {
        attributes.remove(name);
    }

    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
        attributes.clear();
    }

    public boolean isExpired(long timeoutMillis) {
        return !valid || System.currentTimeMillis() - lastAccessTime > timeoutMillis;
    }
}
