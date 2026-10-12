package io.fares.bind.xjc.plugins.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GeneratedMoneyCurrencyCodeTest extends AbstractPluginTest {

  private final GeneratedMoneyCurrencyCodeValidator validator = new GeneratedMoneyCurrencyCodeValidator();

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/GeneratedMoneyCurrencyCode");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "test/moneycurrencycode/Book.java");
  }

  @Override
  protected TestValidator getValidator() {
    return validator;
  }

  @Override
  public List<String> getArgs() {
    final List<String> args = new ArrayList<>(super.getArgs());
    args.add("-Xxml-adapter");
    return args;
  }

  @Override
  @Test
  public void testExecute() throws Exception {
    super.testExecute();
    validator.validateAdapter(mojoUnderTest.getGenerateDirectory().toPath().resolve("test/moneycurrencycode"));
  }
}
