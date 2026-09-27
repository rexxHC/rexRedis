package com.rexredis.store;

/**
 * Wraps a stored value with its type tag.
 * The actual value is one of: String, LinkedList&lt;String&gt;, HashSet&lt;String&gt;, HashMap&lt;String,String&gt;.
 */
public class RedisValue {

    public enum Type {
        STRING, LIST, SET, HASH
    }

    private final Type type;
    private Object value;

    public RedisValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static RedisValue string(String value) {
        return new RedisValue(Type.STRING, value);
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
