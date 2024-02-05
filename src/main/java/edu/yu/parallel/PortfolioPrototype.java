package edu.yu.parallel;

import java.util.Map;

public interface PortfolioPrototype {
	String getClientId();
	double getBalance();
	Map<String, Integer> getStockPositions();
}
