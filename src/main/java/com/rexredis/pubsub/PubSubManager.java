package com.rexredis.pubsub;

import com.rexredis.server.ClientHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Pub/Sub channel subscriptions and message fan-out.
 */
public class PubSubManager {

    /** channel name → set of subscribed client handlers */
    private final Map<String, Set<ClientHandler>> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String channel, ClientHandler client) {
        subscriptions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(client);
    }

    public void unsubscribe(String channel, ClientHandler client) {
        Set<ClientHandler> clients = subscriptions.get(channel);
        if (clients != null) {
            clients.remove(client);
            if (clients.isEmpty()) {
                subscriptions.remove(channel);
            }
        }
    }

    /**
     * Publishes a message to all subscribers of the given channel.
     *
     * @return the number of clients that received the message
     */
    public int publish(String channel, String message) {
        Set<ClientHandler> clients = subscriptions.get(channel);
        if (clients == null || clients.isEmpty()) {
            return 0;
        }
        // TODO: Send RESP message array to each subscribed client
        return clients.size();
    }
}
