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
package io.fares.bind.xjc.plugins.extras.xmladapter;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.Model;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.jvnet.jaxb.plugin.AbstractParameterizablePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class AdapterPlugin extends AbstractParameterizablePlugin {

  private static final Logger log = LoggerFactory.getLogger(AdapterPlugin.class.getPackage().getName() + ".report");

  public static final String NS = "urn:jaxb.fares.io:extras";

  public static final String COMPLEX_XML_ADAPTER = "xml-adapter";

  public static final QName COMPLEX_XML_ADAPTER_NAME = new QName(NS, COMPLEX_XML_ADAPTER);

  @Override
  public String getOptionName() {
    return "Xxml-adapter";
  }

  @Override
  public String getUsage() {
    return "  -Xxml-adapter          : allow adding xml adapters to map complexType";
  }

  @Override
  public Collection<QName> getCustomizationElementNames() {
    return Collections.singletonList(COMPLEX_XML_ADAPTER_NAME);
  }

  @Override
  protected void init(Options options) throws Exception {

    // this is a dirty hack to prevent XJC from falling over when a customisation is attached in a schema that is
    // part of a prior compilation episode - https://github.com/eclipse-ee4j/jaxb-ri/issues/1320

    ByteBuddyAgent.install();

//  Does not work :(
//
//    final ElementMatcher.Junction<NamedElement> uselessCheckerTypeDef = named("com.sun.tools.xjc.reader.xmlschema.UnusedCustomizationChecker");
//
//    final AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, module) ->
//      builder.method(checkMethod).intercept(MethodDelegation.to(UselessCustomizationCheckerInterceptor.class));
//
//    AgentBuilder bldr = new AgentBuilder.Default()
//      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//      .type(uselessCheckerTypeDef)
//      .transform(transformer);
//
//    ResettableClassFileTransformer installOn = bldr.installOn(ByteBuddyAgent.getInstrumentation());

    // need to shuffle through component customizations

    new ByteBuddy()
      .redefine(CCustomizations.class)
      .visit(Advice.to(CCustomisationSetterInterceptor.class).on(ElementMatchers.named("setParent")))
      .visit(Advice.to(CCustomisationConstructorInterceptor.class).on(ElementMatchers.isConstructor()))
      .make()
      .load(getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
      .getLoaded();


    // and also ack BIXPluginCustomizations that are otherwise turned into errors by the useless checker junk

    final Class<?> uselessCheckerType = getClass().getClassLoader().loadClass("com.sun.tools.xjc.reader.xmlschema.UnusedCustomizationChecker");

    new ByteBuddy()
      .redefine(uselessCheckerType)
      .visit(Advice.to(UselessCustomizationCheckerInterceptor.class).on(named("check").and(takesArguments(2))))
      .make()
      .load(getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
      .getLoaded();

  }

  @Override
  public void postProcessModel(Model model, ErrorHandler errorHandler) {

    AdapterInspector inspector = new AdapterInspector();

    for (final CClassInfo classInfo : model.beans().values()) {
      classInfo.accept(inspector);
    }

    // report

    logReport("2020.3 scanning for XMLAdapter candidates");

    Map<String, Stack<AdapterCandidate>> types = inspector.getTypeCandidates();
    Map<String, Stack<AdapterCandidate>> fields = inspector.getFieldCandidates();

    for (Map.Entry<String, Stack<AdapterCandidate>> typeElement : types.entrySet()) {

      if (!typeElement.getValue().isEmpty()) {

        logReport("");
        logReport("XMLAdapter: {}", typeElement.getKey());
        logReport("");
        logReport("Registered Types:");

        for (AdapterCandidate candidate : typeElement.getValue()) {
          logReport(format("   [+]: %s", candidate.getName()));
        }

      }

      if (fields.containsKey(typeElement.getKey())) {

        logReport("");
        logReport(" Field Candidates:");

        for (AdapterCandidate candidate : fields.get(typeElement.getKey())) {
          logReport("   [+]: {}", candidate.getName());
        }

        logReport("   {} candidate(s) being considered", fields.get(typeElement.getKey()).size());

      }

    }

    for (Map.Entry<String, Stack<AdapterCandidate>> entry : fields.entrySet()) {

      if (!types.containsKey(entry.getKey()) && !entry.getValue().isEmpty()) {

        logReport("");
        logReport("XMLAdapter: {}", entry.getKey());
        logReport("");
        logReport("Field Candidates:");

        for (AdapterCandidate candidate : fields.get(entry.getKey())) {
          logReport("   [+]: {}", candidate.getName());
        }

        logReport("   {} candidate(s) being considered", fields.get(entry.getKey()).size());

      }

    }

    logReport("");

    logReport("XMLAdapter modifications:");
    for (final CClassInfo classInfo : model.beans().values()) {
      postProcessClassInfo(model, inspector, classInfo);
    }

    logReport("");

  }

  private void postProcessClassInfo(final Model model, AdapterInspector inspector, final CClassInfo classInfo) {

    for (CPropertyInfo property : classInfo.getProperties()) {
      property.accept(new AdapterCustomizer(model, inspector));
    }

  }

  public static void logReport(String format, Object... arguments) {

    if (log.isInfoEnabled()) {
      log.info(format, arguments);
    }
  }

  public static void logError(String format, Object... arguments) {
    log.error(format, arguments);
  }

}


