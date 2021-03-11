package tech.cassandre.trading.bot.issues;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import tech.cassandre.trading.bot.batch.OrderFlux;
import tech.cassandre.trading.bot.batch.TickerFlux;
import tech.cassandre.trading.bot.dto.position.PositionCreationResultDTO;
import tech.cassandre.trading.bot.dto.position.PositionRulesDTO;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyAmountDTO;
import tech.cassandre.trading.bot.test.service.dry.PositionServiceDryModeTestMock;
import tech.cassandre.trading.bot.repository.OrderRepository;
import tech.cassandre.trading.bot.test.util.junit.BaseTest;
import tech.cassandre.trading.bot.test.util.junit.configuration.Configuration;
import tech.cassandre.trading.bot.test.util.junit.configuration.Property;
import tech.cassandre.trading.bot.test.util.strategies.TestableCassandreStrategy;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static tech.cassandre.trading.bot.dto.trade.OrderStatusDTO.NEW;
import static tech.cassandre.trading.bot.dto.trade.OrderTypeDTO.BID;
import static tech.cassandre.trading.bot.test.util.junit.configuration.ConfigurationExtension.PARAMETER_EXCHANGE_DRY;

/**
 * Duplicated orders.
 * Issue : https://github.com/cassandre-tech/cassandre-trading-bot/issues/421
 */
@SpringBootTest
@DisplayName("Github issues - Duplicated order - 421")
@Configuration({
        @Property(key = PARAMETER_EXCHANGE_DRY, value = "true")
})
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@Import(PositionServiceDryModeTestMock.class)
public class DuplicatedOrders extends BaseTest {

    @Autowired
    private TestableCassandreStrategy strategy;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TickerFlux tickerFlux;

    @Autowired
    private OrderFlux orderFlux;

    @Test
    @DisplayName("Check duplicated order")
    public void checkDuplicatedOrder() {
        // First tickers - cp1 & cp2 (dry mode).
        // ETH, BTC - bid 0.2 / ask 0.2.
        // ETH, USDT - bid 0,3 / ask 0.3.
        tickerFlux.update();
        tickerFlux.update();

        // =============================================================================================================
        // The orders created arrives before the order is created locally by the position.
        orderFlux.emitValue(OrderDTO.builder()
                .orderId("DRY_ORDER_000000001")
                .type(BID)
                .strategy(strategy.getStrategyDTO())
                .currencyPair(cp1)
                .amount(new CurrencyAmountDTO(new BigDecimal("0.0001"), cp1.getBaseCurrency()))
                .status(NEW)
                .build());

        orderFlux.emitValue(OrderDTO.builder()
                .orderId("DRY_ORDER_000000001")
                .type(BID)
                .strategy(strategy.getStrategyDTO())
                .currencyPair(cp1)
                .amount(new CurrencyAmountDTO(new BigDecimal("0.0001"), cp1.getBaseCurrency()))
                .status(NEW)
                .build());

        // =============================================================================================================
        // Creates position 1 (ETH/BTC, 0.0001, 100% stop gain, price of 0.2).
        // As the order is validated and the trade arrives, the position should be opened.
        final PositionCreationResultDTO position1Result = strategy.createLongPosition(cp1,
                new BigDecimal("0.0001"),
                PositionRulesDTO.builder().stopGainPercentage(100f).build());
        assertTrue(position1Result.isSuccessful());

        assertEquals(1, orderRepository.count());
    }

}
