package test.generatedmoney;

import java.math.BigDecimal;
import java.math.MathContext;
import javax.money.MonetaryAmount;
import javax.money.MonetaryContextBuilder;
import org.javamoney.moneta.Money;

public final class MoneyConversions {
  private MoneyConversions() {
  }

  public static MonetaryAmount of(BigDecimal value, String currency) {
    return Money.of(value, currency, MonetaryContextBuilder.of(Money.class).set(MathContext.UNLIMITED).build());
  }

  public static BigDecimal value(MonetaryAmount money) {
    return money.getNumber().numberValueExact(BigDecimal.class);
  }

  public static String currency(MonetaryAmount money) {
    return money.getCurrency().getCurrencyCode();
  }
}
