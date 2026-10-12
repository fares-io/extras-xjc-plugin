package io.fares.bind.xjc.plugins.extras;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComplexTypeMultiModelValidator extends TestValidator implements AdapterAssertions {

  @Override
  public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
    super.visit(declaration, arg);

    switch (declaration.getNameAsString()) {
      case "Book":
        Stream.of("isbn", "title", "author").forEach(name -> assertStringField(declaration, name));
        break;
      case "Cassette":
        assertStringField(declaration, "play");
        break;
      default:
        return;
    }

    assertStringField(declaration, "id");
    assertEquals("Languages", fieldType(declaration, "language"));
    assertEquals("Languages", getterType(declaration, "language"));
    found = true;
  }

  private void assertStringField(ClassOrInterfaceDeclaration declaration, String name) {
    assertEquals("String", fieldType(declaration, name), "Field type for " + name);
    assertEquals("String", getterType(declaration, name), "Getter type for " + name);
  }
}
