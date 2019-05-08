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
import io.fares.bind.xjc.plugins.extras.testing.JaxbMojoExension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test validates that we can still generate code when the customization is attached to a prior episode but
 * the type it is attached to is never referenced.
 */
class ComplexTypeAdapterUnreferencedTest {

  @RegisterExtension
  static JaxbMojoExension MOJO = JaxbMojoExension.builder()
    .verbose()
    .arg("-Xxml-adapter")
    .build();

  @Test
  void shouldProduceBook() throws Exception {
    CompilationUnit book = StaticJavaParser.parse(MOJO.getGeneratedPath().resolve("test/complextypeadapter/Book.java"));
    assertNotNull(book, "expecting a book");
  }

  @Test
  void shouldNotProduceAmount() {
    Path amount = MOJO.getGeneratedPath().resolve("test/core/Amount.java");
    assertFalse(amount.toFile().exists(), "should not have generated a prior episode binding");
  }

}
