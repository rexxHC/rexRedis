package com.rexredis.store;

import java.util.*;

/**
 * Wraps a stored value with its type tag.
 * Supports STRING, LIST, SET, and HASH types.
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

    public static RedisValue list() {
        return new RedisValue(Type.LIST, new LinkedList<String>());
    }

    public static RedisValue list(List<String> initialValues) {
        return new RedisValue(Type.LIST, new LinkedList<>(initialValues));
    }

    public static RedisValue set() {
        return new RedisValue(Type.SET, new LinkedHashSet<String>());
    }

    public static RedisValue set(Set<String> initialValues) {
        return new RedisValue(Type.SET, new LinkedHashSet<>(initialValues));
    }

    public static RedisValue hash() {
        return new RedisValue(Type.HASH, new LinkedHashMap<String, String>());
    }

    public static RedisValue hash(Map<String, String> initialValues) {
        return new RedisValue(Type.HASH, new LinkedHashMap<>(initialValues));
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

    public String asString() {
        return (String) value;
    }

    @SuppressWarnings("unchecked")
    public LinkedList<String> asList() {
        return (LinkedList<String>) value;
    }

    @SuppressWarnings("unchecked")
    public Set<String> asSet() {
        return (Set<String>) value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> asHash() {
        return (Map<String, String>) value;
    }
}
