# Extras XJC Plugin

This plugin provides some extra (missing) XJC features to generated Java Beans.

## Usage

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
        <version>1.0.0</version>
      </plugin>
    </plugins>
    <args>
      <arg>-Xxml-adapter</arg>
    </args>
  </configuration>
</plugin>
```

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
            xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
            xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc"
            xmlns:extras="http://jaxb2-commons.dev.java.net/xjc/extras"
            elementFormDefault="qualified"
            jaxb:version="2.1"
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

Note: Annotations on the ref attribute currently do not work


## Add XmlAdapter for Complex Types

For some reason, XJC does not allow global bindings of complex types. Below will fail with an error message `com.sun.istack.SAXParseException2: undefined simple type "{urn:complextypeadapter.test}Amount"`:

```xml
<jaxb:globalBindings>

  <xjc:javaType name="javax.money.MonetaryAmount"
                xmlType="c:Amount"
                adapter="test.complextypeadapter.AmountXmlAdapter"/>

</jaxb:globalBindings>
```

Unfortunately XJC does not support custom XJC plugin extensions for `globalBindings`. The best we can do is either attach customisations to a schema or to the target type we want to map.

Below example will attach a customisation to the complex type that we want to map using the `AmountXmlAdapter`. The plugin will process every generated field and if the field is of xml type `c:Amount`, it will attach the `XmlAdapter` as specified in `extras:xml-adapter/@name`. It will also ensure that the type of the field exactly matches the custom type of the `XmlAdapter` to prevent extended types to be wrongly adapted to prevent loss of data.


```xml
<xsd:schema targetNamespace="urn:complextypeadapter.test"
            xmlns:tns="urn:complextypeadapter.test"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
            xmlns:extras="http://jaxb2-commons.dev.java.net/xjc/extras"
            jaxb:version="2.1"
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
            <xsd:documentation>The ISO 4217 compliant currency code the currency ammount is nominated in.</xsd:documentation>
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

When using this type in another schema, the generate a field is of type `javax.money.MonetaryAmount` instead of the corresponding generated XML type and the relevant `@XmlJavaTypeAdapter` annotation is in place.


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
