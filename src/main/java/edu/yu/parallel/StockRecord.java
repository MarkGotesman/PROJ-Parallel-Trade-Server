package edu.yu.parallel;

public class StockRecord {
	final public SynchronizedRecordNumber<Integer> quantity;
	public StockRecord(int quantity) {
		this.quantity = new SynchronizedRecordNumber<>(quantity);
	}
}
