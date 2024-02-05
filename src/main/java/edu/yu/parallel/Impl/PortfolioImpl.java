package edu.yu.parallel.Impl;

import java.util.Collections;
import java.util.Map;

import edu.yu.parallel.Portfolio;
import edu.yu.parallel.PortfolioPrototype;

final public class PortfolioImpl implements Portfolio {
    private final String clientId;
    private final double accountBalance;
    private final Map<String, Integer> stockPositions;

    
    /**
     * Contstructor
     * 
     * @param clientId
     * @param accountBalance
     * @param stockPositions
     */
    public PortfolioImpl(String clientId, double accountBalance, Map<String, Integer> stockPositions) {
        this.clientId = clientId;
        this.accountBalance = accountBalance;
        this.stockPositions = stockPositions;
    }

    public PortfolioImpl(PortfolioPrototype portfolioPrototype) {
        this.clientId = portfolioPrototype.getClientId();                    
        this.accountBalance = portfolioPrototype.getBalance();                    
        this.stockPositions = portfolioPrototype.getStockPositions();                        
    }

    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Override
    public double getBalance() {
        return this.accountBalance;
    }

    @Override
    public Map<String, Integer> getStockPositions() {
        return Collections.unmodifiableMap(stockPositions);
    }

    @Override
    public boolean equals(Object obj) {
        Portfolio other = (Portfolio) obj;
        return (
            this.clientId.equals(other.getClientId()) &&   
            (Math.abs(this.accountBalance - other.getBalance()) < .001) &&
            this.stockPositions.equals(other.getStockPositions()) 
        );
    }
}
