package io.fares.bind.xjc.plugins.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeneratedMoneyAdapterTest extends AbstractPluginTest {

  private final GeneratedMoneyAdapterValidator validator = new GeneratedMoneyAdapterValidator();

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/GeneratedMoneyAdapter");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "test/generatedmoney/Book.java");
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
    validator.validateAdapters(mojoUnderTest.getGenerateDirectory().toPath().resolve("test/generatedmoney"));
  }

  @Test
  public void testDefaultConversionAdapterNullGuards() throws Exception {
    super.testExecute();
    GeneratedMoneyAdapterValidator.assertNullGuards(
      mojoUnderTest.getGenerateDirectory().toPath().resolve("test/generatedmoney"), "PrimaryAmount");
  }

  @Test
  public void testConfiguredConverterAdapterNullGuards() throws Exception {
    super.testExecute();
    GeneratedMoneyAdapterValidator.assertNullGuards(
      mojoUnderTest.getGenerateDirectory().toPath().resolve("test/generatedmoney"), "SecondaryAmount");
  }
}
