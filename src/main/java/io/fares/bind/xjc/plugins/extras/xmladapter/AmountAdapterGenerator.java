package io.fares.bind.xjc.plugins.extras.xmladapter;

import com.sun.codemodel.*;
import com.sun.tools.xjc.model.*;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.w3c.dom.Element;

import javax.lang.model.SourceVersion;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Generates a JavaMoney adapter for an XML decimal value and currency.
 *
 * <p>
 * {@code currencyProperty} selects a required string property and defaults to
 * {@code currency}. The optional {@code valueProperty} selects a decimal property
 * by its generated Java name. When omitted, the other property is used.
 */
final class AmountAdapterGenerator {

  private static final String BOUND_TYPE = "javax.money.MonetaryAmount";

  private final Model model;

  private final Map<String, AdapterConfiguration> generatedAdapters = new HashMap<>();

  AmountAdapterGenerator(Model model) {
    this.model = model;
  }

  private static AdapterConfiguration configuration(CPropertyInfo property, Element settings) {
    CClassInfo amount = amountType(property);
    CPropertyInfo currency = currencyProperty(amount, settings);
    CPropertyInfo value = decimalProperty(amount, currency, settings);

    return new AdapterConfiguration(amount.fullName(), converter(settings, amount), value, currency);
  }

  private static Optional<String> converter(Element settings, CClassInfo amount) {
    return Optional.of(attribute(settings, "converter", ""))
      .filter(name -> !name.isEmpty())
      .map(name -> {
        if (!SourceVersion.isName(name)) {
          throw new IllegalArgumentException(
            "Java money adapter converter must be a valid class name: " + name + " on " + amount.fullName());
        }
        return name;
      });
  }

  private static CClassInfo amountType(CPropertyInfo property) {
    if (!(property instanceof CElementPropertyInfo) || property.ref().size() != 1) {
      throw new IllegalArgumentException("Generated amount adapters require one element type: " + property.displayName());
    }
    CClassInfo amount = property.ref().stream()
      .filter(CClassInfo.class::isInstance)
      .map(CClassInfo.class::cast)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(
        "Generated amount adapters require a schema defined complex type: " + property.displayName()));
    if (amount.getBaseClass() != null || amount.getRefBaseClass() != null || amount.hasAttributeWildcard()) {
      throw new IllegalArgumentException("Amount type must not inherit fields or accept extra attributes: " + amount.fullName());
    }

    if (amount.getProperties().size() != 2) {
      throw new IllegalArgumentException("Amount type must contain only a decimal value and currency: " + amount.fullName());
    }
    return amount;
  }

  private static CPropertyInfo currencyProperty(CClassInfo amount, Element settings) {
    return Optional.ofNullable(amount.getProperty(attribute(settings, "currencyProperty", "currency")))
      .filter(AmountAdapterGenerator::isRequiredString)
      .orElseThrow(() -> new IllegalArgumentException(
        "Amount currency must be a required string attribute or element: " + amount.fullName()));
  }

  private static CPropertyInfo decimalProperty(CClassInfo amount, CPropertyInfo currency, Element settings) {
    String name = attribute(settings, "valueProperty", "");
    Optional<CPropertyInfo> selected = name.isEmpty()
      ? amount.getProperties().stream().filter(property -> property != currency).findFirst()
      : Optional.ofNullable(amount.getProperty(name));
    CPropertyInfo value = selected.orElseThrow(() -> new IllegalArgumentException(
      "Amount value property not found: " + name + " in " + amount.fullName()));
    if (value == currency) {
      throw new IllegalArgumentException("Amount value property must differ from currency property: " + amount.fullName());
    }

    if (!isScalarType(value, "java.math.BigDecimal")) {
      throw new IllegalArgumentException("Amount value must be a decimal: " + amount.fullName());
    }

    if (!(value instanceof CValuePropertyInfo) && !(value instanceof CElementPropertyInfo)) {
      throw new IllegalArgumentException("Amount value must be element content or a decimal element: " + amount.fullName());
    }

    if (value instanceof CElementPropertyInfo && !((CElementPropertyInfo) value).isRequired()) {
      throw new IllegalArgumentException("Amount decimal element must be required: " + amount.fullName());
    }
    return value;
  }

  private static boolean isScalarType(CPropertyInfo property, String type) {
    return property != null && !property.isCollection() && property.ref().size() == 1
      && property.ref().stream().map(reference -> reference.getType().fullName()).anyMatch(type::equals);
  }

  private static boolean isRequiredString(CPropertyInfo property) {
    if (!isScalarType(property, "java.lang.String")) {
      return false;
    }

    if (property instanceof CAttributePropertyInfo) {
      return ((CAttributePropertyInfo) property).isRequired();
    }
    return property instanceof CElementPropertyInfo && ((CElementPropertyInfo) property).isRequired();
  }

  private static String attribute(Element settings, String name, String fallback) {
    return Optional.of(settings.getAttribute(name).trim()).filter(value -> !value.isEmpty()).orElse(fallback);
  }

  private static JExpression readAmount(JCodeModel code, JVar input, AdapterConfiguration configuration) {
    return configuration.converter.<JExpression>map(converter -> code.ref(converter)
        .staticInvoke("of")
        .arg(input.invoke("get" + configuration.valueProperty))
        .arg(input.invoke("get" + configuration.currencyProperty)))
      .orElseGet(() -> defaultAmount(code, input, configuration));
  }

  private static JExpression decimalValue(JCodeModel code, JVar money, Optional<String> converter) {
    return converter.<JExpression>map(name -> code.ref(name).staticInvoke("value").arg(money))
      .orElseGet(() -> money.invoke("getNumber")
        .invoke("numberValueExact")
        .arg(code.ref(BigDecimal.class).dotclass()));
  }

  private static JExpression currencyCode(JCodeModel code, JVar money, Optional<String> converter) {
    return converter.<JExpression>map(name -> code.ref(name).staticInvoke("currency").arg(money))
      .orElseGet(() -> money.invoke("getCurrency").invoke("getCurrencyCode"));
  }

  private static JExpression defaultAmount(JCodeModel code, JVar input, AdapterConfiguration configuration) {
    JInvocation context = code.ref("javax.money.MonetaryContextBuilder")
      .staticInvoke("of")
      .invoke("set")
      .arg(code.ref(MathContext.class).staticRef("UNLIMITED"))
      .invoke("build");
    return code.ref("javax.money.Monetary")
      .staticInvoke("getDefaultAmountFactory")
      .invoke("setContext")
      .arg(context)
      .invoke("setCurrency")
      .arg(input.invoke("get" + configuration.currencyProperty))
      .invoke("setNumber")
      .arg(input.invoke("get" + configuration.valueProperty))
      .invoke("create");
  }

  CAdapter generate(CPropertyInfo property, Element settings) {
    AdapterConfiguration configuration = configuration(property, settings);
    return Optional.of(new CAdapter(adapterClass(configuration)))
      .filter(mapping -> configuration.xmlType.equals(mapping.defaultType.fullName())
        && BOUND_TYPE.equals(mapping.customType.fullName()))
      .orElseThrow(() -> new IllegalArgumentException(
        "Amount adapter name is already used for a different XML or Java type: " + configuration.adapterName));
  }

  private JDefinedClass adapterClass(AdapterConfiguration configuration) {
    String name = configuration.adapterName;
    JDefinedClass adapter = model.codeModel._getClass(name);
    AdapterConfiguration previous = generatedAdapters.get(name);
    if (previous != null && !previous.equals(configuration)) {
      throw new IllegalArgumentException("Generated amount adapter name has conflicting configuration: " + name);
    }

    if (adapter != null && previous == null) {
      throw new IllegalArgumentException("Amount adapter class already exists: " + name);
    }

    if (adapter != null) {
      return adapter;
    }
    JDefinedClass generated = createAdapter(configuration);
    generatedAdapters.put(name, configuration);
    return generated;
  }

  private JDefinedClass createAdapter(AdapterConfiguration configuration) {
    JCodeModel code = model.codeModel;
    JDefinedClass adapter;
    try {
      adapter = code._class(configuration.adapterName);
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalArgumentException("Amount adapter class already exists: " + configuration.adapterName, e);
    }
    JClass xmlType = code.ref(configuration.xmlType);
    JClass boundType = code.ref(BOUND_TYPE);
    adapter._extends(code.ref(XmlAdapter.class).narrow(xmlType, boundType));
    adapter.javadoc().add("Converts XML properties " + configuration.valueName + " and "
      + configuration.currencyName + " to the standard JavaMoney amount type.");
    generateUnmarshal(adapter, configuration, xmlType, boundType);
    generateMarshal(adapter, configuration, xmlType, boundType);
    return adapter;
  }

  private void generateUnmarshal(JDefinedClass adapter, AdapterConfiguration configuration,
    JClass xmlType, JClass boundType) {
    JCodeModel code = model.codeModel;
    JMethod read = adapter.method(JMod.PUBLIC, boundType, "unmarshal");
    read.annotate(Override.class);
    read.javadoc().add("Reads the configured XML decimal value and currency properties.");
    read.javadoc().addParam("amount").add("the XML amount, or null");
    read.javadoc().addReturn().add("the JavaMoney amount, or null when the input is null");
    JVar input = read.param(xmlType, "amount");
    read.body()._if(input.eq(JExpr._null()))._then()._return(JExpr._null());
    read.body()._return(readAmount(code, input, configuration));
  }

  private void generateMarshal(JDefinedClass adapter, AdapterConfiguration configuration,
    JClass xmlType, JClass boundType) {
    JCodeModel code = model.codeModel;
    JMethod write = adapter.method(JMod.PUBLIC, xmlType, "marshal");
    write.annotate(Override.class);
    write.javadoc().add("Writes the amount into the configured XML value and currency properties.");
    write.javadoc().addParam("money").add("the JavaMoney amount, or null");
    write.javadoc().addReturn().add("the XML amount, or null when the input is null");
    JVar money = write.param(boundType, "money");
    write.body()._if(money.eq(JExpr._null()))._then()._return(JExpr._null());
    JVar output = write.body().decl(xmlType, "amount", JExpr._new(xmlType));
    write.body().invoke(output, "set" + configuration.valueProperty)
      .arg(decimalValue(code, money, configuration.converter));
    write.body().invoke(output, "set" + configuration.currencyProperty)
      .arg(currencyCode(code, money, configuration.converter));
    write.body()._return(output);
  }

  private static final class AdapterConfiguration {

    private final String xmlType;
    private final String adapterName;

    private final Optional<String> converter;

    private final String valueProperty;
    private final String currencyProperty;

    private final String valueName;
    private final String currencyName;

    private AdapterConfiguration(String xmlType, Optional<String> converter, CPropertyInfo value, CPropertyInfo currency) {
      this.xmlType = xmlType;
      this.adapterName = xmlType + "XmlAdapter";
      this.converter = converter;
      this.valueProperty = value.getName(true);
      this.currencyProperty = currency.getName(true);
      this.valueName = value.getName(false);
      this.currencyName = currency.getName(false);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof AdapterConfiguration)) {
        return false;
      }
      AdapterConfiguration that = (AdapterConfiguration) other;
      return xmlType.equals(that.xmlType) && converter.equals(that.converter)
        && valueProperty.equals(that.valueProperty) && currencyProperty.equals(that.currencyProperty);
    }

    @Override
    public int hashCode() {
      return Objects.hash(xmlType, converter, valueProperty, currencyProperty);
    }
  }
}
