package edu.yu.parallel;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.yu.parallel.Impl.BrokerServiceImpl; // doesn't import the other impls
import edu.yu.parallel.Impl.BrokerageImpl;

public class App {
        private static final Logger logger = LogManager.getLogger(App.class);
        private static TradeServer tradeServer;

        private static final int port = 8080;
        private static CountDownLatch latch;

    public static void main(String[] args) throws IOException {
        latch = new CountDownLatch(1); // needed to be moved here so that in testing the server can be reset between tests
        logger.info("Application starting");

        // Read in the number of broker threads to run
        int brokerCount = 1; // min must be 1 broker thread
        if (args.length > 0) { // if no arg passed in, will default set to 1
            brokerCount = Integer.parseInt(args[0]);
        }
        logger.info("Using {} broker(s)", brokerCount);
        
        List<Stock> stocks = new StockReader("stocks.csv").getStockList(15);
        StockPrices stockPrices = StockPrices.getInstance(stocks); // SINGLETON
        BrokerageImpl brokerage = new BrokerageImpl(stocks, stockPrices);
        BrokerServiceImpl brokerService = new BrokerServiceImpl(brokerage);
        tradeServer = new TradeServer(port, brokerCount, brokerService);
        
        logger.info("Loaded {} stocks", stocks.size());

        // start updating the prices of the stocks
        stockPrices.startUpdates();
        logger.info("Started updating of stock prices");

        tradeServer.start();
       
        logger.info("Server started on port {}. Press Ctrl+C to stop...", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tradeServer.stop(0);
            logger.info("Server stopped.");
            latch.countDown();
        }));

        try {
            latch.await(); // This will block the main thread until the latch reaches zero.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Application exiting");
    }

    public static void shutDown() {
        tradeServer.stop(0);
        logger.info("Server stopped.");
        latch.countDown();
    }
}
