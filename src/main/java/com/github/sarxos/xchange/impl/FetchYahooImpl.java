package com.github.sarxos.xchange.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.github.sarxos.xchange.ExchangeException;
import com.github.sarxos.xchange.ExchangeRate;


public class FetchYahooImpl extends Fetch {

	/**
	 * I'm the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(FetchYahooImpl.class);

	private static final String QUERY = "select * from yahoo.finance.xchange where pair in ( symbols )";
	private static final String HOST = "query.yahooapis.com";
	private static final String ENDPOINT = "/v1/public/yql";
	private static final String ENV = "store://datatables.org/alltableswithkeys";
	private static final String FORMAT = "json";
	private static final String QUOTE = "\"";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static HttpUriRequest buildRequest(String list) {

		URI uri = null;
		try {
			uri = new URIBuilder()
				.setScheme("https")
				.setHost(HOST)
				.setPath(ENDPOINT)
				.setParameter("format", FORMAT)
				.setParameter("env", ENV)
				.setParameter("q", QUERY.replaceAll("symbols", list))
				.build();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Ooops, the URI string seems to be wrong!", e);
		}

		LOG.debug("New URI crafted {}", uri);

		return new HttpGet(uri);
	}

	private static String fetch(Iterator<String> symbols) throws ExchangeException {

		Collection<String> iterable = IteratorUtils.toList(symbols);

		LOG.debug("Fetching symbols {}", iterable);

		Collection<String> quoted = CollectionUtils.collect(iterable, new Transformer<String, String>() {

			@Override
			public String transform(String input) {
				return QUOTE + input + QUOTE;
			}
		});

		String list = StringUtils.join(quoted.iterator(), ",");
		HttpUriRequest request = buildRequest(list);

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
			throw new ExchangeException("Unable to get JSON from Yahoo API");
		}

		LOG.debug("Successfully requested symbols from Yahoo Exchange YQL API");
		LOG.trace("Response is {}", json);

		return json;
	}

	private static String fetch(Collection<String> symbols) throws ExchangeException {
		return fetch(symbols.iterator());
	}

	private static String fetch(final String to, final String... from) throws ExchangeException {

		Iterator<String> iterator = IteratorUtils.arrayIterator(from);
		Collection<String> symbols = CollectionUtils.collect(iterator, new Transformer<String, String>() {

			@Override
			public String transform(String input) {
				return to + input;
			}
		});

		return fetch(symbols);
	}

	private static Collection<ExchangeRate> read(String json) throws JsonProcessingException, IOException {

		// example JSON

		// {
		// "query":{
		// "count":3,
		// "created":"2015-03-30T08:08:21Z",
		// "lang":"en-US",
		// "results":{
		// "rate":[
		// {
		// "id":"EURUSD",
		// "Name":"EUR/USD",
		// "Rate":"1.0840",
		// "Date":"3/30/2015",
		// "Time":"9:08am",
		// "Ask":"1.0840",
		// "Bid":"1.0840"
		// }, ... etc, 2 more records here
		// ]}}}

		LOG.trace("Reading JSON tree: {}", json);

		JsonNode root = MAPPER.readTree(json);
		if (root == null) {
			throw new IOException("Invalid JSON received: " + json);
		}

		JsonNode query = root.get("query");
		if (query == null) {
			throw new IOException("No query element has been found in received JSON: " + json);
		}

		JsonNode results = query.get("results");
		if (results == null) {
			throw new IOException("No results element has been found in received JSON: " + json);
		}

		JsonNode rates = results.get("rate");
		if (rates == null) {
			throw new IOException("No rate element has been found in received JSON: " + json);
		}

		Set<ExchangeRate> exchangerates = new HashSet<>();

		if (rates.isArray()) {
			for (int i = 0; i < rates.size(); i++) {
				process(exchangerates, rates.get(i));
			}
		} else {
			process(exchangerates, rates);
		}

		return exchangerates;
	}

	private static void process(Set<ExchangeRate> exchangerates, JsonNode node) {

		String symbol = node.get("id").asText();
		String value = node.get("Rate").asText();

		if ("N/A".equalsIgnoreCase(value)) {
			LOG.debug("The rate for {} has not been found", symbol);
		} else {
			exchangerates.add(new ExchangeRate(symbol, value));
		}
	}

	public Collection<ExchangeRate> get(String to, Collection<String> from) throws ExchangeException {
		try {
			return read(fetch(to, from.toArray(new String[0])));
		} catch (IOException e) {
			throw new ExchangeException(e);
		}
	}
}
