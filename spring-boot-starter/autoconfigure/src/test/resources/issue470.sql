-- =====================================================================================================================
-- Insert exchange accounts.
INSERT INTO EXCHANGE_ACCOUNTS (ID, EXCHANGE, ACCOUNT)
VALUES (1, 'kucoin', 'cassandre.crypto.bot@gmail.com');

-- =====================================================================================================================
-- Insert strategies.
INSERT INTO STRATEGIES (ID, STRATEGY_ID, TYPE, NAME, FK_EXCHANGE_ACCOUNT_ID)
VALUES (1, '01', 'BASIC_STRATEGY', 'My strategy', 1);

-- =====================================================================================================================
-- Insert orders.
INSERT INTO ORDERS (ID, ORDER_ID, TYPE, AMOUNT_VALUE, AMOUNT_CURRENCY, CURRENCY_PAIR, USER_REFERENCE, TIMESTAMP, STATUS,
                    CUMULATIVE_AMOUNT_VALUE, CUMULATIVE_AMOUNT_CURRENCY, AVERAGE_PRICE_VALUE, AVERAGE_PRICE_CURRENCY,
                    LEVERAGE, LIMIT_PRICE_VALUE, LIMIT_PRICE_CURRENCY, FK_STRATEGY_ID)
values -- For position 1.
       (1, 'OPEN_ORDER_01', 'BID', 10, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       (2, 'CLOSE_ORDER_01', 'ASK', 10, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       -- For position 2.
       (3, 'OPEN_ORDER_02', 'BID', 20, 'ETH', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'ETH', 1, 'USDT', '', 1,
        'USDT', 1),
       (4, 'CLOSE_ORDER_02', 'ASK', 20, 'ETH', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'ETH', 1, 'USDT', '', 1,
        'USDT', 1),
       -- For position 3.
       (5, 'OPEN_ORDER_03', 'BID', 30, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       (6, 'CLOSE_ORDER_03', 'ASK', 30, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       -- For position 4.
       (7, 'OPEN_ORDER_04', 'BID', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'NEW', 10, 'BTC', 1, 'USDT', '', 1, 'USDT',
        1),
       (8, 'CLOSE_ORDER_04', 'ASK', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'NEW', 10, 'BTC', 1, 'USDT', '', 1, 'USDT',
        1),
       -- For position 5.
       (9, 'OPEN_ORDER_05', 'BID', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       (10, 'CLOSE_ORDER_05', 'ASK', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'NEW', 10, 'BTC', 1, 'USDT', '', 1, 'USDT',
        1),
       -- For position 6.
       (11, 'OPEN_ORDER_06', 'BID', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'FILLED', 10, 'BTC', 1, 'USDT', '', 1,
        'USDT', 1),
       (12, 'CLOSE_ORDER_06', 'ASK', 50, 'BTC', 'ETH/BTC', '', '2020-11-20', 'NEW', 10, 'BTC', 1, 'USDT', '', 1, 'USDT',
        1);

-- =====================================================================================================================
-- Insert positions.
INSERT INTO POSITIONS (ID, POSITION_ID, TYPE, STATUS, CURRENCY_PAIR, AMOUNT_VALUE, AMOUNT_CURRENCY,
                       RULES_STOP_GAIN_PERCENTAGE, RULES_STOP_LOSS_PERCENTAGE, OPENING_ORDER_ID,
                       FK_OPENING_ORDER_ID, CLOSING_ORDER_ID, FK_CLOSING_ORDER_ID, LOWEST_GAIN_PRICE_VALUE,
                       HIGHEST_GAIN_PRICE_VALUE, LATEST_GAIN_PRICE_VALUE, LOWEST_GAIN_PRICE_CURRENCY,
                       HIGHEST_GAIN_PRICE_CURRENCY, LATEST_GAIN_PRICE_CURRENCY, FK_STRATEGY_ID)
VALUES (1, 1, 'LONG', 'CLOSED', 'ETH/BTC', 10, 'BTC', null, null, 'OPEN_ORDER_01', 1, 'CLOSE_ORDER_01', 2, null, null,
        null, null, null, null, 1),
       (2, 2, 'LONG', 'CLOSED', 'ETH/BTC', 20, 'ETH', null, null, 'OPEN_ORDER_02', 3, 'CLOSE_ORDER_02', 4, null, null,
        null, null, null, null, 1),
       (3, 3, 'LONG', 'CLOSED', 'ETH/BTC', 30, 'BTC', null, null, 'OPEN_ORDER_03', 5, 'CLOSE_ORDER_03', 6, null, null,
        null, null, null, null, 1),
       (4, 4, 'LONG', 'OPENING', 'ETH/BTC', 50, 'BTC', null, null, 'OPEN_ORDER_04', 7, 'CLOSE_ORDER_04', 8, null, null,
        null, null, null, null, 1),
       (5, 5, 'LONG', 'OPENED', 'ETH/BTC', 50, 'BTC', null, null, 'OPEN_ORDER_05', 9, null, null, null, null, null,
        null, null, null, 1),
       (6, 6, 'LONG', 'CLOSING', 'ETH/BTC', 50, 'BTC', null, null, 'OPEN_ORDER_06', 11, 'CLOSE_ORDER_06', 12, null,
        null, null, null, null, null, 1);

-- =====================================================================================================================
-- Insert trades.
INSERT INTO TRADES (ID, TRADE_ID, ORDER_ID, FK_ORDER_ID, TYPE, AMOUNT_VALUE, AMOUNT_CURRENCY, CURRENCY_PAIR,
                    PRICE_VALUE, PRICE_CURRENCY, TIMESTAMP, FEE_VALUE, FEE_CURRENCY)
values -- For position 1.
       (1, 'TRADE_11', 'OPEN_ORDER_01', 1, 'BID', 7, 'BTC', 'ETH/BTC', 11, 'USDT', DATE '2020-08-05', 1, 'USD'),
       (2, 'TRADE_12', 'OPEN_ORDER_01', 1, 'BID', 3, 'BTC', 'ETH/BTC', 12, 'USDT', DATE '2020-08-06', 2, 'USD'),
       (3, 'TRADE_13', 'CLOSE_ORDER_01', 2, 'ASK', 1, 'BTC', 'ETH/BTC', 13, 'USDT', DATE '2020-08-07', 3, 'USD'),
       (4, 'TRADE_14', 'CLOSE_ORDER_01', 2, 'ASK', 2, 'BTC', 'ETH/BTC', 14, 'USDT', DATE '2020-08-08', 4, 'USD'),
       (5, 'TRADE_15', 'CLOSE_ORDER_01', 2, 'ASK', 8, 'BTC', 'ETH/BTC', 15, 'USDT', DATE '2020-08-09', 5, 'USD'),
       -- For position 2.
       (6, 'TRADE_21', 'OPEN_ORDER_02', 3, 'BID', 20, 'ETH', 'ETH/BTC', 100, 'USDT', DATE '2020-08-05', 5, 'USD'),
       (7, 'TRADE_22', 'CLOSE_ORDER_02', 4, 'ASK', 20, 'ETH', 'ETH/BTC', 50, 'USDT', DATE '2020-08-06', 5, 'USD'),
       -- For position 3.
       (8, 'TRADE_31', 'OPEN_ORDER_03', 5, 'BID', 30, 'BTC', 'ETH/BTC', 20, 'USDT', DATE '2020-08-05', 6, 'USD'),
       (9, 'TRADE_32', 'CLOSE_ORDER_03', 6, 'ASK', 30, 'BTC', 'ETH/BTC', 25, 'USDT', DATE '2020-08-06', 5, 'USD'),
       -- For position 4.
       (10, 'TRADE_41', 'OPEN_ORDER_04', 7, 'BID', 50, 'BTC', 'ETH/BTC', 20, 'USDT', DATE '2020-08-05', 6, 'USD'),
       (11, 'TRADE_42', 'CLOSE_ORDER_04', 8, 'ASK', 50, 'BTC', 'ETH/BTC', 25, 'USDT', DATE '2020-08-06', 5, 'USD'),
       -- For position 5.
       (12, 'TRADE_51', 'OPEN_ORDER_05', 9, 'BID', 50, 'BTC', 'ETH/BTC', 20, 'USDT', DATE '2020-08-05', 6, 'USD'),
       (13, 'TRADE_52', 'CLOSE_ORDER_05', 10, 'ASK', 50, 'BTC', 'ETH/BTC', 25, 'USDT', DATE '2020-08-06', 5, 'USD'),
       -- For position 6.
       (14, 'TRADE_61', 'OPEN_ORDER_06', 11, 'BID', 50, 'BTC', 'ETH/BTC', 20, 'USDT', DATE '2020-08-05', 6, 'USD'),
       (15, 'TRADE_62', 'CLOSE_ORDER_06', 12, 'ASK', 50, 'BTC', 'ETH/BTC', 25, 'USDT', DATE '2020-08-06', 5, 'USD');
