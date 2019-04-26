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

import com.sun.tools.xjc.model.*;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.xml.bind.v2.model.core.TypeInfo;
import io.fares.bind.xjc.plugins.extras.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Stack;

import static io.fares.bind.xjc.plugins.extras.Utils.findCustomizations;
import static io.fares.bind.xjc.plugins.extras.SecureLoader.getContextClassLoader;
import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.COMPLEX_XML_ADAPTER;
import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.COMPLEX_XML_ADAPTER_NAME;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class AdapterCustomizer implements CPropertyVisitor<CPropertyInfo> {

  private static final Logger log = LoggerFactory.getLogger(AdapterCustomizer.class);

  private final Model model;

  private final AdapterInspector inspector;

  AdapterCustomizer(final Model model, final AdapterInspector inspector) {
    this.model = model;
    this.inspector = inspector;
  }

  @Override
  public CPropertyInfo onElement(CElementPropertyInfo propertyInfo) {

    // property customizations trump type ones

    Stack<CPluginCustomization> customizations = new Stack<>();

    findCustomizations(propertyInfo, COMPLEX_XML_ADAPTER_NAME).forEach(customizations::push);

    for (CTypeInfo ref : propertyInfo.ref()) {
      findCustomizations(ref, COMPLEX_XML_ADAPTER_NAME).forEach(customizations::push);
    }

    if (!customizations.empty()) {

      // top most trumps all others
      CPluginCustomization customization = customizations.pop();

      Element ce = customization.element;

      try {

        String adapterClassName = ofNullable(ce.getAttribute("name"))
          .map(String::trim)
          .filter(Utils::isNotEmpty)
          .orElseThrow(
          () -> new IllegalArgumentException(COMPLEX_XML_ADAPTER_NAME.toString() + " must specify the XML adapter class with the name attribute")
        );

        Class<?> adapterClass = getContextClassLoader().loadClass(adapterClassName);

        if (XmlAdapter.class.isAssignableFrom(adapterClass)) {

          log.info("customize {} with {} {}", propertyInfo.displayName(), COMPLEX_XML_ADAPTER, adapterClassName);

          // tried this but a com.sun.tools.xjc.model.nav.NavigatorImpl.getTypeArgument() would not return
          // appropriate type information and the setup of defaultType and customType in
          // com.sun.xml.bind.v2.model.core.Adapter.Adapter(ClassDeclT, com.sun.xml.bind.v2.model.nav.Navigator<TypeT,ClassDeclT,?,?>)
          // would default to java.lang.Object.
          // JClass adapterClass = model.codeModel.ref(adapter);

          @SuppressWarnings("unchecked")
          CAdapter adapter = new CAdapter((Class<XmlAdapter>) adapterClass, false);

          String adapterType = adapter.defaultType.fullName();

          //  test that adapter.defaultType matches the type definition or ref on the field

          boolean matches = propertyInfo.ref().stream()
            .findFirst()
            .map(TypeInfo::getType)
            .map(NType::fullName)
            .filter(adapterType::equals)
            .isPresent();

          if (!matches) {
            log.error(format("unable to verify class type of %s, please raise a defect for error", propertyInfo.displayName()));
          }

          propertyInfo.setAdapter(adapter);

        } else {
          throw new IllegalArgumentException("adapter class " + adapterClassName + " does not extend javax.xml.bind.annotation.adapters.XmlAdapter");
        }

      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("failed to process customization " + COMPLEX_XML_ADAPTER_NAME.toString(), e);
      }

    }

    return null;

  }

  // region not used

  @Override
  public CPropertyInfo onAttribute(CAttributePropertyInfo p) {
    return null;
  }

  @Override
  public CPropertyInfo onValue(CValuePropertyInfo p) {
    return null;
  }

  @Override
  public CPropertyInfo onReference(CReferencePropertyInfo p) {
    return null;
  }

  // endregion

}
