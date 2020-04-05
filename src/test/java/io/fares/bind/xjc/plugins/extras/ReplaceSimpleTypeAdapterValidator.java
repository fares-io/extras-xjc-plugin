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

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ReplaceSimpleTypeAdapterValidator extends TestValidator {

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    super.visit(n, arg);

    if ("ExtendElement".equals(n.getName().asString())) {
      validateFieldName(n, "value");
    } else if ("Book".equals(n.getName().asString())) {
      validateFieldName(n, "overrideElement");
      validateFieldName(n, "overrideAttribute");
      validateFieldName(n, "refAttribute1");
      validateFieldName(n, "refAttribute2");
      validateFieldName(n, "groupAttribute1");
    }

    found = true;

  }

  private void validateFieldName(ClassOrInterfaceDeclaration n, String name) {

    FieldDeclaration fieldDeclaration = n.getFieldByName(name)
      .orElseThrow(() -> new IllegalArgumentException("'field " + name + " was not found in book"));

    fieldDeclaration.getVariables().parallelStream()
      .map(VariableDeclarator::getType)
      .map(Type::asString)
      .filter("LocalDateTime"::equals)
      .findAny()
      .orElseThrow(() -> new IllegalArgumentException("field " + name + " is not a LocalDateTime type"));

    AnnotationExpr xmlAdapterAnnotation = fieldDeclaration.getAnnotationByClass(XmlJavaTypeAdapter.class)
      .orElseThrow(() -> new IllegalArgumentException("'XmlJavaTypeAdapter wad not added to field " + name + " in book"));

    xmlAdapterAnnotation.findAll(Expression.class)
      .parallelStream()
      .filter(e -> e instanceof SingleMemberAnnotationExpr)
      .map(SingleMemberAnnotationExpr.class::cast)
      .map(SingleMemberAnnotationExpr::getMemberValue)
      .filter(e -> e instanceof ClassExpr)
      .map(ClassExpr.class::cast)
      .map(ClassExpr::getType)
      .map(Type::asString)
      .filter("LocalDateTimeAdapter"::equals)
      .findFirst().orElseThrow(() -> new IllegalArgumentException("field " + name + " does not have a LocalDateTimeAdapter"));
  }

}
