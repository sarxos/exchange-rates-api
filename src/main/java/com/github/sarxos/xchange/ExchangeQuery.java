package com.github.sarxos.xchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.xchange.impl.FetchOpenExchangeImpl;
import com.github.sarxos.xchange.impl.FetchYahooImpl;


public class ExchangeQuery {

	/**
	 * I'm the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ExchangeQuery.class);

	/**
	 * Convert from currency.
	 */
	private Collection<String> from;

	/**
	 * Convert to currency.
	 */
	private String to;

	public ExchangeQuery from(String... symbols) {
		from = new ArrayList<>(Arrays.asList(symbols));
		return this;
	}

	public ExchangeQuery from(Collection<String> symbols) {
		from = symbols;
		return this;
	}

	public ExchangeQuery to(String symbol) {
		to = symbol;
		return this;
	}

	public Collection<ExchangeRate> get() throws ExchangeException {

		// return empty list if input is empty

		if (from.isEmpty()) {
			return Collections.emptyList();
		}

		// first, try Yahoo

		LOG.debug("Trying Yahoo Exchange YQL API");

		try {
			return new FetchYahooImpl().get(to, from);
		} catch (ExchangeException e) {
			LOG.error("Unable to get exchange data from Yahoo", e);
		}

		// then, if Yahoo failed, try OpenExchange

		LOG.debug("Trying OpenExchangeRates API");

		try {
			return new FetchOpenExchangeImpl().get(to, from);
		} catch (ExchangeException e) {
			LOG.error("Unable to get exchange data from Yahoo", e);
		}

		return null;
	}
}
