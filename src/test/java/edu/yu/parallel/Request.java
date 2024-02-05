package edu.yu.parallel;
import com.google.gson.Gson;

import static edu.yu.parallel.Request.extractEntityResponseCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

import edu.yu.parallel.Impl.PortfolioImpl;

abstract class Request {
	protected final String baseURL = "http://localhost:8080";
	protected Map<String, String> queryParams = new HashMap<>();
	protected String endpoint;
	protected HttpMethod httpMethod;

	public abstract Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPrioRequest);

	protected static int extractEntityResponseCode(ResponseEntity<String> responseEntity) {
		return responseEntity.getStatusCode().value();
	}

	protected static Portfolio extractEntityBodyPortfolio(ResponseEntity<String> responseEntity) {
		String jsonString = responseEntity.getBody();
		Gson gson = new Gson();
		Portfolio portfolio = gson.fromJson(jsonString, PortfolioImpl.class);
		return portfolio;
	}
	
	protected static ResponseEntity<String> makeHttpRequest(RestClient client, Request request) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Map.Entry<String, String> entry : request.queryParams.entrySet()) {
			stringBuilder.append(entry.getKey() + "=" + entry.getValue() + "&");
		}
		String uri = request.baseURL + "/" + request.endpoint + "?" + stringBuilder.toString();

		return client.method(request.httpMethod)
					.uri(uri)
					.retrieve()
					.onStatus((code) -> (code.value() == 400 || code.value() == 405 || code.value() == 500), new ErrorHandler() {
						@Override
						public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
						}
					})
					.toEntity(String.class);
	}

	protected static List<ResponseEntity<String>> makeHttpRequests(RestClient client, List<Request> requests) {
		List<ResponseEntity<String>> ret = new ArrayList<>();
		for (Request request : requests) {
			ret.add(makeHttpRequest(client, request));
		}
		return ret;
	}
	protected static Portfolio preDeterminePortfolio (List<Request> requests) {
		if (!(requests.get(0) instanceof RequestRegisterClient)) {
			throw new IllegalArgumentException("[ERROR] preDeterminePortfolio must starte with a request to register client or be provided with a portfolio");
		}
		Portfolio portfolio = requests.get(0).calculatePortfolioAfterRequest(null);
		return preDeterminePortfolio(requests, portfolio);
	}

	protected static Portfolio preDeterminePortfolio (List<Request> requests, Portfolio portfolio) {
		for (Request request : requests) {
			portfolio = request.calculatePortfolioAfterRequest(portfolio);
		}
		return portfolio;
	}
}

class RequestSequence implements Callable<Void> {
	final RestClient restClient;
	final String clientId;
	final double initialFunds;
	List<Request> requestList;
	Portfolio portfolioExpected;
	Portfolio portfolioActual;
	List<ResponseEntity<String>> requestsResponses;
	
	RequestSequence(String clientId,double initialFunds) {
		this.restClient = RestClient.create();
		this.clientId = clientId;
		this.initialFunds = initialFunds;
	}
	
	public void addRequestList(List<Request> requestList) {
		this.requestList = requestList;
		requestList.add(0, new RequestRegisterClient(clientId, initialFunds));
		this.portfolioExpected = Request.preDeterminePortfolio(requestList);				
	}

	@Override
	public Void call() {
		this.requestsResponses = Request.makeHttpRequests(restClient, requestList);
		this.portfolioActual = Request.extractEntityBodyPortfolio(Request.makeHttpRequest(restClient, new RequestGetPortfolio(clientId)));
		return null;
	}

	public int getLastResponseCode() {
		return extractEntityResponseCode(requestsResponses.get(requestsResponses.size()-1));
	}

	public boolean portfolioIsConsistent() {
		return portfolioExpected.equals(portfolioActual);
	}
}


class RequestRegisterClient extends Request {
	RequestRegisterClient(String clientId, double funds) {
		this.queryParams.put("clientId", clientId); this.queryParams.put("funds", Double.toString(funds));			
		this.httpMethod = HttpMethod.POST;
		this.endpoint = "register";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		return new PortfolioImpl(
			queryParams.get("clientId"),
			Double.parseDouble(queryParams.get("funds")), 
			new HashMap<String, Integer>());
	}
}

class RequestBuyRequest extends Request {
	RequestBuyRequest(String clientId, String symbol, int shares) {
		this.queryParams.put("clientId", clientId); this.queryParams.put("symbol",symbol); this.queryParams.put("shares", Integer.toString(shares));			
		this.httpMethod = HttpMethod.POST;
		this.endpoint = "buy";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		String clientId = queryParams.get("clientId"); 
		String symbol = queryParams.get("symbol");
		int shares = Integer.parseInt(queryParams.get("shares"));

		StockPrices stockPrices = StockPrices.getInstance(null);
		double valueDelta = stockPrices.getStockPrice(symbol) * shares;
		Map<String, Integer> stockPositions = new HashMap<>(portfolioPreRequest.getStockPositions());
		stockPositions.put(symbol, stockPositions.getOrDefault(symbol, 0) + shares);

		return new PortfolioImpl(
			clientId, 
			portfolioPreRequest.getBalance() - valueDelta , 
			stockPositions);

	}
}

class RequestSellRequest extends Request {
	RequestSellRequest(String clientId, String symbol, int shares) {
		this.queryParams.put("clientId", clientId); this.queryParams.put("symbol",symbol); this.queryParams.put("shares", Integer.toString(shares));			
		this.httpMethod = HttpMethod.POST;
		this.endpoint = "sell";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		String clientId = queryParams.get("clientId"); 
		String symbol = queryParams.get("symbol");
		int shares = Integer.parseInt(queryParams.get("shares"));

		StockPrices stockPrices = StockPrices.getInstance(null);
		double valueDelta = stockPrices.getStockPrice(symbol) * shares;
		Map<String, Integer> stockPositions = new HashMap<>(portfolioPreRequest.getStockPositions());
		stockPositions.put(symbol, stockPositions.getOrDefault(symbol, 0) - shares);

		return new PortfolioImpl(
			clientId, 
			portfolioPreRequest.getBalance() + valueDelta , 
			stockPositions);

	}
}

class RequestGetPortfolio extends Request {
	RequestGetPortfolio(String clientId) {
		this.queryParams.put("clientId", clientId);			
		this.httpMethod = HttpMethod.GET;
		this.endpoint = "portfolio";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		return portfolioPreRequest;
	}
}

class RequestGetMarketValue extends Request {
	RequestGetMarketValue(String clientId) {
		this.queryParams.put("clientId", clientId);			
		this.httpMethod = HttpMethod.GET;
		this.endpoint = "marketvalue";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		return portfolioPreRequest;
	}
}

class RequestGetPrices extends Request {
	RequestGetPrices() {
		this.httpMethod = HttpMethod.GET;
		this.endpoint = "prices";
	}
	
	@Override
	public Portfolio calculatePortfolioAfterRequest(Portfolio portfolioPreRequest) {
		return portfolioPreRequest;
	}
}