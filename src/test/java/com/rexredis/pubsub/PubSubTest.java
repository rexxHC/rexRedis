package com.rexredis.pubsub;

import com.rexredis.command.Command;
import com.rexredis.command.CommandRouter;
import com.rexredis.protocol.RespObject;
import com.rexredis.server.ClientHandler;
import com.rexredis.store.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PubSubTest {

    private PubSubManager pubSubManager;
    private CommandRouter router;
    private DataStore store;
    private ClientHandler mockClient1;
    private ClientHandler mockClient2;

    @BeforeEach
    void setUp() {
        store = new DataStore();
        router = new CommandRouter(store);
        pubSubManager = router.getPubSubManager();
        mockClient1 = mock(ClientHandler.class);
        mockClient2 = mock(ClientHandler.class);
    }

    @Test
    void testSubscribeAndPublish() {
        int count1 = pubSubManager.subscribe("news", mockClient1);
        assertThat(count1).isEqualTo(1);

        int count2 = pubSubManager.subscribe("news", mockClient2);
        assertThat(count2).isEqualTo(1);

        // Publish to "news"
        int receivers = pubSubManager.publish("news", "Breaking News!");
        assertThat(receivers).isEqualTo(2);

        // Both clients should receive the message array
        ArgumentCaptor<RespObject> captor = ArgumentCaptor.forClass(RespObject.class);
        verify(mockClient1).sendResponse(captor.capture());
        verify(mockClient2).sendResponse(any(RespObject.class));

        RespObject msg = captor.getValue();
        assertThat(msg).isInstanceOf(RespObject.ArrayResp.class);
        List<RespObject> elements = ((RespObject.ArrayResp) msg).elements();
        assertThat(elements).hasSize(3);
        assertThat(((RespObject.BulkString) elements.get(0)).value()).isEqualTo("message");
        assertThat(((RespObject.BulkString) elements.get(1)).value()).isEqualTo("news");
        assertThat(((RespObject.BulkString) elements.get(2)).value()).isEqualTo("Breaking News!");
    }

    @Test
    void testPublishNoSubscribersReturnsZero() {
        int receivers = pubSubManager.publish("empty-channel", "hello");
        assertThat(receivers).isEqualTo(0);
    }

    @Test
    void testUnsubscribe() {
        pubSubManager.subscribe("sports", mockClient1);
        pubSubManager.subscribe("weather", mockClient1);
        assertThat(pubSubManager.getSubscriptionCount(mockClient1)).isEqualTo(2);

        int remaining = pubSubManager.unsubscribe("sports", mockClient1);
        assertThat(remaining).isEqualTo(1);
        assertThat(pubSubManager.isSubscribed(mockClient1)).isTrue();

        remaining = pubSubManager.unsubscribe("weather", mockClient1);
        assertThat(remaining).isEqualTo(0);
        assertThat(pubSubManager.isSubscribed(mockClient1)).isFalse();
    }

    @Test
    void testSubscriptionModeCommandRestrictions() {
        pubSubManager.subscribe("alerts", mockClient1);

        // Disallowed command in subscription mode
        RespObject getRes = router.dispatch(new Command("GET", List.of("foo")), mockClient1);
        assertThat(getRes).isInstanceOf(RespObject.Error.class);
        assertThat(((RespObject.Error) getRes).message()).contains("only (P)SUBSCRIBE");

        // Allowed command in subscription mode
        RespObject pingRes = router.dispatch(new Command("PING", List.of()), mockClient1);
        assertThat(pingRes).isInstanceOf(RespObject.SimpleString.class);
    }
}
