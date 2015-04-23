package com.github.sarxos.xchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
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
	 * Transformer to convert exchange rate to symbol.
	 */
	private static final Transformer<ExchangeRate, String> TRANSFORMER = new Transformer<ExchangeRate, String>() {

		@Override
		public String transform(ExchangeRate rate) {
			return rate.getFrom();
		}
	};

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

		// resultant exchange rates

		Collection<ExchangeRate> rates = null;
		Collection<String> tmp1 = new HashSet<>(from);

		// first, try Yahoo

		LOG.debug("Trying Yahoo Exchange YQL API");

		try {
			rates = new FetchYahooImpl().get(to, tmp1);
		} catch (ExchangeException e) {
			LOG.error("Unable to get exchange data from Yahoo", e);
		}

		// check if all quotes has been found

		Collection<String> found = CollectionUtils.collect(rates, TRANSFORMER);
		Collection<String> remaining = new HashSet<>(from);
		remaining.removeAll(found);

		if (remaining.isEmpty()) {
			return rates;
		}

		Collection<String> tmp2 = new HashSet<>(remaining);

		// then, if Yahoo failed, try OpenExchange

		LOG.debug("Trying OpenExchangeRates API");

		try {
			rates = new FetchOpenExchangeImpl().get(to, tmp2);
		} catch (ExchangeException e) {
			LOG.error("Unable to get exchange data from OpenExchangeRates", e);
		}

		return rates;
	}
}
