package tech.cassandre.trading.bot.test.batch;

import io.qase.api.annotation.CaseId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import tech.cassandre.trading.bot.batch.OrderFlux;
import tech.cassandre.trading.bot.dto.trade.OrderCreationResultDTO;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyAmountDTO;
import tech.cassandre.trading.bot.repository.OrderRepository;
import tech.cassandre.trading.bot.service.TradeService;
import tech.cassandre.trading.bot.test.util.junit.BaseTest;
import tech.cassandre.trading.bot.test.util.junit.configuration.Configuration;
import tech.cassandre.trading.bot.test.util.junit.configuration.Property;
import tech.cassandre.trading.bot.test.util.strategies.TestableCassandreStrategy;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static tech.cassandre.trading.bot.dto.trade.OrderStatusDTO.FILLED;
import static tech.cassandre.trading.bot.dto.trade.OrderStatusDTO.PENDING_NEW;
import static tech.cassandre.trading.bot.dto.trade.OrderTypeDTO.ASK;
import static tech.cassandre.trading.bot.dto.trade.OrderTypeDTO.BID;
import static tech.cassandre.trading.bot.test.util.junit.configuration.ConfigurationExtension.PARAMETER_EXCHANGE_DRY;

@SpringBootTest
@DisplayName("Batch - Order flux")
@Configuration({
        @Property(key = PARAMETER_EXCHANGE_DRY, value = "false")
})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@Import(OrderFluxTestMock.class)
public class OrderFluxTest extends BaseTest {

    @Autowired
    private TestableCassandreStrategy strategy;

    @Autowired
    private org.knowm.xchange.service.trade.TradeService xChangeTradeService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderFlux orderFlux;

    @Test
    @CaseId(3)
    @DisplayName("Check received data")
    public void checkReceivedData() {
        // =============================================================================================================
        // Test asynchronous flux.
        final int numberOfUpdatesExpected = 8;
        final int numberOfServiceCallsExpected = 4;

        assertEquals(0, orderRepository.count());

        // ORDER_000001.
        final OrderCreationResultDTO order000001 = tradeService.createSellMarketOrder(strategyDTO, cp1, new BigDecimal("1"));
        assertTrue(order000001.isSuccessful());
        assertEquals("ORDER_000001", order000001.getOrderId());

        // ORDER_000002.
        final OrderCreationResultDTO order000002 = tradeService.createBuyMarketOrder(strategyDTO, cp2, new BigDecimal("2"));
        assertTrue(order000002.isSuccessful());
        assertEquals("ORDER_000002", order000002.getOrderId());

        // ORDER_000003.
        final OrderCreationResultDTO order000003 = tradeService.createSellMarketOrder(strategyDTO, cp1, new BigDecimal("3"));
        assertTrue(order000003.isSuccessful());
        assertEquals("ORDER_000003", order000003.getOrderId());

        // We wait for the orders to be created in database.
        await().until(() -> orderRepository.count() == 3);
        assertTrue(strategy.getOrderByOrderId("ORDER_000001").isPresent());
        assertTrue(strategy.getOrderByOrderId("ORDER_000002").isPresent());
        assertTrue(strategy.getOrderByOrderId("ORDER_000003").isPresent());
        assertFalse(strategy.getOrderByOrderId("ORDER_000004").isPresent());

        // Waiting for the service to have been called with all the test data.
        await().untilAsserted(() -> verify(xChangeTradeService, atLeast(numberOfServiceCallsExpected)).getOpenOrders());

        // Checking that somme data have already been treated.
        // but not all as the flux should be asynchronous and single thread and strategy method method waits 1 second.
        assertTrue(strategy.getOrdersUpdateReceived().size() > 0);
        assertTrue(strategy.getOrdersUpdateReceived().size() <= numberOfUpdatesExpected);

        // Wait for the strategy to have received all the test values.
        await().untilAsserted(() -> assertTrue(strategy.getOrdersUpdateReceived().size() >= numberOfUpdatesExpected));
        final Iterator<OrderDTO> orders = strategy.getOrdersUpdateReceived().iterator();

        // =============================================================================================================
        // Test all values received by the strategy with update methods.

        // First call : 3 orders retrieved from local.
        // - Order ORDER_000001.
        // - Order ORDER_000002.
        // - Order ORDER_000003.

        // Check update 1 - Result of ORDER_000001 creation.
        OrderDTO o = orders.next();
        assertEquals(1, o.getId());
        assertEquals("ORDER_000001", o.getOrderId());
        assertEquals(ASK, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp1, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("1", cp1.getBaseCurrency()), o.getAmount());
        assertNull(o.getAveragePrice());
        assertNull(o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(PENDING_NEW, o.getStatus());
        assertNull(o.getCumulativeAmount());
        assertNull(o.getUserReference());
        assertNotNull(o.getTimestamp());

        // Check update 2 - Result of ORDER_000002 creation.
        o = orders.next();
        assertEquals(2, o.getId());
        assertEquals("ORDER_000002", o.getOrderId());
        assertEquals(BID, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp2, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("2", cp2.getBaseCurrency()), o.getAmount());
        assertNull(o.getAveragePrice());
        assertNull(o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(PENDING_NEW, o.getStatus());
        assertNull(o.getCumulativeAmount());
        assertNull(o.getUserReference());
        assertNotNull(o.getTimestamp());

        // Check update 3 - Result of ORDER_000003 creation.
        o = orders.next();
        assertEquals(3, o.getId());
        assertEquals("ORDER_000003", o.getOrderId());
        assertEquals(ASK, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp1, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("3", cp1.getBaseCurrency()), o.getAmount());
        assertNull(o.getAveragePrice());
        assertNull(o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(PENDING_NEW, o.getStatus());
        assertNull(o.getCumulativeAmount());
        assertNull(o.getUserReference());
        assertNotNull(o.getTimestamp());

        // Second call : 3 orders retrieved from exchange.
        // - Order ORDER_000001 (amount changed).
        // - Order ORDER_000002 (amount changed).
        // - Order ORDER_000003 (amount changed).

        // Check update 4 - Received ORDER_000001 from server for the first time.
        o = orders.next();
        assertEquals(1, o.getId());
        assertEquals("ORDER_000001", o.getOrderId());
        assertEquals(ASK, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp1, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("11", cp1.getBaseCurrency()), o.getAmount());
        assertEquals(new CurrencyAmountDTO("1", cp1.getQuoteCurrency()), o.getAveragePrice());
        assertEquals(new CurrencyAmountDTO("0", cp1.getQuoteCurrency()), o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(FILLED, o.getStatus());
        assertEquals(new CurrencyAmountDTO("111", cp1.getBaseCurrency()), o.getCumulativeAmount());
        assertNotNull(o.getTimestamp());

        // Check update 5 - Received ORDER_000002 from server for the first time.
        o = orders.next();
        assertEquals(2, o.getId());
        assertEquals("ORDER_000002", o.getOrderId());
        assertEquals(BID, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp2, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("22", cp2.getBaseCurrency()), o.getAmount());
        assertEquals(new CurrencyAmountDTO("1", cp2.getQuoteCurrency()), o.getAveragePrice());
        assertEquals(new CurrencyAmountDTO("0", cp2.getQuoteCurrency()), o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(FILLED, o.getStatus());
        assertEquals(new CurrencyAmountDTO("222", cp1.getBaseCurrency()), o.getCumulativeAmount());
        assertNotNull(o.getTimestamp());

        // Check update 6 - Received ORDER_000003 from server for the first time.
        o = orders.next();
        assertEquals(3, o.getId());
        assertEquals("ORDER_000003", o.getOrderId());
        assertEquals(ASK, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp1, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("33", cp1.getBaseCurrency()), o.getAmount());
        assertEquals(new CurrencyAmountDTO("1", cp1.getQuoteCurrency()), o.getAveragePrice());
        assertEquals(new CurrencyAmountDTO("0", cp1.getQuoteCurrency()), o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(FILLED, o.getStatus());
        assertEquals(new CurrencyAmountDTO("333", cp1.getBaseCurrency()), o.getCumulativeAmount());
        assertNotNull(o.getTimestamp());

        // Third call : 4 orders.
        // - Order ORDER_000001 : no changes.
        // - Order ORDER_000002 : no changes.
        // - Order ORDER_000003 : the original amount changed.
        // - Order ORDER_000004 : new order (but not yet created in database).

        // Check update 7 - Received ORDER_000003 from server because the original amount changed.
        o = orders.next();
        assertEquals(3, o.getId());
        assertEquals("ORDER_000003", o.getOrderId());
        assertEquals(ASK, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp1, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("3333", cp1.getBaseCurrency()), o.getAmount());
        assertEquals(new CurrencyAmountDTO("1", cp1.getQuoteCurrency()), o.getAveragePrice());
        assertEquals(new CurrencyAmountDTO("0", cp1.getQuoteCurrency()), o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(FILLED, o.getStatus());
        assertEquals(new CurrencyAmountDTO("33333", cp1.getBaseCurrency()), o.getCumulativeAmount());
        assertNotNull(o.getTimestamp());

        // Fourth call : 4 orders
        // - Order ORDER_000001 : no changes.
        // - Order ORDER_000002 : average price changed.
        // - Order ORDER_000003 : no changes
        // - Order ORDER_000004 : average price changed.

        // Check update 8 - Received ORDER_000002 from server because the average price changed.
        o = orders.next();
        assertEquals(2, o.getId());
        assertEquals("ORDER_000002", o.getOrderId());
        assertEquals(BID, o.getType());
        assertEquals(1, o.getStrategy().getId());
        assertEquals("01", o.getStrategy().getStrategyId());
        assertEquals(cp2, o.getCurrencyPair());
        assertEquals(new CurrencyAmountDTO("22", cp2.getBaseCurrency()), o.getAmount());
        assertEquals(new CurrencyAmountDTO("2", cp2.getQuoteCurrency()), o.getAveragePrice());
        assertEquals(new CurrencyAmountDTO("0", cp2.getQuoteCurrency()), o.getLimitPrice());
        assertNull(o.getLeverage());
        assertEquals(FILLED, o.getStatus());
        assertEquals(new CurrencyAmountDTO("222", cp1.getBaseCurrency()), o.getCumulativeAmount());
        assertNotNull(o.getTimestamp());

        // =============================================================================================================
        // Check data we have in strategy & database.
        assertEquals(3, orderRepository.count());
        final Map<String, OrderDTO> strategyOrders = strategy.getOrders();
        assertEquals(3, strategyOrders.size());
        assertNotNull(strategyOrders.get("ORDER_000001"));
        assertNotNull(strategyOrders.get("ORDER_000002"));
        assertNotNull(strategyOrders.get("ORDER_000003"));
        assertNull(strategyOrders.get("ORDER_000004"));

        // Order ORDER_000001.
        final Optional<OrderDTO> o1 = strategy.getOrderByOrderId("ORDER_000001");
        assertTrue(o1.isPresent());
        assertEquals(1, o1.get().getId());
        assertEquals("ORDER_000001", o1.get().getOrderId());
        assertEquals(ASK, o1.get().getType());
        assertEquals(1, o1.get().getStrategy().getId());
        assertEquals("01", o1.get().getStrategy().getStrategyId());
        assertEquals(cp1, o1.get().getCurrencyPair());
        assertEquals(0, new BigDecimal("11").compareTo(o1.get().getAmount().getValue()));
        assertEquals(cp1.getBaseCurrency(), o1.get().getAmount().getCurrency());
        assertEquals(0, new BigDecimal("1").compareTo(o1.get().getAveragePrice().getValue()));
        assertEquals(cp1.getQuoteCurrency(), o1.get().getAveragePrice().getCurrency());
        assertEquals(0, new BigDecimal("0").compareTo(o1.get().getLimitPrice().getValue()));
        assertEquals(cp1.getQuoteCurrency(), o1.get().getLimitPrice().getCurrency());
        assertNull(o1.get().getLeverage());
        assertEquals(FILLED, o1.get().getStatus());
        assertEquals(0, new BigDecimal("111").compareTo(o1.get().getCumulativeAmount().getValue()));
        assertEquals(cp1.getBaseCurrency(), o1.get().getCumulativeAmount().getCurrency());
        assertEquals("My reference", o1.get().getUserReference());
        assertNotNull(o1.get().getTimestamp());

        // Order ORDER_000002.
        final Optional<OrderDTO> o2 = strategy.getOrderByOrderId("ORDER_000002");
        assertTrue(o2.isPresent());
        assertEquals(2, o2.get().getId());
        assertEquals("ORDER_000002", o2.get().getOrderId());
        assertEquals(BID, o2.get().getType());
        assertEquals(1, o2.get().getStrategy().getId());
        assertEquals("01", o2.get().getStrategy().getStrategyId());
        assertEquals(cp2, o2.get().getCurrencyPair());
        assertEquals(0, new BigDecimal("22").compareTo(o2.get().getAmount().getValue()));
        assertEquals(cp2.getBaseCurrency(), o2.get().getAmount().getCurrency());
        assertEquals(0, new BigDecimal("2").compareTo(o2.get().getAveragePrice().getValue()));
        assertEquals(cp2.getQuoteCurrency(), o2.get().getAveragePrice().getCurrency());
        assertEquals(0, new BigDecimal("0").compareTo(o2.get().getLimitPrice().getValue()));
        assertEquals(cp2.getQuoteCurrency(), o2.get().getLimitPrice().getCurrency());
        assertNull(o1.get().getLeverage());
        assertEquals(FILLED, o2.get().getStatus());
        assertEquals(0, new BigDecimal("222").compareTo(o2.get().getCumulativeAmount().getValue()));
        assertEquals(cp2.getBaseCurrency(), o2.get().getCumulativeAmount().getCurrency());
        assertEquals("My reference", o2.get().getUserReference());
        assertNotNull(o2.get().getTimestamp());

        // Order ORDER_000003.
        final Optional<OrderDTO> o3 = strategy.getOrderByOrderId("ORDER_000003");
        assertTrue(o3.isPresent());
        assertEquals(3, o3.get().getId());
        assertEquals("ORDER_000003", o3.get().getOrderId());
        assertEquals(ASK, o3.get().getType());
        assertEquals(1, o3.get().getStrategy().getId());
        assertEquals("01", o3.get().getStrategy().getStrategyId());
        assertEquals(cp1, o3.get().getCurrencyPair());
        assertEquals(0, new BigDecimal("3333").compareTo(o3.get().getAmount().getValue()));
        assertEquals(cp1.getBaseCurrency(), o3.get().getAmount().getCurrency());
        assertEquals(0, new BigDecimal("1").compareTo(o3.get().getAveragePrice().getValue()));
        assertEquals(cp1.getQuoteCurrency(), o3.get().getAveragePrice().getCurrency());
        assertEquals(0, new BigDecimal("0").compareTo(o3.get().getLimitPrice().getValue()));
        assertEquals(cp1.getQuoteCurrency(), o3.get().getLimitPrice().getCurrency());
        assertNull(o1.get().getLeverage());
        assertEquals(FILLED, o3.get().getStatus());
        assertEquals(0, new BigDecimal("33333").compareTo(o3.get().getCumulativeAmount().getValue()));
        assertEquals(cp1.getBaseCurrency(), o3.get().getCumulativeAmount().getCurrency());
        assertEquals("My reference", o3.get().getUserReference());
        assertNotNull(o3.get().getTimestamp());
    }

}
