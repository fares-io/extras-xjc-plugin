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

import com.sun.codemodel.JPackage;
import com.sun.tools.xjc.model.*;
import io.fares.bind.xjc.plugins.extras.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Just to ack any customizations attached to the type.
 */
public class AdapterInspector implements CClassInfoParent.Visitor<CClassInfo>, CPropertyVisitor<CPropertyInfo> {

  private static final Logger log = LoggerFactory.getLogger(AdapterInspector.class);

  private final Map<String, Stack<AdapterCandidate>> typeCandidates = new HashMap<>();

  private final Map<String, Stack<AdapterCandidate>> fieldCandidates = new HashMap<>();

  public Map<String, Stack<AdapterCandidate>> getTypeCandidates() {
    return typeCandidates;
  }

  public Map<String, Stack<AdapterCandidate>> getFieldCandidates() {
    return fieldCandidates;
  }

  @Override
  public CClassInfo onBean(CClassInfo classInfo) {

    List<CPluginCustomization> customizations = Utils.findCustomizations(classInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);

    if (!customizations.isEmpty()) {
      trackCustomizations(classInfo, customizations);
    }

    // now descend into all the properties
    for (CPropertyInfo p : classInfo.getProperties()) {
      p.accept(this);
    }

    return null;

  }

  @Override
  public CPropertyInfo onElement(CElementPropertyInfo propertyInfo) {

    List<CPluginCustomization> elementCustomizations = Utils.findCustomizations(propertyInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);

    if (!elementCustomizations.isEmpty()) {
      trackCustomizations(propertyInfo, elementCustomizations);
    }

    for (CTypeInfo ref : propertyInfo.ref()) {

      List<CPluginCustomization> typeCustomizations = Utils.findCustomizations(ref, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);

      if (!typeCustomizations.isEmpty()) {
        trackCustomizations(propertyInfo, ref, typeCustomizations);
      }

    }

    return null;

  }

  @Override
  public CClassInfo onElement(CElementInfo element) {
    List<CPluginCustomization> c = Utils.findCustomizations(element, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);
    if (!c.isEmpty()) {
      log.warn("element reference {} has {} unsupported customization attached", element.fullName(), AdapterPlugin.COMPLEX_XML_ADAPTER);
    }
    return null;
  }

  @Override
  public CPropertyInfo onAttribute(CAttributePropertyInfo propertyInfo) {

    List<CPluginCustomization> elementCustomizations = Utils.findCustomizations(propertyInfo, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);

    if (!elementCustomizations.isEmpty()) {
      trackCustomizations(propertyInfo, elementCustomizations);
    }

    for (CTypeInfo ref : propertyInfo.ref()) {

      List<CPluginCustomization> typeCustomizations = Utils.findCustomizations(ref, AdapterPlugin.COMPLEX_XML_ADAPTER_NAME);

      if (!typeCustomizations.isEmpty()) {
        trackCustomizations(propertyInfo, ref, typeCustomizations);
      }

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

      Optional<String> adapterName = getAdapterName(customization);

      if (!adapterName.isPresent()) {

        log.warn("{} customization on field {} does not specify a name of the adapter", AdapterPlugin.COMPLEX_XML_ADAPTER_NAME, propertyInfo.displayName());

      } else if (!fieldCandidates.containsKey(adapterName.get())) {

        Stack<AdapterCandidate> candidates = new Stack<>();
        candidates.push(new AdapterCPropertyInfoCandidate(propertyInfo));
        fieldCandidates.put(adapterName.get(), candidates);

      } else {

        Stack<AdapterCandidate> candidates = fieldCandidates.get(adapterName.get());
        candidates.add(new AdapterCPropertyInfoCandidate(propertyInfo));
      }

    }

  }

  private void trackCustomizations(CPropertyInfo propertyInfo, CTypeInfo ref, List<CPluginCustomization> customizations) {

    for (CPluginCustomization customization : customizations) {

      Optional<String> adapterName = getAdapterName(customization);

      if (!adapterName.isPresent()) {

        log.warn("{} customization on field {} does not specify a name of the adapter", AdapterPlugin.COMPLEX_XML_ADAPTER_NAME, propertyInfo.displayName());

      } else if (!fieldCandidates.containsKey(adapterName.get())) {

        Stack<AdapterCandidate> candidates = new Stack<>();
        candidates.push(new AdapterCPropertyInfoRefCandidate(propertyInfo, ref));
        fieldCandidates.put(adapterName.get(), candidates);

      } else {

        Stack<AdapterCandidate> candidates = fieldCandidates.get(adapterName.get());
        candidates.add(new AdapterCPropertyInfoRefCandidate(propertyInfo, ref));

      }

    }

  }

  private void trackCustomizations(CClassInfo classInfo, List<CPluginCustomization> customizations) {

    for (CPluginCustomization customization : customizations) {

      Optional<String> adapterName = getAdapterName(customization);

      if (!adapterName.isPresent()) {

        log.warn("{} customization on class {} does not specify a name of the adapter", AdapterPlugin.COMPLEX_XML_ADAPTER_NAME, classInfo.fullName());

      } else if (!typeCandidates.containsKey(adapterName.get())) {

        Stack<AdapterCandidate> candidates = new Stack<>();
        candidates.push(new AdapterCClassInfoCandidate(classInfo));
        typeCandidates.put(adapterName.get(), candidates);

      } else {

        Stack<AdapterCandidate> candidates = typeCandidates.get(adapterName.get());
        candidates.add(new AdapterCClassInfoCandidate(classInfo));
      }

    }

  }

  private Optional<String> getAdapterName(CPluginCustomization customization) {
    return Optional.ofNullable(customization.element.getAttribute("name"))
      .filter(s -> !"".equals(s.trim()));
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


