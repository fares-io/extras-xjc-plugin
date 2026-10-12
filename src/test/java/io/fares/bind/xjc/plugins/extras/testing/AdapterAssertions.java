package io.fares.bind.xjc.plugins.extras.testing;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.type.Type;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface AdapterAssertions {

  default void assertMappedField(ClassOrInterfaceDeclaration declaration, String name, String type, String adapter) {
    FieldDeclaration field = declaration.getFieldByName(name)
      .orElseThrow(() -> new AssertionError("Missing mapped field: " + name));

    assertAll("Mapped property " + name,
      () -> assertEquals(type, field.getVariable(0).getType().asString(), "Field type"),
      () -> assertEquals(type, getterType(declaration, name), "Getter type"),
      () -> assertEquals(adapter, adapterType(field), "XML adapter"));
  }

  default String fieldType(ClassOrInterfaceDeclaration declaration, String name) {
    return declaration.getFieldByName(name)
      .map(field -> field.getVariable(0).getType())
      .map(Type::asString)
      .orElseThrow(() -> new AssertionError("Missing field: " + name));
  }

  default String getterType(ClassOrInterfaceDeclaration declaration, String property) {
    String name = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return declaration.getMethodsByName(name).stream()
      .map(MethodDeclaration::getType)
      .map(Type::asString)
      .findFirst()
      .orElseThrow(() -> new AssertionError("Missing getter: " + name));
  }

  default String adapterType(FieldDeclaration field) {
    return field.getAnnotationByClass(XmlJavaTypeAdapter.class)
      .flatMap(annotation -> annotation.findFirst(ClassExpr.class))
      .map(ClassExpr::getType)
      .map(Type::asString)
      .orElseThrow(() -> new AssertionError("Missing XML adapter class on " + field.getVariable(0).getNameAsString()));
  }

  default boolean importsType(ClassOrInterfaceDeclaration declaration, String name) {
    return declaration.findCompilationUnit()
      .map(unit -> unit.getImports().stream().anyMatch(imported -> name.equals(imported.getNameAsString())))
      .orElse(false);
  }
}
