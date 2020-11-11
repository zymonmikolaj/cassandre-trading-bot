package tech.cassandre.trading.bot.web.api.v1;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Position websocket API handled.
 */
public class PositionWebSocketHandler implements WebSocketHandler {

    @Override
    public final Mono<Void> handle(final WebSocketSession session) {
        return session.send(s -> Flux.interval(Duration.ofSeconds(1))
                .map(Object::toString)
                .map(session::textMessage));
    }

}
