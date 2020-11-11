package tech.cassandre.trading.bot.test.web.api.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import tech.cassandre.trading.bot.service.PositionService;
import tech.cassandre.trading.bot.test.util.junit.configuration.Configuration;
import tech.cassandre.trading.bot.test.util.junit.configuration.Property;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayName("API - Positions")
@Configuration({
        @Property(key = "spring.datasource.data", value = "classpath:/backup.sql"),
        @Property(key = "spring.jpa.hibernate.ddl-auto", value = "create-drop")
})
@DirtiesContext(classMode = AFTER_CLASS)
public class PositionAPITest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private PositionService positionService;

    @Test
    @DisplayName("getAll - /v1/positions")
    public void getAll() {
        client.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/positions").build())
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(5)
                .jsonPath("$.[0].id").isEqualTo(1)
                .jsonPath("$.[1].id").isEqualTo(2)
                .jsonPath("$.[2].id").isEqualTo(3)
                .jsonPath("$.[3].id").isEqualTo(4)
                .jsonPath("$.[4].id").isEqualTo(5);
    }

    @Test
    @DisplayName("getById - /v1/positions/id")
    public void getById() {
        // Test for a non existing position.
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/positions/{id}")
                        .build(6))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        // Test of data retrieved for position 5.
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/positions/{id}")
                        .build(5))
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                //.jsonPath("$.[0]").isEqualTo("toto")
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.status").isEqualTo("CLOSED")
                .jsonPath("$.amount").isEqualTo(50)
                // Currency pair.
                .jsonPath("$.currencyPair").isNotEmpty()
                .jsonPath("$.currencyPair.length()").isEqualTo(2)
                .jsonPath("$.currencyPair.baseCurrency.code").isEqualTo("ETH")
                .jsonPath("$.currencyPair.quoteCurrency.code").isEqualTo("USD")
                // Rules.
                .jsonPath("$.rules.stopGainPercentageSet").isEqualTo(true)
                .jsonPath("$.rules.stopGainPercentage").isEqualTo(30)
                .jsonPath("$.rules.stopLossPercentageSet").isEqualTo(true)
                .jsonPath("$.rules.stopLossPercentage").isEqualTo(40)
                // Orders & trades.
                .jsonPath("$.openOrderId").isEqualTo("OPEN_ORDER_01")
                .jsonPath("$.closeOrderId").isEqualTo("CLOSE_ORDER_01")
                .jsonPath("$.openTrades").isArray()
                .jsonPath("$.openTrades.length()").isEqualTo(2)
                .jsonPath("$.openTrades.[0].id").isEqualTo("TRADE_01")
                .jsonPath("$.openTrades.[1].id").isEqualTo("TRADE_02")
                .jsonPath("$.closeTrades").isArray()
                .jsonPath("$.closeTrades.length()").isEqualTo(3)
                .jsonPath("$.closeTrades.[0].id").isEqualTo("TRADE_03")
                .jsonPath("$.closeTrades.[1].id").isEqualTo("TRADE_04")
                .jsonPath("$.closeTrades.[2].id").isEqualTo("TRADE_05")
                // Gains.
                .jsonPath("$.lowestCalculatedGain.percentage").isEqualTo(54.540000915527344)
                .jsonPath("$.lowestCalculatedGain.netAmount.value").isEqualTo(60.0000000000000000)
                .jsonPath("$.lowestCalculatedGain.netAmount.currency.code").isEqualTo("USD")
                .jsonPath("$.highestCalculatedGain.percentage").isEqualTo(518.1799926757812)
                .jsonPath("$.highestCalculatedGain.netAmount.value").isEqualTo(570.0000000000000000)
                .jsonPath("$.highestCalculatedGain.netAmount.currency.code").isEqualTo("USD");
    }

}
