# Extras XJC Plugin

This plugin provides some extra (missing) XJC features to generated Java Beans.

## Usage

Build and run this plugin with Java 11 or newer and Maven 3.9.2 or newer.

Add this library to the JAXB compiler plugin and activate the respective plugins.

```xml
<plugin>
  <groupId>org.jvnet.jaxb</groupId>
  <artifactId>jaxb-maven-plugin</artifactId>
  <configuration>
    <extension>true</extension>
    <plugins>
      <plugin>
        <groupId>io.fares.bind.xjc.plugins</groupId>
        <artifactId>extras-xjc-plugin</artifactId>
        <version>1.0.5</version>
      </plugin>
    </plugins>
    <args>
      <arg>-Xxml-adapter</arg>
    </args>
  </configuration>
</plugin>
```

## Logging Compatibility

The project compiles against the SLF4J 1.7 API to expose incompatible SLF4J 2 calls. Plugin hooks use the classic `Logger` and `LoggerFactory` methods. Do not use SLF4J 2 fluent APIs. Logback 1.2 is used only by tests.

## Add XmlAdapter for Simple Types

This plugin can also be used to "override" XJC adapters that have been attached to a simple type in the global bindings.

```xml
<jaxb:globalBindings>

  <xjc:javaType name="java.time.OffsetDateTime"
                xmlType="xsd:dateTime"
                adapter="test.time.OffsetDateTimeAdapter"/>

</jaxb:globalBindings>
```

To override the default adapter binding just add the `extras:xml-adapter` annotations to either `xsd:element` or `xsd:attribute`:

```xml
<xsd:schema targetNamespace="urn:replacesimpletype.test"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:jaxb="https://jakarta.ee/xml/ns/jaxb"
			      xmlns:xjc ="http://java.sun.com/xml/ns/jaxb/xjc"
            xmlns:extras="urn:jaxb.fares.io:extras"
            elementFormDefault="qualified"
            jaxb:version="3.0"
            jaxb:extensionBindingPrefixes="xjc extras">

  <xsd:annotation>
    <xsd:appinfo>
      <jaxb:globalBindings fixedAttributeAsConstantProperty="true">
        <xjc:javaType name="java.time.OffsetDateTime"
                      xmlType="xsd:dateTime"
                      adapter="test.time.OffsetDateTimeAdapter"/>
      </jaxb:globalBindings>
    </xsd:appinfo>
  </xsd:annotation>

  <xsd:complexType name="Book">
        <xsd:sequence>
          <xsd:element name="overrideElement" type="xsd:dateTime">
            <xsd:annotation>
              <xsd:appinfo>
                <extras:xml-adapter name="test.time.LocalDateTimeAdapter"/>
              </xsd:appinfo>
            </xsd:annotation>
          </xsd:element>
        </xsd:sequence>
        <xsd:attribute  name="overrideAttribute" type="xsd:dateTime">
          <xsd:annotation>
            <xsd:appinfo>
              <extras:xml-adapter name="test.time.LocalDateTimeAdapter"/>
            </xsd:appinfo>
          </xsd:annotation>
        </xsd:attribute>
  </xsd:complexType>

  <xsd:attribute name="refAttribute1" type="xsd:dateTime"/>
  <xsd:attribute name="refAttribute2" type="xsd:string"/>

  <!-- this one currently would not work -->
  <xsd:attribute name="refAttribute3" type="xsd:dateTime">
    <xsd:annotation>
      <xsd:appinfo>
        <extras:xml-adapter name="test.time.LocalDateTimeAdapter"/>
      </xsd:appinfo>
    </xsd:annotation>
  </xsd:attribute>

</xsd:schema>

```

Place the customization on the local attribute use, including an attribute with `ref`. A customization on the global attribute declaration is not applied to its uses.


## Add XmlAdapter for Complex Types

### Problem

XJC's `xjc:javaType` global binding supports simple types. Applying it to the complex type `{urn:complextypeadapter.test}Amount` fails with `com.sun.istack.SAXParseException2: undefined simple type`:

```xml
<jaxb:globalBindings>

  <xjc:javaType name="javax.money.MonetaryAmount"
                xmlType="c:Amount"
                adapter="test.complextypeadapter.AmountXmlAdapter"/>

</jaxb:globalBindings>
```

### Solution

Enable `-Xxml-adapter` and attach `extras:xml-adapter` to the complex type in the schema or an external `.xjb` file. The plugin applies the named adapter to properties of that XML type. It checks the adapter's XML type to prevent incompatible mappings.

This example maps `Amount` through `AmountXmlAdapter`:


```xml
<xsd:schema targetNamespace="urn:complextypeadapter.test"
            xmlns:tns="urn:complextypeadapter.test"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:jaxb="https://jakarta.ee/xml/ns/jaxb"
            xmlns:extras="urn:jaxb.fares.io:extras"
            jaxb:version="3.0"
            jaxb:extensionBindingPrefixes="extras">

  <xsd:element name="Amount" type="tns:Amount"/>
  <xsd:complexType name="Amount">
    <xsd:annotation>
      <xsd:appinfo>
        <extras:xml-adapter name="test.complextypeadapter.AmountXmlAdapter"/>
      </xsd:appinfo>
    </xsd:annotation>
    <xsd:simpleContent>
      <xsd:extension base="xsd:decimal">
        <xsd:attribute name="currency" type="tns:CurrencyCode" use="required">
          <xsd:annotation>
            <xsd:documentation>The ISO 4217 currency code for this amount.</xsd:documentation>
          </xsd:annotation>
        </xsd:attribute>
      </xsd:extension>
    </xsd:simpleContent>
  </xsd:complexType>

  <xsd:simpleType name="CurrencyCode">
    <xsd:annotation>
      <xsd:documentation>Use ISO 4217 three letter alpha code. </xsd:documentation>
    </xsd:annotation>
    <xsd:restriction base="xsd:string">
      <xsd:pattern value="[A-Z]{3}"/>
    </xsd:restriction>
  </xsd:simpleType>

</xsd:schema>
```

A property using `Amount` now exposes `MonetaryAmount` and carries `@XmlJavaTypeAdapter(AmountXmlAdapter.class)`:


```xml
<xsd:schema targetNamespace="urn:book.test"
            xmlns:c="urn:complextypeadapter.test"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema">

  <xsd:import namespace="urn:complextypeadapter.test" schemaLocation="Amount.xsd" />

  <xsd:complexType name="Book">
        <xsd:sequence>
          <xsd:element name="isbn" type="xsd:string" />
          <xsd:element name="price" type="c:Amount" />
        </xsd:sequence>
  </xsd:complexType>

</xsd:schema>
```

```java
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Book", propOrder = {
    "isbn",
    "price"
})
public class Book {

    @XmlElement(required = true)
    protected String isbn;

    @XmlElement(required = true, type = Amount.class)
    @XmlJavaTypeAdapter(AmountXmlAdapter.class)
    protected MonetaryAmount price;

    ...

}
```

## JavaMoney Adapters

Enable `-Xxml-adapter` and attach `extras:java-money-adapter` to the complex type or its element property, inline in the schema or through an external `.xjb` file. Generated properties use `javax.money.MonetaryAmount`.

```xml
<bindings xmlns="https://jakarta.ee/xml/ns/jaxb"
          xmlns:xs="http://www.w3.org/2001/XMLSchema"
          xmlns:extras="urn:jaxb.fares.io:extras"
          version="3.0"
          extensionBindingPrefixes="extras">
  <bindings schemaLocation="model.xsd" node="/xs:schema">
    <bindings node="xs:complexType[@name='Amount']">
      <extras:java-money-adapter/>
    </bindings>
  </bindings>
</bindings>
```

The amount type must contain only a decimal value and a required string currency attribute or element. The value can be decimal simple content or a required decimal child element.

| Attribute | Default |
|---|---|
| `currencyProperty` | `currencyCode` |
| `valueProperty` | `value` |
| `converter` | Standard JavaMoney conversion |

A property customization takes precedence over its type customization. Both adapter kinds on the same scope are rejected. Direct customizations on reference properties, value properties, or global element declarations are rejected with the component and source location.

Property names refer to the generated Java model. Set an explicit property name for each field that does not use `currencyCode` or `value`. For example, to use `currency` and `amount`:

```xml
<extras:java-money-adapter
  currencyProperty="currency"
  valueProperty="amount"
  converter="org.example.money.JavaMoneyAmountAdapter"/>
```

An explicit value property must exist, be decimal and differ from the currency property. A custom converter provides three public static methods:

```java
MonetaryAmount of(BigDecimal value, String currency);
BigDecimal value(MonetaryAmount amount);
String currency(MonetaryAmount amount);
```

At runtime, generated bindings need JAXB and JavaMoney. Default conversion requests `MathContext.UNLIMITED` from the application's JavaMoney provider. A configured converter must also be on the runtime classpath. The Extras compiler plugin is not needed at runtime.
