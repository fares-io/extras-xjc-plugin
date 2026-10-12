package io.fares.bind.xjc.plugins.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IncompatibleAttributeAdapterTest extends AbstractPluginTest {

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/IncompatibleAttributeAdapter");
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
    assertTrue(failure.getMessage().contains("cannot adapt"));
    assertTrue(failure.getMessage().contains("price"));
    assertTrue(failure.getMessage().contains("java.lang.String"));
    assertTrue(failure.getMessage().contains("test.complextypeadapter.Amount"));
  }
}
