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
import com.github.javaparser.ast.CompilationUnit;
import com.sun.tools.xjc.Options;
import org.jvnet.jaxb.maven.AbstractXJCMojo;
import org.jvnet.jaxb.maven.test.RunXJCMojo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractPluginTest extends RunXJCMojo {

  AbstractXJCMojo<Options> mojoUnderTest;

  @Override
  @Test
  public void testExecute() throws Exception {

    super.testExecute();

    // get access to the directory the sources were generated into
    File genDir = mojoUnderTest.getGenerateDirectory();

    final File clazzFile = getClassFile(genDir);

    assertNotNull(clazzFile, "class file was null");
    assertTrue(clazzFile.exists(), "class file " + clazzFile + " does not exist");
    assertTrue(clazzFile.isFile(), "class file " + clazzFile + " is no file");

    try {

      CompilationUnit unit = StaticJavaParser.parse(clazzFile);

      TestValidator validator = getValidator();
      validator.visit(unit, null);

      assertTrue(validator.isFound(), "Could not validate");

    } catch (FileNotFoundException e) {
      throw new AssertionError("Could not parse generated source: " + clazzFile, e);
    }

  }

  @Override
  protected void configureMojo(AbstractXJCMojo<Options> mojo) {
    super.configureMojo(mojo);
    Path generated = mojo.getGenerateDirectory().toPath();
    if (Files.exists(generated)) {
      try (Stream<Path> paths = Files.walk(generated)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
          Files.delete(path);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Could not clean generated test sources: " + generated, e);
      }
    }
    mojo.setDebug(isDebug());
    this.mojoUnderTest = mojo;
  }

  @Override
  public List<String> getArgs() {
    final List<String> args = new ArrayList<>(super.getArgs());
    args.add("-extension");
    return args;
  }

  protected abstract File getClassFile(File genDir);

  protected abstract TestValidator getValidator();

  protected File getGeneratedDirectory() {
    return new File(getBaseDir(), "target/generated-test-sources/xjc/" + getClass().getSimpleName());
  }

  protected boolean isDebug() {
    return false;
  }

}
