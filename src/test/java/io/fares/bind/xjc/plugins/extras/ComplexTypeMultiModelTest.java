/*
 * Copyright 2019 Niels Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fares.bind.xjc.plugins.extras;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComplexTypeMultiModelTest extends AbstractPluginTest {

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/ComplexTypeMultiModel");
  }

  @Override
  protected File getClassFile(File genDir) {
    return new File(genDir, "test/multimodel/Book.java");
  }

  @Override
  protected TestValidator getValidator() {
    return new ComplexTypeMultiModelValidator();
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
    File cassette = new File(mojoUnderTest.getGenerateDirectory(), "test/multimodel/Cassette.java");
    TestValidator validator = getValidator();
    validator.visit(StaticJavaParser.parse(cassette), null);
    assertTrue(validator.isFound(), "Validator must find the generated Cassette");
  }

}
