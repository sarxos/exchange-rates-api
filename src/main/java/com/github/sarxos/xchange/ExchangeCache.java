package com.github.sarxos.xchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExchangeCache {

	/**
	 * I'm the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ExchangeCache.class);

	private static final long DEFAULT_INTERVAL = 60 * 60 * 1000; // 1h

	private static final Map<String, Object> PARAMETERS = new HashMap<String, Object>();

	private final class Updater extends Thread {

		public Updater() {
			super("exchange-cache-updater");
			setDaemon(true);
			start();
		}

		@Override
		public void run() {

			LOG.info("Starting exchange cache updater");

			while (true) {
				try {
					throttle();
					init();
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}

		private void throttle() throws InterruptedException {
			Thread.sleep(interval);
		}
	}

	private final ConcurrentHashMap<String, ExchangeRate> cache = new ConcurrentHashMap<>();
	private final String base;
	private final Collection<String> currencies = new HashSet<String>();
	private final long interval;
	private final Thread updater;

	public ExchangeCache(String base) {
		this(base, new ArrayList<String>(1));
	}

	public ExchangeCache(String base, String... currencies) {
		this(base, Arrays.asList(currencies));
	}

	public ExchangeCache(String base, Collection<String> currencies) {
		this(base, currencies, DEFAULT_INTERVAL);
	}

	public ExchangeCache(String base, Collection<String> currencies, long interval) {
		this.base = base;
		this.currencies.addAll(currencies);
		this.interval = interval;
		this.init();
		this.updater = new Updater();
	}

	protected Thread getUpdaterThread() {
		return updater;
	}

	private void cache(ExchangeRate rate) {

		LOG.debug("Caching exchange rate {}", rate);

		currencies.add(rate.getFrom());
		cache.put(rate.getFrom(), rate);
	}

	private synchronized void init() {

		if (currencies.isEmpty()) {
			LOG.info("No currencies to update");
		} else {
			LOG.info("Initializing forex data for {}", currencies);
		}

		Collection<ExchangeRate> rates = null;
		try {
			rates = new ExchangeQuery()
				.from(currencies)
				.to(base)
				.get();
		} catch (ExchangeException e) {
			LOG.error("Cannot initialize exchange cache", e);
		}

		if (rates == null) {
			return;
		}

		for (ExchangeRate rate : rates) {
			cache(rate);
		}
	}

	private ExchangeRate preload(String currency) throws ExchangeException {

		LOG.warn("Preloading {} to {} exchange rate", currency, base);

		Collection<ExchangeRate> rates = new ExchangeQuery()
			.from(Arrays.asList(currency))
			.to(base)
			.get();

		// check if null (no network connectivity)

		if (rates == null) {
			throw new ExchangeException("Cache was not able to communicate with the supported exchanges, this is most likely caused by the network connectivity issue");
		}

		// check if empty (currency not supported by the exchange)

		if (rates.isEmpty()) {
			throw new ExchangeException("The " + currency + " currency seems to be unsupported");
		}

		// get first and the only one

		ExchangeRate rate = rates.iterator().next();

		// put it into the cache

		cache(rate);

		// and return

		return rate;
	}

	public ExchangeRate getRate(String currency) throws ExchangeException {

		ExchangeRate rate = cache.get(currency);
		if (rate == null) {
			rate = preload(currency);
		}

		return rate;
	}

	public static void setParameter(String key, Object value) {
		PARAMETERS.put(key, value);
	}

	public static Object getParameter(String key) {
		return PARAMETERS.get(key);
	}
}
