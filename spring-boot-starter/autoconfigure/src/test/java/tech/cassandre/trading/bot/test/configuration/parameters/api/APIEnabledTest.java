package tech.cassandre.trading.bot.test.configuration.parameters.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import tech.cassandre.trading.bot.CassandreTradingBot;
import tech.cassandre.trading.bot.test.util.junit.BaseTest;
import tech.cassandre.trading.bot.test.util.junit.configuration.Configuration;
import tech.cassandre.trading.bot.test.util.junit.configuration.Property;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static tech.cassandre.trading.bot.util.parameters.APIParameters.PARAMETER_API_ENABLED;

@DisplayName("API parameters - No api")
@Configuration({
        @Property(key = PARAMETER_API_ENABLED, value = "true")
})
@DirtiesContext(classMode = AFTER_CLASS)
@Disabled
public class APIEnabledTest extends BaseTest {

    @Test
    @DisplayName("Check if API is disabled")
    public void apiDisabled() {
        try {
            SpringApplication application = new SpringApplication(CassandreTradingBot.class);
            application.run();
            WebSocketClient client = new ReactorNettyWebSocketClient();

            URI url = new URI("ws://localhost:8080/api/v1/streams/positions");
            client.execute(url, session ->
                    session.receive()
                            .doOnNext(webSocketMessage -> System.out.println("=!!"))
                            .doOnError(throwable -> System.out.println("==> " + throwable.getMessage()))
                            .doOnSubscribe(subscriber -> System.out.println(session.getId() + ".OPEN"))
                            .doAfterTerminate(() -> System.out.println("FINI"))
                            .then());
            Thread.sleep(10000);
            System.err.println("TOTOTOTOO");

        } catch (Exception e) {
            fail("Exception raised " + e.getMessage());
        }
    }

}
