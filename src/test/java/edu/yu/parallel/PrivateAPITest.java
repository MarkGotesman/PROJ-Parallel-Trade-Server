package edu.yu.parallel;
import static edu.yu.parallel.Request.extractEntityBodyPortfolio;
import static edu.yu.parallel.Request.makeHttpRequest;
import static edu.yu.parallel.Request.makeHttpRequests;
import static edu.yu.parallel.Request.preDeterminePortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

import edu.yu.parallel.Impl.PortfolioImpl;


public class PrivateAPITest {
	final static int brokerCount = 10;
	Thread mainThread;

	@BeforeEach
    public void setUp() throws IOException {
		mainThread = new Thread (() -> {
			try {
				App.main(new String[]{Integer.toString(brokerCount)});
			} catch (IOException e) {}
		});
		mainThread.start();
		while (!mainThread.getState().equals(State.WAITING)) {}
		StockPrices.getInstance(null).stopUpdates(); // this non-randomness is needed for testing 
    }
	
	@Test
	void singleClientIndependentRequests() throws InterruptedException {
		RestClient restClient = RestClient.create();
		String clientId = "clyde";
		double initialFunds = 100000.0;
		List<Request> requestList =  List.of(
			new RequestRegisterClient(clientId, initialFunds),
			new RequestBuyRequest(clientId, "AAPL", 5),
			new RequestGetPrices()
		);
		Portfolio portfolioExpected = preDeterminePortfolio(requestList);
		
		makeHttpRequests(restClient, requestList);
		Portfolio portfolioActual = extractEntityBodyPortfolio(makeHttpRequest(restClient, new RequestGetPortfolio(clientId)));
		System.out.println(portfolioExpected.equals(portfolioActual));		
		assertEquals(portfolioExpected, portfolioActual);	
	}

	@Test
	void singleClientConflictingRequests() throws InterruptedException {
		RestClient restClient = RestClient.create();
		String clientId = "clyde";
		double initialFunds = 100000.0;
		List<Request> requestList =  List.of(
			new RequestRegisterClient(clientId, initialFunds),
			new RequestBuyRequest(clientId, "AAPL", 5),
			new RequestBuyRequest(clientId, "AMGN", 10),
			new RequestSellRequest(clientId, "AAPL", 3),
			new RequestGetMarketValue(clientId),
			new RequestGetPrices(),
			new RequestSellRequest(clientId, "AMGN", 3)
		);
		Portfolio portfolioExpected = preDeterminePortfolio(requestList);
		
		makeHttpRequests(restClient, requestList);
		Portfolio portfolioActual = extractEntityBodyPortfolio(makeHttpRequest(restClient, new RequestGetPortfolio(clientId)));
		
		assertEquals(portfolioExpected, portfolioActual);	
	}

	@Test
	void multiClientIndependentRequests() throws InterruptedException {
		RequestSequence c1Seq = new RequestSequence("client1", 10000.0);
		c1Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestBuyRequest(c1Seq.clientId, "AAPL", 5),
			new RequestBuyRequest(c1Seq.clientId, "AMGN", 10),
			new RequestSellRequest(c1Seq.clientId, "AAPL", 3),
			new RequestGetMarketValue(c1Seq.clientId),
			new RequestGetPrices(),
			new RequestSellRequest(c1Seq.clientId, "AMGN", 3)
			)));

		RequestSequence c2Seq = new RequestSequence("client2", 10000.0);
		c2Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestGetPrices(),
			new RequestBuyRequest(c2Seq.clientId, "CAT", 5),
			new RequestBuyRequest(c2Seq.clientId, "CSCO", 20),
			new RequestSellRequest(c2Seq.clientId, "CAT", 3),
			new RequestGetMarketValue(c2Seq.clientId),
			new RequestBuyRequest(c2Seq.clientId, "AXP", 3)
			)));
		

		RequestSequence c3Seq = new RequestSequence("client3", 10000.0);
		c3Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestGetPrices(),
			new RequestGetMarketValue(c3Seq.clientId),
			new RequestBuyRequest(c3Seq.clientId, "CVX", 5),
			new RequestBuyRequest(c3Seq.clientId, "GS", 20),
			new RequestSellRequest(c3Seq.clientId, "CVX", 3),
			new RequestBuyRequest(c3Seq.clientId, "IBM", 3),
			new RequestSellRequest(c3Seq.clientId, "GS", 5)
			)));
		

		ExecutorService executor = Executors.newCachedThreadPool();
		executor.invokeAll(List.of(c1Seq, c2Seq, c3Seq));
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);

		assertTrue(c1Seq.portfolioIsConsistent());
		assertTrue(c2Seq.portfolioIsConsistent());
		assertTrue(c3Seq.portfolioIsConsistent());
	}

	@Test
	void multiClientConflictingRequests() throws InterruptedException {
		RequestSequence c1Seq = new RequestSequence("client1", 10000.0);
		c1Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestBuyRequest(c1Seq.clientId, "AAPL", 5),
			new RequestBuyRequest(c1Seq.clientId, "AMGN", 10),
			new RequestSellRequest(c1Seq.clientId, "AAPL", 3),
			new RequestGetMarketValue(c1Seq.clientId),
			new RequestGetPrices(),
			new RequestSellRequest(c1Seq.clientId, "AMGN", 3)
			)));

		RequestSequence c2Seq = new RequestSequence("client2", 10000.0);
		c2Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestGetPrices(),
			new RequestBuyRequest(c2Seq.clientId, "AAPL", 5),
			new RequestBuyRequest(c2Seq.clientId, "AMGN", 20),
			new RequestSellRequest(c2Seq.clientId, "AAPL", 3),
			new RequestGetMarketValue(c2Seq.clientId),
			new RequestBuyRequest(c2Seq.clientId, "AMGN", 3)
			)));
		

		RequestSequence c3Seq = new RequestSequence("client3", 10000.0);
		c3Seq.addRequestList(
			new ArrayList<>(Arrays.asList(
			new RequestGetPrices(),
			new RequestGetMarketValue(c3Seq.clientId),
			new RequestBuyRequest(c3Seq.clientId, "AAPL", 5),
			new RequestBuyRequest(c3Seq.clientId, "AMGN", 20),
			new RequestSellRequest(c3Seq.clientId, "AAPL", 3),
			new RequestBuyRequest(c3Seq.clientId, "AAPL", 3),
			new RequestSellRequest(c3Seq.clientId, "AMGN", 5)
			)));
		

		ExecutorService executor = Executors.newCachedThreadPool();
		executor.invokeAll(List.of(c1Seq, c2Seq, c3Seq));
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);

		assertTrue(c1Seq.portfolioIsConsistent());
		assertTrue(c2Seq.portfolioIsConsistent());
		assertTrue(c3Seq.portfolioIsConsistent());
	}

	@Test
	public void errorTest() {
		RequestSequence wrongMethodSeq = new RequestSequence("c1", 10000.0);
		List<Request> requests = new ArrayList<>(Arrays.asList(
			new RequestBuyRequest(wrongMethodSeq.clientId, "AAPL", 5),
			new RequestGetPrices(),
			new RequestGetMarketValue(wrongMethodSeq.clientId),
			new RequestSellRequest(wrongMethodSeq.clientId, "AAPL", 3),
			new RequestGetPortfolio(wrongMethodSeq.clientId)
			));
		for (Request request : requests) {
			request.httpMethod = HttpMethod.PATCH; //nonsense method
			wrongMethodSeq.addRequestList(new ArrayList<>(Arrays.asList(request)));
			wrongMethodSeq.call();
			assertEquals(405, wrongMethodSeq.getLastResponseCode());
		}

		// RequestSequence illegalArgumentExceptionSeq = new RequestSequence("c2", 10000.0);
		// requests = new ArrayList<>(Arrays.asList(
		// 	new RequestBuyRequest(illegalArgumentExceptionSeq.clientId, "AAPL", 5),
		// 	new RequestGetPrices(),
		// 	new RequestGetMarketValue(illegalArgumentExceptionSeq.clientId),
		// 	new RequestSellRequest(illegalArgumentExceptionSeq.clientId, "AAPL", 3),
		// 	new RequestGetPortfolio(illegalArgumentExceptionSeq.clientId)
		// 	));
		// for (Request request : requests) {
		// 	request.queryParams = new HashMap<>(); 
		// 	request.queryParams.put("symbol","sadfadsf"); //nonsense query params 
		// 	request.queryParams.put("foo","bar"); //nonsense query params 
		// 	illegalArgumentExceptionSeq.addRequestList(new ArrayList<>(Arrays.asList(request)));
		// 	illegalArgumentExceptionSeq.call();
		// 	assertEquals(400, illegalArgumentExceptionSeq.getLastResponseCode());
		// }

		RequestSequence insufficientSharesException = new RequestSequence("InsufficientSharesException", 100_000_000);
		requests = new ArrayList<>(Arrays.asList(
			new RequestBuyRequest(insufficientSharesException.clientId, "AAPL", 10_000)
		));
		insufficientSharesException.addRequestList(requests);
		insufficientSharesException.call();
		assertEquals(400, insufficientSharesException.getLastResponseCode());
		
		RequestSequence insufficientFundsException = new RequestSequence("InsufficientFundsException", 10);
		requests = new ArrayList<>(Arrays.asList(
			new RequestBuyRequest(insufficientFundsException.clientId, "AAPL", 1)
		));
		insufficientFundsException.addRequestList(requests);
		insufficientFundsException.call();
		assertEquals(400, insufficientFundsException.getLastResponseCode());


	}

	@AfterEach
	public void tearDown() throws IOException, InterruptedException {
		App.shutDown();	
		while (!mainThread.getState().equals(State.TERMINATED)) {}
	}



}
