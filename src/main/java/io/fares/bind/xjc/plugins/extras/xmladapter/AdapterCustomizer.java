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

import com.sun.tools.xjc.model.*;
import io.fares.bind.xjc.plugins.extras.Utils;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.fares.bind.xjc.plugins.extras.SecureLoader.getContextClassLoader;
import static io.fares.bind.xjc.plugins.extras.Utils.findCustomizations;
import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.*;

/** Applies XML adapter customizations to XJC properties. */
public class AdapterCustomizer implements CPropertyVisitor<CPropertyInfo> {

  private final AmountAdapterGenerator amountAdapterGenerator;

  // Retained for later hooks that reconcile applied customizations.
  @SuppressWarnings("unused")
  private final AdapterInspector inspector;

  AdapterCustomizer(AmountAdapterGenerator amountAdapterGenerator, AdapterInspector inspector) {
    this.amountAdapterGenerator = amountAdapterGenerator;
    this.inspector = inspector;
  }

  @Override
  public CPropertyInfo onElement(CElementPropertyInfo propertyInfo) {
    findAdapter(propertyInfo).ifPresent(adapter -> setElementAdapter(propertyInfo, adapter));
    return null;
  }

  @Override
  public CPropertyInfo onAttribute(CAttributePropertyInfo propertyInfo) {
    findAdapter(propertyInfo).ifPresent(adapter -> setAttributeAdapter(propertyInfo, adapter));
    return null;
  }

  private Optional<CAdapter> findAdapter(CPropertyInfo propertyInfo) {
    List<CPluginCustomization> selected = adapterCustomizations(propertyInfo).collect(Collectors.toList());
    if (selected.isEmpty()) {
      selected = propertyInfo.ref().stream()
        .flatMap(this::adapterCustomizations)
        .collect(Collectors.toList());
    }

    boolean xmlAdapter = selected.stream().anyMatch(c -> Utils.matchesCustomization(c, COMPLEX_XML_ADAPTER_NAME));
    boolean moneyAdapter = selected.stream().anyMatch(c -> Utils.matchesCustomization(c, JAVA_MONEY_ADAPTER_NAME));
    if (xmlAdapter && moneyAdapter) {
      throw new IllegalArgumentException(
        "A property cannot use both " + COMPLEX_XML_ADAPTER_NAME + " and " + JAVA_MONEY_ADAPTER_NAME
          + ": " + propertyInfo.displayName());
    }

    return selected.stream()
      .reduce((earlier, later) -> later)
      .map(customization -> moneyAdapter
        ? amountAdapterGenerator.generate(propertyInfo, customization.element)
        : createXmlAdapter(customization));
  }

  private Stream<CPluginCustomization> adapterCustomizations(CCustomizable customizable) {
    return Stream.concat(
      findCustomizations(customizable, COMPLEX_XML_ADAPTER_NAME).stream(),
      findCustomizations(customizable, JAVA_MONEY_ADAPTER_NAME).stream());
  }

  private CAdapter createXmlAdapter(CPluginCustomization customization) {
    String name = Optional.of(customization.element.getAttribute("name"))
      .map(String::trim)
      .filter(Utils::isNotEmpty)
      .orElseThrow(() -> new IllegalArgumentException(
        COMPLEX_XML_ADAPTER_NAME + " must specify the XML adapter class with the name attribute"));
    return new CAdapter(loadAdapterClass(name), false);
  }

  @SuppressWarnings("rawtypes")
  private Class<? extends XmlAdapter> loadAdapterClass(String name) {
    try {
      Class<?> adapterClass = getContextClassLoader().loadClass(name);
      if (!XmlAdapter.class.isAssignableFrom(adapterClass)) {
        throw new IllegalArgumentException(
          "adapter class " + name + " does not extend jakarta.xml.bind.annotation.adapters.XmlAdapter");
      }
      return adapterClass.asSubclass(XmlAdapter.class);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("failed to process customization " + COMPLEX_XML_ADAPTER_NAME + ": " + name, e);
    }
  }

  private void setElementAdapter(CElementPropertyInfo propertyInfo, CAdapter adapter) {
    if (propertyInfo.ref().size() != 1) {
      throw new IllegalArgumentException("Adapter requires one XML type on " + propertyInfo.displayName());
    }

    CNonElement reference = propertyInfo.ref().iterator().next();
    if (isCompatible(reference, adapter)) {
      logReport("   [+]: modify {} element with {} {}",
        propertyInfo.displayName(), COMPLEX_XML_ADAPTER, adapter.adapterType.fullName());
      if (propertyInfo.getAdapter() == null) {
        propertyInfo.setAdapter(adapter);
      } else {
        forceTypeField(propertyInfo, "adapter", adapter);
      }
      return;
    }

    CValuePropertyInfo value = Optional.of(reference)
      .filter(CClassInfo.class::isInstance)
      .map(CClassInfo.class::cast)
      .flatMap(type -> type.getProperties().stream()
        .filter(CValuePropertyInfo.class::isInstance)
        .map(CValuePropertyInfo.class::cast)
        .filter(property -> isCompatible(property.getTarget(), adapter))
        .findFirst())
      .orElseThrow(() -> incompatibleAdapter(propertyInfo, reference, adapter));

    logReport("   [+]: modify {} type extension with {} {}",
      propertyInfo.displayName(), COMPLEX_XML_ADAPTER, adapter.adapterType.fullName());
    setValuePropertyAdapter(value, adapter);
  }

  private static boolean isCompatible(CNonElement reference, CAdapter adapter) {
    if (adapter.defaultType.fullName().equals(reference.getType().fullName())) {
      return true;
    }
    // JAXB string adapters read the lexical form of built in simple XML types.
    return reference instanceof CBuiltinLeafInfo && reference != CBuiltinLeafInfo.ANYTYPE
      && "java.lang.String".equals(adapter.defaultType.fullName());
  }

  private static IllegalArgumentException incompatibleAdapter(CPropertyInfo property, CNonElement reference, CAdapter adapter) {
    return new IllegalArgumentException("Adapter " + adapter.adapterType.fullName()
      + " cannot adapt " + property.displayName() + " from " + reference.getType().fullName()
      + " to its XML type " + adapter.defaultType.fullName());
  }

  private void setAttributeAdapter(CAttributePropertyInfo propertyInfo, CAdapter adapter) {
    if (!isCompatible(propertyInfo.getTarget(), adapter)) {
      throw incompatibleAdapter(propertyInfo, propertyInfo.getTarget(), adapter);
    }
    logReport("   [+]: modify {} attribute with {} {}",
      propertyInfo.displayName(), COMPLEX_XML_ADAPTER, adapter.adapterType.fullName());
    forceTypeField(propertyInfo, "type", TypeUseFactory.adapt(propertyInfo.getTarget(), adapter));
  }

  private void setValuePropertyAdapter(CValuePropertyInfo propertyInfo, CAdapter adapter) {
    forceTypeField(propertyInfo, "type", TypeUseFactory.adapt(propertyInfo.getTarget(), adapter));
  }

  /** Replaces XJC fields whose setters reject an adapter override. */
  @SuppressWarnings("java:S3011")
  private void forceTypeField(CPropertyInfo propertyInfo, String name, Object value) {
    try {
      Field field = findField(propertyInfo.getClass(), name);
      field.setAccessible(true);
      field.set(propertyInfo, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(
        "failed to apply " + COMPLEX_XML_ADAPTER_NAME + " to property " + propertyInfo.displayName(), e);
    }
  }

  private Field findField(Class<?> type, String name) throws NoSuchFieldException {
    try {
      return type.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      if (type.getSuperclass() == null) {
        throw e;
      }
      return findField(type.getSuperclass(), name);
    }
  }

  @Override
  public CPropertyInfo onValue(CValuePropertyInfo propertyInfo) {
    rejectUnsupportedCustomizations(propertyInfo);
    return null;
  }

  @Override
  public CPropertyInfo onReference(CReferencePropertyInfo propertyInfo) {
    rejectUnsupportedCustomizations(propertyInfo);
    return null;
  }

  private void rejectUnsupportedCustomizations(CPropertyInfo property) {
    Optional<CPluginCustomization> customization = adapterCustomizations(property).findFirst();
    if (customization.isPresent()) {
      CPluginCustomization unsupported = customization.get();
      throw new IllegalArgumentException("Unsupported " + unsupported.element.getTagName()
        + " on " + property.displayName() + " at " + Utils.toLocation(unsupported.locator));
    }
  }
}
