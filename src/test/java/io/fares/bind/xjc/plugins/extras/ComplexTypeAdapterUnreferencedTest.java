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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;
import io.fares.bind.xjc.plugins.extras.testing.JaxbMojoExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test validates that we can still generate code when the customization is attached to a prior episode but
 * the type it is attached to is never referenced.
 */
class ComplexTypeAdapterUnreferencedTest implements AdapterAssertions {

  @Order(1)
  @RegisterExtension
  static JaxbMojoExtension CORE = JaxbMojoExtension.builder()
    .generatedLocation("target/generated-test-sources/core")
    .arg("-Xxml-adapter")
    .build(mojo -> mojo.setSchemaIncludes(new String[]{"core.xsd"}));

  @Order(2)
  @RegisterExtension
  static JaxbMojoExtension MOJO = JaxbMojoExtension.builder()
    .arg("-Xxml-adapter", "-b",
      "target/generated-test-sources/core/ComplexTypeAdapterUnreferencedTest/META-INF/sun-jaxb.episode")
    .build(mojo -> mojo.setSchemaIncludes(new String[]{"schema.xsd"}));

  @Test
  void shouldGenerateCoreEpisode() throws Exception {
    Path directory = CORE.getGeneratedPath();
    assertTrue(Files.isRegularFile(directory.resolve("META-INF/sun-jaxb.episode")));
    ClassOrInterfaceDeclaration amount = StaticJavaParser.parse(directory.resolve("test/core/Amount.java"))
      .getClassByName("Amount")
      .orElseThrow(() -> new AssertionError("Missing generated core Amount"));
    assertEquals("BigDecimal", fieldType(amount, "value"));
    assertEquals("String", fieldType(amount, "currency"));
  }

  @Test
  void shouldProduceBook() throws Exception {
    CompilationUnit book = StaticJavaParser.parse(MOJO.getGeneratedPath().resolve("test/complextypeadapter/Book.java"));
    ClassOrInterfaceDeclaration declaration = book.getClassByName("Book")
      .orElseThrow(() -> new AssertionError("Missing generated Book"));
    assertEquals("String", fieldType(declaration, "isbn"));
    assertEquals("String", fieldType(declaration, "special"));
  }

  @Test
  void shouldNotProduceAmount() {
    Path amount = MOJO.getGeneratedPath().resolve("test/core/Amount.java");
    assertFalse(amount.toFile().exists(), "should not have generated a prior episode binding");
  }

}
