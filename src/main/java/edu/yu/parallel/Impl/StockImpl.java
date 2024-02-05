package edu.yu.parallel.Impl;

import edu.yu.parallel.Stock;

public class StockImpl implements Stock {
    private final String symbol;
    private final String name;
    private final double price;
    private final int shares;

    /**
     * Consructor
     * 
     * @param symbol
     * @param name
     * @param price
     * @param shares
     */
    public StockImpl(String symbol, String name, double price, int shares) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.shares = shares;        
    }

    @Override
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public double getPrice() {
        return this.price;
    }

    @Override
    public int getShares() {
        return this.shares;
    }

}
