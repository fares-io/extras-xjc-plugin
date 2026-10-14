package io.fares.bind.xjc.plugins.extras;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratedMoneyCurrencyCodeValidator extends TestValidator implements AdapterAssertions {

  @Override
  public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
    super.visit(declaration, arg);
    if (!"Book".equals(declaration.getNameAsString())) {
      return;
    }
    assertTrue(importsType(declaration, "javax.money.MonetaryAmount"),
      "Book must import javax.money.MonetaryAmount");
    assertMappedField(declaration, "price", "MonetaryAmount", "MoneyAmountXmlAdapter");
    found = true;
  }

  public void validateAdapter(Path directory) throws IOException {
    GeneratedMoneyAdapterValidator.assertAdapter(directory, "MoneyAmount");
    GeneratedMoneyAdapterValidator.assertDefaultConversion(directory.resolve("MoneyAmountXmlAdapter.java"), "Value", "CurrencyCode");
    ClassOrInterfaceDeclaration amount = GeneratedMoneyAdapterValidator.readClass(directory.resolve("MoneyAmount.java"), "MoneyAmount");
    assertEquals("String", amount.getMethodsByName("getCurrencyCode").get(0).getType().asString());
    assertEquals("BigDecimal", amount.getMethodsByName("getValue").get(0).getType().asString());
  }
}
