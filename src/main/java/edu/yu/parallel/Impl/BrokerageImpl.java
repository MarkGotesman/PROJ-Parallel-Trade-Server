package edu.yu.parallel.Impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.yu.parallel.Brokerage;
import edu.yu.parallel.ClientRecord;
import edu.yu.parallel.Portfolio;
import edu.yu.parallel.Stock;
import edu.yu.parallel.StockPrices;
import edu.yu.parallel.StockRecord;
import edu.yu.parallel.Impl.BrokerageImpl.StockTrade.StockTradeCall;

final public class BrokerageImpl implements Brokerage {
    private StockPrices stockPrices;

    private Map<String, StockRecord> symmbolToStockRecord = new HashMap<>();
    private Map<String, ClientRecord> clientIdToClientRecord = new HashMap<>();

    /**
     * Constructor
     * 
     * @param stocks      list of Stock objects
     * @param stockPrices self-updating object with the current price of each stock
     */
    public BrokerageImpl(List<Stock> stocks, StockPrices stockPrices) {
        this.stockPrices = stockPrices;
        for (Stock stock : stocks) {
            symmbolToStockRecord.put(stock.getSymbol(), new StockRecord(stock.getShares()));
        }
    }

    @Override
    public Portfolio registerClient(String clientId, double initialBalance) {
        boolean clientAlreadyRegistered = clientIdToClientRecord.containsKey(clientId);
        if (clientAlreadyRegistered) throw new IllegalArgumentException("clientAlreadyRegistered: " + clientId);

        // initialBalance = Math.round(initialBalance * 100.0) / 100.0; // round to 2 digits
        // Map<String, Integer> defaultPostions = stockPrices.getStockDefaultPositions();
        Map<String, Integer> defaultPostions = new HashMap<>();
        ClientRecord clientRecord = new ClientRecord(clientId, initialBalance, defaultPostions);
        
        clientIdToClientRecord.put(clientId, clientRecord);
        
        Portfolio portfolio = new PortfolioImpl(clientRecord);
        return portfolio;
    }

    @Override
    public Portfolio buyShares(String clientId, String symbol, int shares) {
        StockTrade stockTrade = new StockTrade(clientId, symbol, shares, StockTradeCall.BUY);
        stockTrade.validate(); // validation does not acquire any locks. the pattern dictates that only when a trade seems valid should the structures be locked to then do a final validation of the trade and execute
        stockTrade.trade();
        return stockTrade.getNewPortfolio();
    }

    @Override
    public Portfolio sellShares(String clientId, String symbol, int shares) {
        StockTrade stockTrade = new StockTrade(clientId, symbol, shares, StockTradeCall.SELL);
        stockTrade.validate(); // validation does not acquire any locks. the pattern dictates that only when a trade seems valid should the structures be locked to then do a final validation of the trade and execute
        stockTrade.trade();
        return stockTrade.getNewPortfolio();
    }

    @Override
    public Portfolio getClientPortfolio(String clientId) {
        ClientRecord clientRecord = clientIdToClientRecord.get(clientId);
        boolean clientNotRegistered = (clientRecord == null);
        if (clientNotRegistered) throw new IllegalArgumentException("clientNotRegistered: " + clientId);            

        Portfolio portfolio = new PortfolioImpl(clientRecord);        
        return portfolio;
    }

    @Override
    public Map<String, Double> getStockPrices() {
        return stockPrices.getStockPrices();
    }

    class StockTrade {
        enum StockTradeCall {BUY, SELL};
        private final StockRecord  stockRecord;
        private final ClientRecord clientRecord; 
        private final String clientId;       
        private final String symbol;
        private final StockTradeCall call;

        private double  currentStockPrice;
        private int     currentStockQuantity;
        private int     deltaofStockQuantity;
        private double  currentClientBalance;
        private double  deltaofClientBalance;
        private int     currentClientStockQuantity;
        
        private Portfolio newPortfolio;

        StockTrade(String clientId, String symbol, int shares, StockTradeCall call) {
            this.clientId = clientId;
            this.symbol = symbol;
            this.deltaofStockQuantity = shares;
            this.call = call;
            this.stockRecord = symmbolToStockRecord.get(symbol);            
            this.clientRecord = clientIdToClientRecord.get(clientId);           
            
            // * @throws IllegalArgumentException    if the client is not registered, or the
            // *                                     symbol is invalid, pr the shares is not
            // *                                     positive
            boolean clientNotRegistered = (clientRecord == null);
            boolean symbolInvalid = (stockRecord == null);
            boolean negativeShares = (deltaofStockQuantity < 0);

            if (clientNotRegistered) throw new IllegalArgumentException("clientNotRegistered: " + clientId);            
            if (symbolInvalid) throw new IllegalArgumentException("symbolInvalid: " + symbol);                    
            if (negativeShares) throw new IllegalArgumentException("negativeShares: " + shares);                
        }

        private void readRecords() {
            this.currentStockPrice = stockPrices.getStockPrice(symbol);
            this.currentStockQuantity = stockRecord.quantity.get();
            this.currentClientBalance = clientRecord.balance.get();
            this.deltaofClientBalance = currentStockPrice * deltaofStockQuantity;
            this.currentClientStockQuantity = clientRecord.positions.getValOrDefault(symbol, 0);
        }

        public void validate() {
            this.readRecords();
            if (call.equals(StockTradeCall.BUY)) {
                boolean insufficientShares = currentStockQuantity < deltaofStockQuantity;
                boolean insufficientFunds = currentClientBalance < deltaofClientBalance; 
                
                if (insufficientShares) throw new InsufficientSharesException();
                if (insufficientFunds) throw new InsufficientFundsException();
            }

            if (call.equals(StockTradeCall.SELL)) {
                boolean insufficientShares = currentClientStockQuantity < deltaofStockQuantity;
                
                if (insufficientShares) throw new InsufficientSharesException();
            }
        }

        public void trade() {
            synchronized (clientRecord.balance) {
                synchronized (clientRecord.positions) {
                    synchronized (stockRecord.quantity) {
                        this.validate(); // validate again within the synchronized block. this pattern ensures that the 'set' methods of the SynchronizedRecords don't fail
                        boolean tradeSuccess = false;
                        if (call.equals(StockTradeCall.BUY)) tradeSuccess = this.tradeBuy();
                        if (call.equals(StockTradeCall.SELL)) tradeSuccess = this.tradeSell();
                        if (!tradeSuccess) throw new IllegalStateException("Trade failed when it should not have. Offending StockTrade: " + this.toString());
                    }
                    this.newPortfolio = new PortfolioImpl(clientRecord);
                }
            }
        }

        private boolean tradeBuy() {
            return (stockRecord.quantity.set(currentStockQuantity, currentStockQuantity - deltaofStockQuantity) && 
            clientRecord.positions.setVal(symbol, currentClientStockQuantity, currentClientStockQuantity + deltaofStockQuantity) &&
            clientRecord.balance.set(currentClientBalance, currentClientBalance - deltaofClientBalance));
        }
        
        private boolean tradeSell() {
            return (stockRecord.quantity.set(currentStockQuantity, currentStockQuantity + deltaofStockQuantity) && 
            clientRecord.positions.setVal(symbol, currentClientStockQuantity, currentClientStockQuantity - deltaofStockQuantity) &&
            clientRecord.balance.set(currentClientBalance, currentClientBalance + deltaofClientBalance));            
        }
        
        public Portfolio getNewPortfolio() {
            return this.newPortfolio;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("StockTrade {");
            sb.append("clientId=").append(clientId);
            sb.append("symbol=").append(symbol);
            sb.append("call=").append(call);
            sb.append("currentStockPrice=").append(currentStockPrice);
            sb.append("currentStockQuantity=").append(currentStockQuantity);
            sb.append("deltaofStockQuantity=").append(deltaofStockQuantity);
            sb.append("currentClientBalance=").append(currentClientBalance);
            sb.append("deltaofClientBalance=").append(deltaofClientBalance);
            sb.append("currentClientStockQuantity=").append(currentClientStockQuantity);
            sb.append("newPortfolio=").append(newPortfolio);
            sb.append("}");
            return sb.toString();
        }
    }   
}
