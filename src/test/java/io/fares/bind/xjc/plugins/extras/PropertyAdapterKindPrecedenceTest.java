package io.fares.bind.xjc.plugins.extras;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class PropertyAdapterKindPrecedenceTest extends AbstractPluginTest {

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/PropertyAdapterKindPrecedence");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "test/propertyadapterprecedence/Book.java");
  }

  @Override
  protected TestValidator getValidator() {
    return new PropertyAdapterPrecedenceValidator();
  }

  @Override
  public List<String> getArgs() {
    final List<String> args = new ArrayList<>(super.getArgs());
    args.add("-Xxml-adapter");
    return args;
  }
}
