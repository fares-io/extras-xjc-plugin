package io.fares.bind.xjc.plugins.extras;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedMoneyElementsTest extends AbstractPluginTest {

  private final GeneratedMoneyCurrencyCodeValidator validator = new GeneratedMoneyCurrencyCodeValidator();

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/GeneratedMoneyElements");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "test/moneyelements/Book.java");
  }

  @Override
  protected TestValidator getValidator() {
    return validator;
  }

  @Override
  public List<String> getArgs() {
    List<String> args = new ArrayList<>(super.getArgs());
    args.add("-Xxml-adapter");
    return args;
  }

  @Override
  @Test
  public void testExecute() throws Exception {
    super.testExecute();
    Path directory = mojoUnderTest.getGenerateDirectory().toPath().resolve("test/moneyelements");
    validator.validateAdapter(directory);
    var amount = GeneratedMoneyAdapterValidator.readClass(directory.resolve("MoneyAmount.java"), "MoneyAmount");
    for (String name : List.of("currencyCode", "value")) {
      var field = amount.getFieldByName(name).orElseThrow(() -> new AssertionError("Missing XML field: " + name));
      assertTrue(field.isAnnotationPresent(XmlElement.class), name + " must remain an XML element");
      assertFalse(field.isAnnotationPresent(XmlValue.class), name + " must not become simple content");
    }
    String documentation = amount.getJavadocComment()
      .orElseThrow(() -> new AssertionError("Missing XML placeholder documentation")).getContent();
    assertTrue(documentation.contains("Placeholder XML type for {@link test.moneyelements.MoneyAmountXmlAdapter}"));
    assertTrue(documentation.contains("Application code should use {@link javax.money.MonetaryAmount}"));
  }
}
