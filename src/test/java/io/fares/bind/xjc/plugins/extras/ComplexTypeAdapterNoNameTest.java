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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexTypeAdapterNoNameTest extends AbstractPluginTest {

  @Override
  @Test
  public void testExecute() {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, super::testExecute);
    assertNotNull(failure.getMessage(), "Generation failure must include a diagnostic");
    assertTrue(failure.getMessage().contains("must specify the XML adapter class with the name attribute"),
      "Unexpected generation diagnostic: " + failure.getMessage());
  }

  @Override
  public File getSchemaDirectory() {
    return new File(getBaseDir(), "src/test/resources/schemas/ComplexTypeAdapterNoName");
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

}
