package io.fares.bind.xjc.plugins.extras;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.fares.bind.xjc.plugins.extras.testing.AdapterAssertions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratedMoneyAdapterValidator extends TestValidator implements AdapterAssertions {

  @Override
  public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
    super.visit(declaration, arg);
    if (!"Book".equals(declaration.getNameAsString())) {
      return;
    }

    assertTrue(importsType(declaration, "javax.money.MonetaryAmount"), "Book must import javax.money.MonetaryAmount");
    assertMappedField(declaration, "price", "MonetaryAmount", "PrimaryAmountXmlAdapter");
    assertMappedField(declaration, "fee", "MonetaryAmount", "SecondaryAmountXmlAdapter");
    assertMappedField(declaration, "extras", "List<MonetaryAmount>", "PrimaryAmountXmlAdapter");
    assertMappedField(declaration, "child", "MonetaryAmount", "TertiaryAmountXmlAdapter");
    found = true;
  }

  public void validateAdapters(Path directory) throws IOException {
    assertAdapter(directory, "PrimaryAmount");
    assertAdapter(directory, "SecondaryAmount");
    assertAdapter(directory, "TertiaryAmount");

    assertDefaultConversion(directory.resolve("PrimaryAmountXmlAdapter.java"), "Value", "Currency");
    assertConverterConversion(directory.resolve("SecondaryAmountXmlAdapter.java"), "MoneyConversions", "Value", "CurrencyCode");
    assertDefaultConversion(directory.resolve("TertiaryAmountXmlAdapter.java"), "Amount", "Currency");
  }

  static void assertAdapter(Path directory, String xmlType) throws IOException {
    ClassOrInterfaceDeclaration adapter = readClass(directory.resolve(xmlType + "XmlAdapter.java"), xmlType + "XmlAdapter");
    ClassOrInterfaceType base = adapter.getExtendedTypes().get(0);
    assertEquals("XmlAdapter", base.getNameAsString());
    assertEquals(2, base.getTypeArguments().get().size());
    assertEquals(xmlType, base.getTypeArguments().get().get(0).asString());
    assertEquals("MonetaryAmount", base.getTypeArguments().get().get(1).asString());

    MethodDeclaration unmarshal = adapter.getMethodsByName("unmarshal").get(0);
    assertEquals("MonetaryAmount", unmarshal.getType().asString());
    assertEquals(xmlType, unmarshal.getParameter(0).getType().asString());
    assertNullGuard(unmarshal, "amount");

    MethodDeclaration marshal = adapter.getMethodsByName("marshal").get(0);
    assertEquals(xmlType, marshal.getType().asString());
    assertEquals("MonetaryAmount", marshal.getParameter(0).getType().asString());
    assertNullGuard(marshal, "money");
    assertExpression("amount", returnExpression(marshal));

    VariableDeclarator output = marshal.findAll(VariableDeclarator.class).stream()
      .filter(variable -> "amount".equals(variable.getNameAsString()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Missing XML amount allocation"));
    assertEquals(xmlType, output.getType().asString());
    assertExpression("new " + xmlType + "()", output.getInitializer().get());
  }

  static void assertDefaultConversion(Path source, String value, String currency) throws IOException {
    Expression returned = returnExpression(readMethod(source, "unmarshal"));
    assertEquals("create", returned.asMethodCallExpr().getNameAsString());
    assertArguments(call(returned, "create"));
    assertExpression("Monetary", call(returned, "getDefaultAmountFactory").getScope().get());
    assertArguments(call(returned, "getDefaultAmountFactory"));
    assertArguments(call(returned, "setNumber"), "amount.get" + value + "()");
    assertArguments(call(returned, "setCurrency"), "amount.get" + currency + "()");
    assertArguments(call(returned, "setContext"),
      "MonetaryContextBuilder.of().set(MathContext.UNLIMITED).build()");

    MethodDeclaration marshal = readMethod(source, "marshal");
    assertArguments(setter(marshal, "set" + value), "money.getNumber().numberValueExact(BigDecimal.class)");
    assertArguments(setter(marshal, "set" + currency), "money.getCurrency().getCurrencyCode()");
  }

  static void assertConverterConversion(Path source, String converter, String value, String currency) throws IOException {
    MethodCallExpr returned = returnExpression(readMethod(source, "unmarshal")).asMethodCallExpr();
    assertEquals("of", returned.getNameAsString());
    assertExpression(converter, returned.getScope().get());
    assertArguments(returned, "amount.get" + value + "()", "amount.get" + currency + "()");

    MethodDeclaration marshal = readMethod(source, "marshal");
    assertArguments(setter(marshal, "set" + value), converter + ".value(money)");
    assertArguments(setter(marshal, "set" + currency), converter + ".currency(money)");
  }

  static ClassOrInterfaceDeclaration readClass(Path source, String name) throws IOException {
    return StaticJavaParser.parse(source).getClassByName(name)
      .orElseThrow(() -> new AssertionError("Missing generated class: " + name));
  }

  private static MethodDeclaration readMethod(Path source, String name) throws IOException {
    return StaticJavaParser.parse(source).findAll(MethodDeclaration.class).stream()
      .filter(method -> name.equals(method.getNameAsString()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Missing generated method: " + name));
  }

  private static void assertNullGuard(MethodDeclaration method, String parameter) {
    IfStmt guard = method.getBody().get().getStatements().stream()
      .findFirst()
      .flatMap(Statement::toIfStmt)
      .orElseThrow(() -> new AssertionError("Missing null guard in " + method.getNameAsString()));
    assertExpression(parameter + " == null", guard.getCondition());
    NodeList<Statement> statements = guard.getThenStmt().asBlockStmt().getStatements();
    assertEquals(1, statements.size(), "Null guard must return immediately");
    assertExpression("null", statements.get(0).asReturnStmt().getExpression().get());
  }

  private static Expression returnExpression(MethodDeclaration method) {
    return method.getBody()
      .flatMap(body -> body.getStatements().stream().reduce((earlier, later) -> later))
      .flatMap(Statement::toReturnStmt)
      .flatMap(ReturnStmt::getExpression)
      .orElseThrow(() -> new AssertionError("Missing final return in " + method.getNameAsString()));
  }

  private static MethodCallExpr call(Expression expression, String name) {
    return expression.findAll(MethodCallExpr.class).stream()
      .filter(invocation -> name.equals(invocation.getNameAsString()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Missing " + name + " in returned conversion"));
  }

  private static MethodCallExpr setter(MethodDeclaration method, String name) {
    return method.getBody().get().getStatements().stream()
      .filter(Statement::isExpressionStmt)
      .map(Statement::asExpressionStmt)
      .map(ExpressionStmt::getExpression)
      .filter(Expression::isMethodCallExpr)
      .map(Expression::asMethodCallExpr)
      .filter(invocation -> name.equals(invocation.getNameAsString()))
      .filter(invocation -> invocation.getScope().map(scope -> "amount".equals(scope.toString())).orElse(false))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Missing direct XML amount setter: " + name));
  }

  private static void assertArguments(MethodCallExpr invocation, String... arguments) {
    List<Expression> expected = Arrays.stream(arguments)
      .map(StaticJavaParser::<Expression>parseExpression)
      .collect(Collectors.toList());
    List<Expression> actual = invocation.getArguments().stream().collect(Collectors.toList());
    assertEquals(expected, actual, "Arguments to " + invocation.getNameAsString());
  }

  private static void assertExpression(String expected, Expression actual) {
    assertEquals(StaticJavaParser.parseExpression(expected), actual);
  }
}
