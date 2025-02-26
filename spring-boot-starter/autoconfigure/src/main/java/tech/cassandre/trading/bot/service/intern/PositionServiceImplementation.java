package tech.cassandre.trading.bot.service.intern;

import tech.cassandre.trading.bot.batch.PositionFlux;
import tech.cassandre.trading.bot.domain.Position;
import tech.cassandre.trading.bot.dto.market.TickerDTO;
import tech.cassandre.trading.bot.dto.position.PositionCreationResultDTO;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.dto.position.PositionRulesDTO;
import tech.cassandre.trading.bot.dto.position.PositionTypeDTO;
import tech.cassandre.trading.bot.dto.strategy.StrategyDTO;
import tech.cassandre.trading.bot.dto.trade.OrderCreationResultDTO;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.trade.TradeDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyAmountDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyPairDTO;
import tech.cassandre.trading.bot.dto.util.GainDTO;
import tech.cassandre.trading.bot.repository.PositionRepository;
import tech.cassandre.trading.bot.service.PositionService;
import tech.cassandre.trading.bot.service.TradeService;
import tech.cassandre.trading.bot.util.base.service.BaseService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static tech.cassandre.trading.bot.dto.position.PositionStatusDTO.CLOSED;
import static tech.cassandre.trading.bot.dto.position.PositionStatusDTO.OPENED;
import static tech.cassandre.trading.bot.dto.position.PositionTypeDTO.LONG;
import static tech.cassandre.trading.bot.dto.position.PositionTypeDTO.SHORT;

/**
 * Position service implementation.
 */
public class PositionServiceImplementation extends BaseService implements PositionService {

    /** Position repository. */
    private final PositionRepository positionRepository;

    /** Trade service. */
    private final TradeService tradeService;

    /** Position flux. */
    private final PositionFlux positionFlux;

    /**
     * Constructor.
     *
     * @param newPositionRepository position repository
     * @param newTradeService       trade service
     * @param newPositionFlux       position flux
     */
    public PositionServiceImplementation(final PositionRepository newPositionRepository,
                                         final TradeService newTradeService,
                                         final PositionFlux newPositionFlux) {
        this.positionRepository = newPositionRepository;
        this.tradeService = newTradeService;
        this.positionFlux = newPositionFlux;
    }

    @Override
    public final PositionCreationResultDTO createLongPosition(final StrategyDTO strategy, final CurrencyPairDTO currencyPair, final BigDecimal amount, final PositionRulesDTO rules) {
        return createPosition(strategy, LONG, currencyPair, amount, rules);
    }

    @Override
    public final PositionCreationResultDTO createShortPosition(final StrategyDTO strategy, final CurrencyPairDTO currencyPair, final BigDecimal amount, final PositionRulesDTO rules) {
        return createPosition(strategy, SHORT, currencyPair, amount, rules);
    }

    /**
     * Creates a position.
     *
     * @param strategy     strategy
     * @param type         long or short
     * @param currencyPair currency pair
     * @param amount       amount
     * @param rules        rules
     * @return position creation result
     */
    public final PositionCreationResultDTO createPosition(final StrategyDTO strategy,
                                                          final PositionTypeDTO type,
                                                          final CurrencyPairDTO currencyPair,
                                                          final BigDecimal amount,
                                                          final PositionRulesDTO rules) {
        // Trying to create an order.
        logger.debug("PositionService - Creating a {} position for {} on {} with the rules : {}", type.toString().toLowerCase(Locale.ROOT), amount, currencyPair, rules);
        // =============================================================================================================
        // Creates the order.
        final OrderCreationResultDTO orderCreationResult;
        if (type == LONG) {
            // Long position - we buy.
            orderCreationResult = tradeService.createBuyMarketOrder(strategy, currencyPair, amount);
        } else {
            // Short position - we sell.
            orderCreationResult = tradeService.createSellMarketOrder(strategy, currencyPair, amount);
        }

        // If it works, creates the position.
        if (orderCreationResult.isSuccessful()) {
            // =========================================================================================================
            // Creates the position in database.
            Position position = new Position();
            position.setStrategy(strategyMapper.mapToStrategy(strategy));
            position = positionRepository.save(position);

            // =========================================================================================================
            // Creates the position dto.
            PositionDTO p = new PositionDTO(position.getId(), type, strategy, currencyPair, amount, orderCreationResult.getOrderId(), rules);
            positionRepository.save(positionMapper.mapToPosition(p));
            logger.debug("PositionService - Position {} opened with order {}", p.getPositionId(), orderCreationResult.getOrder().getOrderId());

            // =========================================================================================================
            // Creates the result.
            positionFlux.emitValue(p);
            return new PositionCreationResultDTO(p);
        } else {
            logger.error("PositionService - Position creation failure : {}", orderCreationResult.getErrorMessage());
            // If it doesn't work, returns the error.
            return new PositionCreationResultDTO(orderCreationResult.getErrorMessage(), orderCreationResult.getException());
        }
    }

    @Override
    public final Set<PositionDTO> getPositions() {
        logger.debug("PositionService - Retrieving all positions");
        return positionRepository.findByOrderById()
                .stream()
                .map(positionMapper::mapToPositionDTO)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public final Optional<PositionDTO> getPositionById(final long id) {
        logger.debug("PositionService - Retrieving position {}", id);
        final Optional<Position> position = positionRepository.findById(id);
        return position.map(positionMapper::mapToPositionDTO);
    }

    @Override
    public final void orderUpdate(final OrderDTO order) {
        logger.debug("PositionService - Updating position with order {}", order);
        positionRepository.findByStatusNot(CLOSED)
                .stream()
                .map(positionMapper::mapToPositionDTO)
                .forEach(p -> {
                    if (p.orderUpdate(order)) {
                        logger.debug("PositionService - Position {} updated with order {}", p.getPositionId(), order);
                        positionFlux.emitValue(p);
                    }
                });
    }

    @Override
    public final void tradeUpdate(final TradeDTO trade) {
        logger.debug("PositionService - Updating position with trade {}", trade);
        positionRepository.findByStatusNot(CLOSED)
                .stream()
                .map(positionMapper::mapToPositionDTO)
                .forEach(p -> {
                    if (p.tradeUpdate(trade)) {
                        logger.debug("PositionService - Position {} updated with trade {}", p.getPositionId(), trade);
                        positionFlux.emitValue(p);
                    }
                });
    }

    @Override
    public final void tickerUpdate(final TickerDTO ticker) {
        // With the ticker received, we check for every position, if it should be closed.
        logger.debug("PositionService - Updating position with ticker {}", ticker);
        positionRepository.findByStatus(OPENED)
                .stream()
                .map(positionMapper::mapToPositionDTO)
                .filter(p -> p.tickerUpdate(ticker))
                .peek(p -> logger.debug("PositionService - Position {} updated with ticker {}", p.getPositionId(), ticker))
                .forEach(p -> {
                    // We close the position if it triggers the rules.
                    if (p.shouldBeClosed()) {
                        final OrderCreationResultDTO orderCreationResult;
                        if (p.getType() == LONG) {
                            // Long.
                            orderCreationResult = tradeService.createSellMarketOrder(p.getStrategy(), ticker.getCurrencyPair(), p.getAmount().getValue());
                        } else {
                            // Short.
                            orderCreationResult = tradeService.createBuyMarketOrder(p.getStrategy(), ticker.getCurrencyPair(), p.getAmount().getValue());
                        }

                        if (orderCreationResult.isSuccessful()) {
                            p.closePositionWithOrderId(orderCreationResult.getOrder().getOrderId());
                            logger.debug("PositionService - Position {} closed with order {}", p.getPositionId(), orderCreationResult.getOrder().getOrderId());
                        }
                    }
                    positionFlux.emitValue(p);
                });
    }

    @Override
    public final HashMap<CurrencyDTO, GainDTO> getGains() {
        HashMap<CurrencyDTO, BigDecimal> totalBefore = new LinkedHashMap<>();
        HashMap<CurrencyDTO, BigDecimal> totalAfter = new LinkedHashMap<>();
        HashMap<CurrencyDTO, BigDecimal> totalFees = new LinkedHashMap<>();
        HashMap<CurrencyDTO, GainDTO> gains = new LinkedHashMap<>();

        // We calculate, by currency, the amount bought & sold.
        positionRepository.findByStatus(CLOSED)
                .stream()
                .map(positionMapper::mapToPositionDTO)
                .forEach(p -> {
                    // We retrieve the currency and initiate the maps if they are empty
                    CurrencyDTO currency;
                    if (p.getType() == LONG) {
                        // LONG.
                        currency = p.getCurrencyPair().getQuoteCurrency();
                    } else {
                        // SHORT.
                        currency = p.getCurrencyPair().getBaseCurrency();
                    }
                    gains.putIfAbsent(currency, null);
                    totalBefore.putIfAbsent(currency, ZERO);
                    totalAfter.putIfAbsent(currency, ZERO);
                    totalFees.putIfAbsent(currency, ZERO);

                    // We calculate the amounts bought and amount sold.
                    if (p.getType() == LONG) {
                        totalBefore.put(currency, p.getOpeningOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmount().getValue().multiply(t.getPrice().getValue()))
                                .reduce(totalBefore.get(currency), BigDecimal::add));
                        totalAfter.put(currency, p.getClosingOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmount().getValue().multiply(t.getPrice().getValue()))
                                .reduce(totalAfter.get(currency), BigDecimal::add));
                    } else {
                        totalBefore.put(currency, p.getOpeningOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmount().getValue())
                                .reduce(totalBefore.get(currency), BigDecimal::add));
                        totalAfter.put(currency, p.getClosingOrder().getTrades()
                                .stream()
                                .map(t -> t.getAmount().getValue())
                                .reduce(totalAfter.get(currency), BigDecimal::add));
                    }

                    // And now the feeds.
                    final BigDecimal fees = Stream.concat(p.getOpeningOrder().getTrades().stream(),
                            p.getClosingOrder().getTrades().stream())
                            .map(t -> t.getFee().getValue())
                            .reduce(totalFees.get(currency), BigDecimal::add);
                    totalFees.put(currency, fees);
                });

        gains.keySet()
                .forEach(currency -> {
                    // We make the calculation.
                    BigDecimal before = totalBefore.get(currency);
                    BigDecimal after = totalAfter.get(currency);
                    BigDecimal fees = totalFees.get(currency);
                    BigDecimal gainAmount = after.subtract(before);
                    BigDecimal gainPercentage = ((after.subtract(before)).divide(before, HALF_UP)).multiply(new BigDecimal("100"));

                    GainDTO g = GainDTO.builder()
                            .percentage(gainPercentage.setScale(2, HALF_UP).doubleValue())
                            .amount(CurrencyAmountDTO.builder()
                                    .value(gainAmount)
                                    .currency(currency)
                                    .build())
                            .fees(CurrencyAmountDTO.builder()
                                    .value(fees)
                                    .currency(currency)
                                    .build())
                            .build();
                    gains.put(currency, g);
                });
        return gains;
    }

}
