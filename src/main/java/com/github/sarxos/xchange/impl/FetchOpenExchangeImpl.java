package com.github.sarxos.xchange.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sarxos.xchange.ExchangeCache;
import com.github.sarxos.xchange.ExchangeException;
import com.github.sarxos.xchange.ExchangeRate;


public class FetchOpenExchangeImpl extends Fetch {

	/**
	 * I'm the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(FetchOpenExchangeImpl.class);

	private static final String KEY = "openexchangerates.org.apikey";
	private static final String HOST = "openexchangerates.org";
	private static final String ENDPOINT = "/api/latest.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static HttpUriRequest buildRequest() {

		Object apikey = ExchangeCache.getParameter(KEY);
		if (apikey == null) {
			throw new IllegalStateException("API key has not been found under " + KEY + " parameter");
		}

		URI uri = null;
		try {
			uri = new URIBuilder()
				.setScheme("http")
				.setHost(HOST)
				.setPath(ENDPOINT)
				.setParameter("app_id", apikey.toString())
				.build();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Ooops, the URI string seems to be wrong!", e);
		}

		LOG.debug("New URI crafted {}", uri);

		return new HttpGet(uri);
	}

	private static String fetch() throws ExchangeException {

		LOG.trace("Fetching OpenExchangeRates latest JSON");

		HttpUriRequest request = buildRequest();

		String json = null;
		try (
			CloseableHttpClient client = client();
			CloseableHttpResponse response = client.execute(request); // .
		) {

			HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new ExchangeException("Unable to get HTTP response");
			}

			try (InputStream is = entity.getContent()) {
				json = IOUtils.toString(is);
			}

		} catch (IOException e) {
			LOG.debug("HTTP call failed when requesting {}", request);
			LOG.error("Unable to perform HTTP GET operation", e);
		}

		if (json == null) {
			throw new ExchangeException("Unable to get JSON from OpenExchangeRates.org API");
		}

		return json;
	}

	private static Collection<ExchangeRate> read(String json, String to, Collection<String> from) throws JsonProcessingException, IOException {

		// example JSON

		// {
		// "disclaimer": "...",
		// "license": "...",
		// "timestamp": 1427716861,
		// "base": "USD",
		// "rates": {
		// "AED": 3.67305,
		// "AFN": 57.780167,
		// "ALL": 129.4859,
		// "AMD": 471.586001,
		// "ANG": 1.78948,
		// "AOA": 107.97125,
		// ... etc for more symbols

		JsonNode root = MAPPER.readTree(json);
		if (root == null) {
			throw new IOException("Invalid JSON received: " + json);
		}

		JsonNode rates = root.get("rates");
		if (rates == null) {
			throw new IOException("No rate element has been found in received JSON: " + json);
		}

		Iterator<Entry<String, JsonNode>> entries = rates.fields();
		Map<String, BigDecimal> currencies = new HashMap<>();

		while (entries.hasNext()) {

			Entry<String, JsonNode> entry = entries.next();
			String symbol = entry.getKey();
			String value = entry.getValue().asText();

			currencies.put(symbol, new BigDecimal(value));
		}

		BigDecimal base = currencies.get(to);
		Set<ExchangeRate> exchangerates = new HashSet<>();

		for (Entry<String, BigDecimal> entry : currencies.entrySet()) {

			String symbol = entry.getKey();

			if (!from.contains(symbol)) {
				continue;
			}

			BigDecimal rate = entry.getValue();
			BigDecimal newrate = rate.divide(base, 8, RoundingMode.HALF_EVEN);

			exchangerates.add(new ExchangeRate(to + symbol, newrate.toString()));
		}

		return exchangerates;
	}

	public Collection<ExchangeRate> get(String to, Collection<String> from) throws ExchangeException {
		try {
			return read(fetch(), to, from);
		} catch (IOException e) {
			throw new ExchangeException(e);
		}
	}

}
