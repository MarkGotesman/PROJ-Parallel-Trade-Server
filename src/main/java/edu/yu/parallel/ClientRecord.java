package edu.yu.parallel;

import java.util.Map;

public class ClientRecord implements PortfolioPrototype {
	final private String clientId;
	final public SynchronizedRecordNumber<Double> balance;
	final public SynchronizedRecordMap<String, Integer> positions;
	public ClientRecord (String clientId, double balance, Map<String, Integer> positions) {
		this.clientId = clientId;
		this.balance = new SynchronizedRecordNumber<Double>(balance);
		this.positions = new SynchronizedRecordMap<String, Integer>(positions);
	}

	@Override
	public String getClientId() { return this.clientId; }

	@Override
	public double getBalance() { return this.balance.get(); }
		
	@Override
	public Map<String, Integer> getStockPositions() { return this.positions.getMap(); }
}
