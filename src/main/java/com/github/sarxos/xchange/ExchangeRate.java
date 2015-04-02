package com.github.sarxos.xchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


public class ExchangeRate {

	private final String from;
	private final String to;
	private final String symbol;
	private final String name;
	private final BigDecimal rate;

	public ExchangeRate(String forex, String rate) {
		this.to = forex.substring(0, 3);
		this.from = forex.substring(3, 6);
		this.symbol = from + to;
		this.name = from + "/" + to;
		this.rate = new BigDecimal("1.0").divide(new BigDecimal(rate), 8, RoundingMode.HALF_EVEN);
	}

	public BigDecimal convert(int value) {
		return rate.multiply(new BigDecimal(value));
	}

	public BigDecimal convert(BigDecimal value) {
		return rate.multiply(value);
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getRate() {
		return rate;
	}

	@Override
	public int hashCode() {
		return symbol.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return Objects.equals(getSymbol(), ((ExchangeRate) obj).getSymbol());
	}

	@Override
	public String toString() {
		return getName() + " " + getRate();
	}
}
