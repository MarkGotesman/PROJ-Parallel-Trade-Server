import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import java.lang.Thread.State;

import edu.yu.parallel.App;


public class PublicAPITest {
	final static int brokerCount = 10;
	final String baseURL = "http://localhost:8080";
	final RestClient client = RestClient.create();

	@BeforeEach
    public void setUp() {
		Thread t = new Thread (() -> {
			try {
				App.main(new String[]{Integer.toString(brokerCount)});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		t.start();
		while (!t.getState().equals(State.WAITING)) {}
    }
	


	
	/**
	 * The BrokerService interface defines the methods that must be implemented by a
	 * class that provides the functionality of a broker service.
	 * 
	 * The BrokerService is responsible for handling requests from clients and
	 * returning the appropriate responses.
	 * 
	 * HTTP response codes are as follows:
	 *   - Successful requests should return a 200 response code.
	 *   - Requests with an incorrect HTTP Method should return a 405 response code
	 *   - Requests resulting in the the following exceptions should return a 400 response code:
	 *      - IllegalArgumentException
	 *      - InsufficientSharesException
	 *      - InsufficientFundsException
	 *   - Requests resulting in any other exception should return a 500 response code.
	 *   
	 * 
	 */
	
		/**
		 * Handle a request to register a client
		 * POST /register
		 * Query Parameters: clientId, funds
		 * Response: JSON representation of the client's Portfolio
		 * 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void RegisterClientTest() {
			int clientId = 1;
			int funds = 100000;
			
			String[] queryParams = new String[] {
				"clientId", 
				"" + clientId, 
				"funds",
				"" + funds};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.POST, "register", queryParams);
			printResponse(result);

		}
	
		/**
		 * Handle a request to buy shares of a stock for a client
		 * POST /buy
		 * Query Parameters: clientId, symbol, shares
		 * Response: JSON representation of the client's Portfolio
		 * 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void BuyRequestTest() {
			RegisterClientTest();
			int clientId = 1;
			String symbol = "AAPL";
			int shares = 5; 
			
			String[] queryParams = new String[] {
				"clientId", 
				"" + clientId, 
				"shares", 
				"" + shares, 
				"symbol",
				"" + symbol};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.POST, "buy", queryParams);
			printResponse(result);

		}
	
		/**
		 * Handle a request to sell shares of a stock for a client
		 * POST /sell
		 * Query Parameters: clientId, symbol, shares
		 * Response: JSON representation of the client's Portfolio
		 * 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void SellRequestTest() {
			BuyRequestTest();

			int clientId = 1;
			String symbol = "AAPL";
			int shares = 5; 
			
			String[] queryParams = new String[] {
				"clientId", 
				"" + clientId, 
				"shares", 
				"" + shares, 
				"symbol",
				"" + symbol};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.POST, "sell", queryParams);
			printResponse(result);

		}
	
		/**
		 * Handle a request to get a client's portfolio
		 * GET /portfolio
		 * Query Parameters: clientId
		 * Response: JSON representation of the client's Portfolio
		 * 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void GetPortfolioTest() {
			RegisterClientTest();

			int clientId = 1;
			
			String[] queryParams = new String[] {
				"clientId", 
				"" + clientId,
				};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.GET, "portfolio", queryParams);
			printResponse(result);

		}
		
		/**
		 * Handle a request to get the market value of a client's portfolio
		 * GET /marketvalue
		 * Query Parameters: clientId
		 * Response: JSON object containing the total market value of the client's portfolio
		 * {"marketvalue": 123.45"}
		 * 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void GetMarketValueTest() {
			RegisterClientTest();
			
			int clientId = 1;
			
			String[] queryParams = new String[] {
				"clientId", 
				"" + clientId 
				};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.GET, "marketvalue", queryParams);
			printResponse(result);

		}
		
		/**
		 * Handle a request to get the current stock prices
		 * GET /prices
		 * Response: a JSON array of JSON objects, All containing the symbol and price of a stock
		 * Query Parameters: 
		 * @param exchange the HttpExchange object
		 * 
		 */
		@Test
		void GetPricesTest() {
			
			String[] queryParams = new String[] {};

			ResponseEntity<String> result = makeHttpReuest(HttpMethod.GET, "prices", queryParams);
			printResponse(result);

		}
	
	@AfterEach
	public void tearDown() {
		App.shutDown();	
	}

	private void printResponse (ResponseEntity<String> responseEntity) {
		System.out.println("Response status: " + responseEntity.getStatusCode()); 
		System.out.println("Response headers: " + responseEntity.getHeaders()); 
		System.out.println("Contents: " + responseEntity.getBody()); 	
	}

	private ResponseEntity<String> makeHttpReuest(HttpMethod httpMethod, String endpoint, String[] queryParams) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < queryParams.length; i+=2) {
			String key = queryParams[i];
			String val = queryParams[i+1];
			stringBuilder.append(key + "=" + val + "&");
		}
		String uri = baseURL + "/" + endpoint + "?" + stringBuilder.toString();

		return client.method(httpMethod)
					.uri(uri)
					.retrieve()
					.toEntity(String.class);
	}

}
