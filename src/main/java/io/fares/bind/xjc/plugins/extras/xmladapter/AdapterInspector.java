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

import com.sun.codemodel.JPackage;
import com.sun.tools.xjc.model.*;
import io.fares.bind.xjc.plugins.extras.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Collects XML adapter customizations from XJC classes and properties.
 */
@SuppressWarnings({"rawtypes"})
public class AdapterInspector implements CClassInfoParent.Visitor<CClassInfo>, CPropertyVisitor<CPropertyInfo> {

  private static final Logger log = LoggerFactory.getLogger(AdapterInspector.class);

  private final Map<String, Stack<AdapterCandidate>> typeCandidates = new HashMap<>();

  private final Map<String, Stack<AdapterCandidate>> fieldCandidates = new HashMap<>();

  /** Creates an inspector for XML adapter customizations. */
  public AdapterInspector() {
  }

  /**
   * Returns candidates declared on XJC classes.
   *
   * @return candidates indexed by adapter class name
   */
  public Map<String, Stack<AdapterCandidate>> getTypeCandidates() {
    return typeCandidates;
  }

  /**
   * Returns candidates declared on XJC properties or property types.
   *
   * @return candidates indexed by adapter class name
   */
  public Map<String, Stack<AdapterCandidate>> getFieldCandidates() {
    return fieldCandidates;
  }

  @Override
  public CClassInfo onBean(CClassInfo classInfo) {

    trackCustomizations(classInfo, Utils.findCustomizations(classInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME));
    trackCustomizations(classInfo, Utils.findCustomizations(classInfo, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME));

    // now descend into all the properties
    for (CPropertyInfo p : classInfo.getProperties()) {
      p.accept(this);
    }

    return null;

  }

  @Override
  public CPropertyInfo onElement(CElementPropertyInfo propertyInfo) {

    trackCustomizations(propertyInfo, Utils.findCustomizations(propertyInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME));
    trackCustomizations(propertyInfo, Utils.findCustomizations(propertyInfo, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME));

    for (CTypeInfo ref : propertyInfo.ref()) {

      trackCustomizations(propertyInfo, ref, Utils.findCustomizations(ref, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME));
      trackCustomizations(propertyInfo, ref, Utils.findCustomizations(ref, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME));

    }

    return null;

  }

  @Override
  public CClassInfo onElement(CElementInfo element) {
    for (CPluginCustomization customization : element.getCustomizations()) {
      if (AdapterPlugin.isSupportedCustomization(customization)) {
        throw new IllegalArgumentException("Unsupported " + customization.element.getTagName()
          + " on global element " + element.getElementName()
          + " at " + Utils.toLocation(customization.locator));
      }
    }
    return null;
  }

  @Override
  public CPropertyInfo onAttribute(CAttributePropertyInfo propertyInfo) {

    trackCustomizations(propertyInfo, Utils.findCustomizations(propertyInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME));
    trackCustomizations(propertyInfo, Utils.findCustomizations(propertyInfo, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME));

    for (CTypeInfo ref : propertyInfo.ref()) {

      trackCustomizations(propertyInfo, ref, Utils.findCustomizations(ref, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME));
      trackCustomizations(propertyInfo, ref, Utils.findCustomizations(ref, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME));

    }

    return null;
  }

  @Override
  public CPropertyInfo onReference(CReferencePropertyInfo propertyInfo) {
    List<CPluginCustomization> c = Utils.findCustomizations(propertyInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);
    if (!c.isEmpty()) {
      log.warn("reference property {} has {} unsupported customization attached", propertyInfo.displayName(), AdapterPlugin.COMPLEX_XML_ADAPTER);
    }
    return null;
  }

  private void trackCustomizations(CPropertyInfo propertyInfo, List<CPluginCustomization> customizations) {

    for (CPluginCustomization customization : customizations) {

      Optional<String> adapterName = getAdapterName(customization, generatedType(propertyInfo));

      if (!adapterName.isPresent()) {

        log.warn("{} customization on field {} does not specify a name of the adapter", AdapterPlugin.COMPLEX_XML_ADAPTER_NAME,
          propertyInfo.displayName());

      } else {
        fieldCandidates.computeIfAbsent(adapterName.get(), name -> new Stack<>())
          .push(new AdapterCPropertyInfoCandidate(propertyInfo));
      }

    }

  }

  private void trackCustomizations(CPropertyInfo propertyInfo, CTypeInfo ref, List<CPluginCustomization> customizations) {

    for (CPluginCustomization customization : customizations) {

      Optional<String> adapterName = getAdapterName(customization, ref);

      if (!adapterName.isPresent()) {

        log.warn("{} customization on field {} does not specify a name of the adapter", AdapterPlugin.COMPLEX_XML_ADAPTER_NAME,
          propertyInfo.displayName());

      } else {
        fieldCandidates.computeIfAbsent(adapterName.get(), name -> new Stack<>())
          .push(new AdapterCPropertyInfoRefCandidate(propertyInfo, ref));
      }

    }

  }

  private void trackCustomizations(CClassInfo classInfo, List<CPluginCustomization> customizations) {

    for (CPluginCustomization customization : customizations) {

      Optional<String> adapterName = getAdapterName(customization, classInfo);

      if (!adapterName.isPresent()) {

        log.warn("{} customization on class {} does not specify a name of the adapter",
          AdapterPlugin.COMPLEX_XML_ADAPTER_NAME, classInfo.fullName());

      } else {
        typeCandidates.computeIfAbsent(adapterName.get(), name -> new Stack<>())
          .push(new AdapterCClassInfoCandidate(classInfo));
      }

    }

  }

  private Optional<String> getAdapterName(CPluginCustomization customization, CTypeInfo generatedType) {
    Optional<String> explicitName = Optional.ofNullable(customization.element.getAttribute("name"))
      .filter(s -> !"".equals(s.trim()));
    if (explicitName.isPresent()) {
      return explicitName;
    }
    if (Utils.matchesCustomization(customization, AdapterPlugin.JAVA_MONEY_ADAPTER_NAME)
      && (generatedType instanceof CClassInfo || generatedType instanceof CClassRef)) {
      return Optional.of(generatedType.getType().fullName() + "XmlAdapter");
    }
    return Optional.empty();
  }

  private CTypeInfo generatedType(CPropertyInfo propertyInfo) {
    if (propertyInfo.ref().size() == 1) {
      return propertyInfo.ref().iterator().next();
    }
    return null;
  }

  // region not used

  @Override
  public CClassInfo onPackage(JPackage pkg) {
    return null;
  }

  @Override
  public CPropertyInfo onValue(CValuePropertyInfo p) {
    return null;
  }

  // endregion

}
