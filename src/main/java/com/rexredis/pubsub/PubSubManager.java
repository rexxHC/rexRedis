package com.rexredis.pubsub;

import com.rexredis.protocol.RespObject;
import com.rexredis.server.ClientHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Pub/Sub channel subscriptions and message fan-out.
 */
public class PubSubManager {

    /** channel name → set of subscribed client handlers */
    private final Map<String, Set<ClientHandler>> channelSubscribers = new ConcurrentHashMap<>();

    /** client handler → set of channel names it is subscribed to */
    private final Map<ClientHandler, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();

    /**
     * Subscribes a client to a channel.
     *
     * @param channel the channel name
     * @param client  the client handler
     * @return the total number of channels this client is now subscribed to
     */
    public int subscribe(String channel, ClientHandler client) {
        channelSubscribers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(client);
        Set<String> channels = clientSubscriptions.computeIfAbsent(client, k -> ConcurrentHashMap.newKeySet());
        channels.add(channel);
        return channels.size();
    }

    /**
     * Unsubscribes a client from a channel.
     *
     * @param channel the channel name
     * @param client  the client handler
     * @return the remaining number of channels this client is subscribed to
     */
    public int unsubscribe(String channel, ClientHandler client) {
        Set<ClientHandler> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(client);
            if (subscribers.isEmpty()) {
                channelSubscribers.remove(channel, Collections.emptySet());
            }
        }

        Set<String> channels = clientSubscriptions.get(client);
        if (channels != null) {
            channels.remove(channel);
            int remaining = channels.size();
            if (remaining == 0) {
                clientSubscriptions.remove(client);
            }
            return remaining;
        }

        return 0;
    }

    /**
     * Unsubscribes a client from all channels it is subscribed to.
     *
     * @param client the client handler
     * @return map of channel name to remaining channels count at the moment of unsubscribing
     */
    public List<Map.Entry<String, Integer>> unsubscribeAll(ClientHandler client) {
        Set<String> channels = clientSubscriptions.get(client);
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }

        List<String> toUnsubscribe = new ArrayList<>(channels);
        List<Map.Entry<String, Integer>> result = new ArrayList<>();

        for (String channel : toUnsubscribe) {
            int remaining = unsubscribe(channel, client);
            result.add(Map.entry(channel, remaining));
        }

        return result;
    }

    /**
     * Publishes a message to all subscribers of the given channel.
     *
     * @param channel the channel to publish to
     * @param message the message payload
     * @return the number of subscribers that received the message
     */
    public int publish(String channel, String message) {
        Set<ClientHandler> subscribers = channelSubscribers.get(channel);
        if (subscribers == null || subscribers.isEmpty()) {
            return 0;
        }

        RespObject msgObj = RespObject.array(List.of(
                RespObject.bulkString("message"),
                RespObject.bulkString(channel),
                RespObject.bulkString(message)
        ));

        int count = 0;
        for (ClientHandler subscriber : subscribers) {
            subscriber.sendResponse(msgObj);
            count++;
        }

        return count;
    }

    public boolean isSubscribed(ClientHandler client) {
        Set<String> channels = clientSubscriptions.get(client);
        return channels != null && !channels.isEmpty();
    }

    public int getSubscriptionCount(ClientHandler client) {
        Set<String> channels = clientSubscriptions.get(client);
        return channels != null ? channels.size() : 0;
    }

    public Set<String> getSubscribedChannels(ClientHandler client) {
        Set<String> channels = clientSubscriptions.get(client);
        return channels != null ? new HashSet<>(channels) : Collections.emptySet();
    }
}
