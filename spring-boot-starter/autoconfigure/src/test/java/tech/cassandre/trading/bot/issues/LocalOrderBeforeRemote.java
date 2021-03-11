package tech.cassandre.trading.bot.issues;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tech.cassandre.trading.bot.batch.OrderFlux;
import tech.cassandre.trading.bot.batch.TickerFlux;
import tech.cassandre.trading.bot.batch.TradeFlux;
import tech.cassandre.trading.bot.domain.Order;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyAmountDTO;
import tech.cassandre.trading.bot.repository.OrderRepository;
import tech.cassandre.trading.bot.repository.PositionRepository;
import tech.cassandre.trading.bot.repository.TradeRepository;
import tech.cassandre.trading.bot.test.util.junit.BaseTest;
import tech.cassandre.trading.bot.test.util.junit.configuration.Configuration;
import tech.cassandre.trading.bot.test.util.junit.configuration.Property;
import tech.cassandre.trading.bot.test.util.strategies.TestableCassandreStrategy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static tech.cassandre.trading.bot.dto.trade.OrderStatusDTO.NEW;
import static tech.cassandre.trading.bot.dto.trade.OrderTypeDTO.ASK;
import static tech.cassandre.trading.bot.test.util.junit.configuration.ConfigurationExtension.PARAMETER_EXCHANGE_DRY;

/**
 * Save local order before saving distant order.
 * Issue : https://github.com/cassandre-tech/cassandre-trading-bot/issues/427
 */
@SpringBootTest
@ActiveProfiles("schedule-disabled")
@DisplayName("Github issues - Save local order before saving distant order - 427")
@Configuration({
        @Property(key = PARAMETER_EXCHANGE_DRY, value = "true")
})
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@Import(LocalOrderBeforeRemoteMock.class)
public class LocalOrderBeforeRemote extends BaseTest {

    @Autowired
    private TestableCassandreStrategy strategy;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TickerFlux tickerFlux;

    @Autowired
    private OrderFlux orderFlux;

    @Autowired
    private TradeFlux tradeFlux;

    @Test
    @DisplayName("Check local order before remote one")
    public void saveLocalOrderBeforeRemote() throws InterruptedException {
        // Check that a distant order is not saved before the local order is created.

        // Call getOrders to retrieve the distant order.
        orderFlux.update();

        // We wait a bit, the order should not be here as the local order is not saved.
        TimeUnit.SECONDS.sleep(WAITING_TIME_IN_SECONDS);
        assertEquals(0, orderRepository.count());

        // The local order is saved
        orderFlux.emitValue(OrderDTO.builder()
                .orderId("ORDER_000001")
                .type(ASK)
                .currencyPair(cp1)
                .amount(new CurrencyAmountDTO("1", cp1.getBaseCurrency()))
                .averagePrice(new CurrencyAmountDTO("3", cp1.getQuoteCurrency()))
                .limitPrice(new CurrencyAmountDTO("5", cp1.getQuoteCurrency()))
                .leverage("leverage2")
                .status(NEW)
                .cumulativeAmount(new CurrencyAmountDTO("2", cp1.getBaseCurrency()))
                .userReference("MY_REF_1")
                .timestamp(ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))
                .build());

        // We wait a bit, the local order should be here.
        TimeUnit.SECONDS.sleep(WAITING_TIME_IN_SECONDS);
        assertEquals(1, orderRepository.count());
        Optional<Order> o = orderRepository.findByOrderId("ORDER_000001");
        assertTrue(o.isPresent());
        assertEquals("leverage2", o.get().getLeverage());

        // Call getOrders to retrieve the distant order.
        orderFlux.update();

        // We wait a bit, the order in database should be updated with the distant one.
        TimeUnit.SECONDS.sleep(WAITING_TIME_IN_SECONDS);
        assertEquals(1, orderRepository.count());
        o = orderRepository.findByOrderId("ORDER_000001");
        assertTrue(o.isPresent());
        assertEquals("leverage1", o.get().getLeverage());
    }

}
