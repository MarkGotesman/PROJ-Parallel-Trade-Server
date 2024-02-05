package edu.yu.parallel.Impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

import edu.yu.parallel.App;
import edu.yu.parallel.BrokerService;
import edu.yu.parallel.Brokerage;
import edu.yu.parallel.Brokerage.InsufficientFundsException;
import edu.yu.parallel.Brokerage.InsufficientSharesException;
import edu.yu.parallel.Portfolio;

final public class BrokerServiceImpl implements BrokerService {
    public class InvalidHttpMethodException extends Exception {
    }

    public class IncompleteHttpHandlingException extends Exception {
    }

    private static final Logger logger = LogManager.getLogger(App.class);
    private final Brokerage brokerage;

    /**
     * Constructor
     * 
     * @param brokerage
     */
    public BrokerServiceImpl(Brokerage brokerage) {
        this.brokerage = brokerage;
    }

    @Override
    public void handleRegisterClient(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "POST";
        logger.info("In handleRegisterClient with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String clientId = query.get("clientId");
        double funds = Double.parseDouble(query.get("funds"));
        Callable<Portfolio> brokerMethod = () -> brokerage.registerClient(clientId, funds);

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Portfolio portfolio = null;
        try {
            portfolio = httpExchangeHandler.call(brokerMethod);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }
        String outgoingHttpBody = portfolioToJSON(portfolio).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    @Override
    public void handleBuyRequest(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "POST";
        logger.info("In handleBuyRequest with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String clientId = query.get("clientId");
        String symbol = query.get("symbol");
        int shares = Integer.parseInt(query.getOrDefault("shares", "-1"));
        Callable<Portfolio> brokerMethod = () -> brokerage.buyShares(clientId, symbol, shares);

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Portfolio portfolio = null;
        try {
            portfolio = httpExchangeHandler.call(brokerMethod);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }
        String outgoingHttpBody = portfolioToJSON(portfolio).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    @Override
    public void handleSellRequest(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "POST";
        logger.info("In handleSellRequest with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String clientId = query.get("clientId");
        String symbol = query.get("symbol");
        int shares = Integer.parseInt(query.getOrDefault("shares", "-1"));
        Callable<Portfolio> brokerMethod = () -> brokerage.sellShares(clientId, symbol, shares);

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Portfolio portfolio = null;
        try {
            portfolio = httpExchangeHandler.call(brokerMethod);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }
        String outgoingHttpBody = portfolioToJSON(portfolio).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    @Override
    public void handleGetPortfolio(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "GET";
        logger.info("In handleGetPortfolio with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String clientId = query.get("clientId");
        Callable<Portfolio> brokerMethod = () -> brokerage.getClientPortfolio(clientId);

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Portfolio portfolio = null;
        try {
            portfolio = httpExchangeHandler.call(brokerMethod);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }
        String outgoingHttpBody = portfolioToJSON(portfolio).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    @Override
    public void handleGetMarketValue(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "GET";
        logger.info("In handleGetMarketValue with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String clientId = query.get("clientId");
        Callable<Portfolio> brokerMethodGetClientPortfolio = () -> brokerage.getClientPortfolio(clientId);
        Callable<Map<String, Double>> brokerMethodGetStockPrices = () -> brokerage.getStockPrices();

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Portfolio portfolio = null;
        try {
            portfolio = httpExchangeHandler.call(brokerMethodGetClientPortfolio);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }

        Map<String, Double> prices = null;
        try {
            prices = httpExchangeHandler.call(brokerMethodGetStockPrices);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }

        double marketValue = calculateMarketValueFromPortfolio(portfolio, prices);
        String outgoingHttpBody = new JSONObject().put("marketvalue", marketValue).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    @Override
    public void handleGetPrices(HttpExchange exchange) throws IOException {
        String intendedIncomingHttpMethod = "GET";
        logger.info("In handleGetPrices with URI: " + exchange.getRequestURI() + ". Request Method: "
                + exchange.getRequestMethod());
        Callable<Map<String, Double>> brokerMethod = () -> brokerage.getStockPrices();

        HttpExchangeHandler httpExchangeHandler = new HttpExchangeHandler(exchange);

        try {
            httpExchangeHandler.validateIncomingHttpMethod(intendedIncomingHttpMethod);
        } catch (InvalidHttpMethodException e) {
            httpExchangeHandler.flush();
            return;
        }

        Map<String, Double> prices = null;
        try {
            prices = httpExchangeHandler.call(brokerMethod);
        } catch (IncompleteHttpHandlingException e) {
            httpExchangeHandler.flush();
            return;
        }

        String outgoingHttpBody = convertMapToJsonArray(prices).toString();
        httpExchangeHandler.setOutgoingHttpResponseCode(HttpURLConnection.HTTP_OK); // 200
        httpExchangeHandler.setOutgoingHttpBody(outgoingHttpBody);
        httpExchangeHandler.flush();
        return;
    }

    private Double calculateMarketValueFromPortfolio(Portfolio portfolio, Map<String, Double> symbolToPrices) {
        double clientMarketValue = 0.0;
        Map<String, Integer> clientPositions = portfolio.getStockPositions();
        for (String symbol : clientPositions.keySet()) {
            double stockValue = symbolToPrices.get(symbol);
            int stockShares = clientPositions.get(symbol);
            clientMarketValue += stockValue * stockShares;
        }
        return clientMarketValue;
    }

    private JSONArray convertMapToJsonArray(Map<?, ?> map) {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(entry.getKey().toString(), entry.getValue().toString());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /*
     * this class will have the fields needed to flush, that is complete, the
     * HttpExchange. These fields are the response code and the body in JSON format.
     * first, this class will check that the HTTP method of the http exchange
     * matches the intended method.
     * then this class will be given a callable to run of return type T. it will
     * call that callable. If it gets and excpetion (or per the above scenario, the
     * Http method is found to be wrong)
     * then this object's fields representing the data to return will be set
     * appropriately (using exceptionToJSON and setting the resonse code as it
     * should) and then will throw an "UnfinishedCallException".
     * The calling method should have wrapped this in a try-catch. when it catches
     * that exception, it should choose to either flush the httpexchange as it is,
     * or add more stuff as needed in the particular case (but for this hw it will
     * just flush)
     * if the callable returns succesfully, this object will return to the caller
     * the return. the caller will then manipualte that return into the appropriate
     * JSON format, and then set that json back to this object (and set the response
     * code to 200).
     * then the caller will call 'flush()' on this object to flush the http exchange
     * data
     */
    class HttpExchangeHandler {
        private final HttpExchange httpExchange;
        // private final Callable<?> callable;

        private final String outgoingHttpContentType = "application/json";
        private String outgoingHttpBody;
        private int outgoingHttpResponseCode;

        public HttpExchangeHandler(HttpExchange httpExchange) {
            this.httpExchange = httpExchange;
        }

        public void validateIncomingHttpMethod(String intentedIncomingHttpMethod) throws InvalidHttpMethodException {
            String incomingHttpMethod = httpExchange.getRequestMethod();
            if (!incomingHttpMethod.equals(intentedIncomingHttpMethod)) {
                this.outgoingHttpBody = messageToJSON("!incomingHttpMethod[" + incomingHttpMethod
                        + "].equals(intentedIncomingHttpMethod[" + intentedIncomingHttpMethod + "])").toString();
                this.outgoingHttpResponseCode = HttpURLConnection.HTTP_BAD_METHOD; // 405
                throw new InvalidHttpMethodException();
            }
        }

        public <T> T call(Callable<T> callable)
                throws IncompleteHttpHandlingException, IncompleteHttpHandlingException {
            T ret;
            try {
                ret = callable.call();
            } catch (IllegalArgumentException | InsufficientSharesException | InsufficientFundsException e) {
                this.outgoingHttpBody = exceptionToJSON(e).toString();
                this.outgoingHttpResponseCode = HttpURLConnection.HTTP_BAD_REQUEST; // 400
                throw new IncompleteHttpHandlingException();
            } catch (Exception e) {
                this.outgoingHttpBody = exceptionToJSON(e).toString();
                this.outgoingHttpResponseCode = HttpURLConnection.HTTP_INTERNAL_ERROR; // 500
                throw new IncompleteHttpHandlingException();
            }
            return ret;
        }

        public void setOutgoingHttpBody(String outgoingHttpBody) {
            this.outgoingHttpBody = outgoingHttpBody;
        }

        public void setOutgoingHttpResponseCode(int outgoingHttpResponseCode) {
            this.outgoingHttpResponseCode = outgoingHttpResponseCode;
        }

        // flush the http exchange out with the current data stored in the JSON Object
        // as the body
        public void flush() throws IOException {
            // Set Content-Type header
            httpExchange.getResponseHeaders().set("Content-Type", this.outgoingHttpContentType);

            // Set HTTP status code
            httpExchange.sendResponseHeaders(this.outgoingHttpResponseCode, 0);

            // Get output stream
            OutputStream outputStream = httpExchange.getResponseBody();

            // Convert the outgoingHttpBody to bytes and write to the output stream
            byte[] responseBytes = this.outgoingHttpBody.getBytes("UTF-8");
            outputStream.write(responseBytes);
            outputStream.close();
        }

    }

    // JSON Utility Methods
    private JSONObject exceptionToJSON(Exception e) {
        JSONObject mainJson = new JSONObject();
        mainJson.put("type", e.getClass().getSimpleName());
        mainJson.put("message", e.getMessage());

        JSONArray stackTraceArray = new JSONArray();
        for (StackTraceElement element : e.getStackTrace()) {
            stackTraceArray.put(element.toString());
        }

        mainJson.put("stacktrace", stackTraceArray);

        return mainJson;
    }

    private JSONObject portfolioToJSON(Portfolio portfolio) {
        // Create the main JSONObject and populate it
        JSONObject mainJson = new JSONObject();
        mainJson.put("clientId", portfolio.getClientId());
        mainJson.put("accountBalance", portfolio.getBalance());
        mainJson.put("stockPositions", new JSONObject(portfolio.getStockPositions()));

        return mainJson;
    }

    private JSONObject messageToJSON(String message) {
        return new JSONObject().put("message", message);
    }

    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
