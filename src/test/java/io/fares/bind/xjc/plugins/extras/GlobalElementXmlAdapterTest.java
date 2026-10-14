package io.fares.bind.xjc.plugins.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalElementXmlAdapterTest extends AbstractPluginTest {

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/GlobalElementXmlAdapter");
  }

  @Override
  protected File getClassFile(File genDir) {
    return null;
  }

  @Override
  protected TestValidator getValidator() {
    return null;
  }

  @Override
  public List<String> getArgs() {
    final List<String> args = new ArrayList<>(super.getArgs());
    args.add("-Xxml-adapter");
    return args;
  }

  @Override
  @Test
  public void testExecute() {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, super::testExecute);
    assertNotNull(failure.getMessage(), "Generation failure must include a diagnostic");
    assertTrue(failure.getMessage().contains("Unsupported extras:xml-adapter"),
      "Unexpected generation diagnostic: " + failure.getMessage());
    assertTrue(failure.getMessage().contains("Price"));
    assertTrue(failure.getMessage().contains("schema.xsd"));
  }
}
