# IMPORTANT NOTE!

Yahoo YQL API has been discontinued and it's not working any more!

# exchange-rates-api

Java API to access forex exchange rates via Yahoo YQL with fallback to OpenExchangeRates JSON. Response from these services is cached for the one hour, and then, request is re-executed to get the newest rates.

The following official ISO 4217 symbols are **not** supported:

* BOV (Bolivian Mvdol, funds code),
* CHE (WIR Euro, complementary currency),
* CHW (WIR Franc, complementary currency),
* COU (Unidad de Valor Real (UVR), funds code),
* SSP (South Sudanese pound)
* USN (United States dollar (next day), funds code),
* UYI (Uruguay Peso en Unidades Indexadas (URUIURUI), funds code),
* XBA (European Composite Unit (EURCO), bond market unit),
* XBB (European Monetary Unit (E.M.U.-6), bond market unit),
* XBC (European Unit of Account 9 (E.U.A.-9), bond market unit)
* XBD (European Unit of Account 17 (E.U.A.-17), bond market unit),
* XSU (SUCRE from Unified System for Regional Compensation),
* XTS (test currency),
* XUA (ADB Unit of Account from African Development Bank),
* XXX (no currency).


## Configuration

The [openexchangerates.org](https://openexchangerates.org/) is used as a fallback for the default Yahoo YQL API, so it will be used in most of the cases, but to make use of it when Yahoo fails, the following property must be set before it is launched:

```java
ExchangeCache.setParameter("openexchangerates.org.apikey", "apikey");
```

_(note to replace "apikey" with your own key API key obtained from openexchangerates.org)_


## Example

```java
import java.math.BigDecimal;

import com.github.sarxos.xchange.ExchangeCache;
import com.github.sarxos.xchange.ExchangeException;
import com.github.sarxos.xchange.ExchangeRate;


public class ConvertCadToUsd {

  public static void main(String[] args) throws ExchangeException, InterruptedException {

    // change apikey to your own one
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
```

## Maven

```xml
<dependency>
  <groupId>com.github.sarxos</groupId>
  <artifactId>exchange-rates-api</artifactId>
  <version>0.2</version>
</dependency>
```

## License

Please see [LICENSE.txt](https://github.com/sarxos/exchange-rates-api/blob/master/LICENSE.txt)
