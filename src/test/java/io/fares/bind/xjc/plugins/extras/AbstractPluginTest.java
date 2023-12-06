/*
 * Copyright 2019 Niels Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin;
import org.jvnet.jaxb.maven.AbstractXJC2Mojo;
import org.jvnet.jaxb.maven.test.RunXJC2Mojo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPluginTest extends RunXJC2Mojo {

  AbstractXJC2Mojo<AdapterPlugin> mojoUnderTest;

  @Override
  public void testExecute() throws Exception {

    super.testExecute();

    // get access to the directory the sources were generated into
    File genDir = mojoUnderTest.getGenerateDirectory();

    final File clazzFile = getClassFile(genDir);

    assertNotNull("class file was null", clazzFile);
    assertTrue("class file " + clazzFile.toString() + " does not exist", clazzFile.exists());
    assertTrue("class file " + clazzFile.toString() + " is no file", clazzFile.isFile());

    try {

      CompilationUnit unit = StaticJavaParser.parse(clazzFile);

      TestValidator validator = getValidator();
      validator.visit(unit, null);

      assertTrue("Could not validate", validator.isFound());

    } catch (FileNotFoundException e) {
      fail("not expecting parse failures");
    }

  }

  @Override
  protected void configureMojo(AbstractXJC2Mojo mojo) {
    super.configureMojo(mojo);
    mojo.setForceRegenerate(true);
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
    return new File(getBaseDir(), "target/generated-test-sources/xjc");
  }

  protected boolean isDebug() {
    return false;
  }

}
