import java.math.BigDecimal;

import com.github.sarxos.xchange.ExchangeCache;
import com.github.sarxos.xchange.ExchangeException;
import com.github.sarxos.xchange.ExchangeRate;


public class ConvertCadToUsd {

	public static void main(String[] args) throws ExchangeException, InterruptedException {

		// change apikey to your own value
		ExchangeCache.setParameter("openexchangerates.org.apikey", "apikey");

		// define base currency
		ExchangeCache cache = new ExchangeCache("USD");

		// get the CAD to USD exchange rate
		ExchangeRate rate = cache.getRate("CAD");

		// convert
		BigDecimal amount = new BigDecimal("1000");
		BigDecimal converted = rate.convert(amount);

		System.out.println("The " + amount + " CAD = " + converted + " in USD");
	}
}
