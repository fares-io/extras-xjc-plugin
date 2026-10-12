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
package io.fares.bind.xjc.plugins.extras.xmladapter;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.Model;
import io.fares.bind.xjc.plugins.extras.Utils;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * XJC plugin that applies {@code extras:xml-adapter} customizations.
 */
public class AdapterPlugin extends AbstractParameterizablePlugin {

  private static final Logger log = LoggerFactory.getLogger(AdapterPlugin.class.getPackage().getName() + ".report");

  /** Creates an XML adapter plugin. */
  public AdapterPlugin() {
  }

  /**
   * Namespace for extras customizations.
   */
  public static final String NS = "urn:jaxb.fares.io:extras";

  /**
   * Local name of the XML adapter customization.
   */
  public static final String COMPLEX_XML_ADAPTER = "xml-adapter";

  /**
   * Qualified name of the XML adapter customization.
   */
  public static final QName COMPLEX_XML_ADAPTER_NAME = new QName(NS, COMPLEX_XML_ADAPTER);

  /**
   * Local name of the JavaMoney adapter customisation.
   *
   * <p>
   * {@code currencyProperty} defaults to {@code currency}. The optional
   * {@code valueProperty} selects the decimal property by its generated Java name.
   * Without it, the decimal property is selected automatically. Both properties
   * must belong to the amount type and must differ.
   *
   * <pre>{@code
   * <extras:java-money-adapter currencyProperty="currencyCode" valueProperty="amount"/>
   * }</pre>
   */
  public static final String JAVA_MONEY_ADAPTER = "java-money-adapter";

  /**
   * Qualified name of the Java money adapter customization.
   */
  public static final QName JAVA_MONEY_ADAPTER_NAME = new QName(NS, JAVA_MONEY_ADAPTER);

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
    return Arrays.asList(COMPLEX_XML_ADAPTER_NAME, JAVA_MONEY_ADAPTER_NAME);
  }

  /**
   * Checks whether a name identifies an adapter customisation supported by this plugin.
   *
   * @param name the customisation element name
   * @return true for XML adapter or JavaMoney adapter customisations
   */
  public static boolean isSupportedCustomizationName(QName name) {
    return COMPLEX_XML_ADAPTER_NAME.equals(name) || JAVA_MONEY_ADAPTER_NAME.equals(name);
  }

  /**
   * Checks whether a customization belongs to this plugin.
   *
   * @param customization customization to inspect
   * @return {@code true} when the customization is an XML adapter or JavaMoney adapter customization
   */
  public static boolean isSupportedCustomization(CPluginCustomization customization) {
    return Utils.matchesCustomization(customization, COMPLEX_XML_ADAPTER_NAME)
      || Utils.matchesCustomization(customization, JAVA_MONEY_ADAPTER_NAME);
  }

  @Override
  protected void init(Options options) throws Exception {

    // XJC rejects unused customizations from prior episodes without these hooks.
    // https://github.com/eclipse-ee4j/jaxb-ri/issues/1320

    ByteBuddyAgent.install();

    // Acknowledge customizations before XJC assigns their owners.

    new ByteBuddy()
      .redefine(CCustomizations.class)
      .visit(Advice.to(CCustomisationSetterInterceptor.class).on(ElementMatchers.named("setParent")))
      .visit(Advice.to(CCustomisationConstructorInterceptor.class).on(ElementMatchers.isConstructor()))
      .make()
      .load(getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
      .getLoaded();

    // Prior episode customizations must also pass the schema customization check.

    final Class<?> uselessCheckerType = getClass().getClassLoader().loadClass("com.sun.tools.xjc.reader.xmlschema.UnusedCustomizationChecker");

    new ByteBuddy()
      .redefine(uselessCheckerType)
      .visit(Advice.to(UselessCustomizationCheckerInterceptor.class).on(named("check").and(takesArguments(2))))
      .make()
      .load(getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
      .getLoaded();

  }

  @Override
  @SuppressWarnings({"rawtypes"})
  public void postProcessModel(Model model, ErrorHandler errorHandler) {

    AdapterInspector inspector = new AdapterInspector();
    AmountAdapterGenerator amountAdapterGenerator = new AmountAdapterGenerator(model);

    for (final CClassInfo classInfo : model.beans().values()) {
      classInfo.accept(inspector);
    }

    for (CElementInfo element : model.getAllElements()) {
      inspector.onElement(element);
    }

    reportCandidates(inspector);

    AdapterCustomizer customizer = new AdapterCustomizer(amountAdapterGenerator, inspector);
    logReport("XMLAdapter modifications:");
    for (final CClassInfo classInfo : model.beans().values()) {
      postProcessClassInfo(customizer, classInfo);
    }

    logReport("");

  }

  private void reportCandidates(AdapterInspector inspector) {
    logReport("Scanning for XMLAdapter candidates");
    Map<String, Stack<AdapterCandidate>> types = inspector.getTypeCandidates();
    Map<String, Stack<AdapterCandidate>> fields = inspector.getFieldCandidates();

    for (Map.Entry<String, Stack<AdapterCandidate>> entry : types.entrySet()) {
      logReport("XMLAdapter: {}", entry.getKey());
      reportCandidates("Registered Types:", entry.getValue());
      if (fields.containsKey(entry.getKey())) {
        reportCandidates("Field Candidates:", fields.get(entry.getKey()));
      }
    }

    for (Map.Entry<String, Stack<AdapterCandidate>> entry : fields.entrySet()) {
      if (!types.containsKey(entry.getKey())) {
        logReport("XMLAdapter: {}", entry.getKey());
        reportCandidates("Field Candidates:", entry.getValue());
      }
    }
    logReport("");
  }

  private void reportCandidates(String heading, Collection<AdapterCandidate> candidates) {
    if (candidates.isEmpty()) {
      return;
    }
    logReport(heading);
    for (AdapterCandidate candidate : candidates) {
      logReport("   [+]: {}", candidate.getName());
    }
    logReport("   {} candidate(s) being considered", candidates.size());
  }

  private void postProcessClassInfo(AdapterCustomizer customizer, CClassInfo classInfo) {

    for (CPropertyInfo property : classInfo.getProperties()) {
      property.accept(customizer);
    }

  }

  /**
   * Writes an informational XML adapter report message.
   *
   * @param format    the SLF4J message format
   * @param arguments values for the message format
   */
  public static void logReport(String format, Object... arguments) {

    if (log.isInfoEnabled()) {
      log.info(format, arguments);
    }
  }

  /**
   * Writes an XML adapter error message.
   *
   * @param format    the SLF4J message format
   * @param arguments values for the message format
   */
  public static void logError(String format, Object... arguments) {
    log.error(format, arguments);
  }

}
