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

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComplexTypeAdapterValidator extends TestValidator implements AdapterAssertions {

  private final String priceType;

  public ComplexTypeAdapterValidator(String priceType) {
    this.priceType = priceType;
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
    super.visit(declaration, arg);
    if (!"Book".equals(declaration.getNameAsString())) {
      return;
    }

    assertTrue(importsType(declaration, "javax.money.MonetaryAmount"), "Book must import MonetaryAmount");
    assertMappedField(declaration, "price", priceType, "AmountXmlAdapter");
    found = true;
  }
}
