package io.fares.bind.xjc.plugins.extras;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;

public class PropertyAdapterPrecedenceValidator extends TestValidator implements AdapterAssertions {

  @Override
  public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
    super.visit(declaration, arg);
    if (!"Book".equals(declaration.getNameAsString())) {
      return;
    }
    assertMappedField(
      declaration, "price", "MonetaryAmount", "PropertyAmountXmlAdapter");
    found = true;
  }
}
