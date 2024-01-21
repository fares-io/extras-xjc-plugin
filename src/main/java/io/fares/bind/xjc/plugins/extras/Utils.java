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
package io.fares.bind.xjc.plugins.extras;

import com.sun.tools.xjc.model.CCustomizable;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Locator;

import javax.xml.namespace.QName;
import java.util.LinkedList;
import java.util.List;

public class Utils {

  private static final String XML_NAMESPACE = "xmlns";

  public static List<CPluginCustomization> findCustomizations(CCustomizable customizable, QName name) {
    return findCustomizations(customizable.getCustomizations(), name);
  }

  public static List<CPluginCustomization> findCustomizations(CCustomizations customizations, QName name) {

    final List<CPluginCustomization> foundCustomizations = new LinkedList<>();

    for (CPluginCustomization customization : customizations) {
      if (matchesCustomization(customization, name)) {
        customization.markAsAcknowledged();
        foundCustomizations.add(customization);
      }
    }

    return foundCustomizations;

  }

  public static boolean matchesCustomization(CPluginCustomization customization, QName name) {
    return fixNull(customization.element.getNamespaceURI()).equals(name.getNamespaceURI())
      && fixNull(customization.element.getLocalName()).equals(name.getLocalPart());
  }

  public static String toLocation(Locator l) {

    if (l == null) {
      return "unknown";
    }

    StringBuilder sb = new StringBuilder();
    if (l.getSystemId() != null) {
      sb.append(l.getSystemId());
    } else if (l.getPublicId() != null) {
      sb.append(l.getPublicId());
    } else {
      sb.append("unknown");
    }

    return sb.append('{')
      .append(l.getLineNumber())
      .append('.')
      .append(l.getColumnNumber())
      .append('}')
      .toString();

  }


  /**
   * Starting from a node, find the namespace declaration for a prefix and return the namespace.
   *
   * @param node         the node to start searching in
   * @param searchPrefix the prefix to search for
   * @return the namespace or <code>null</code> if nothing was found
   */
  public static String getNamespace(Node node, String searchPrefix) {

    Element el;

    while (!(node instanceof Element)) {
      node = node.getParentNode();
    }
    el = (Element) node;

    NamedNodeMap atts = el.getAttributes();
    for (int i = 0; i < atts.getLength(); i++) {
      Node currentAttribute = atts.item(i);
      String currentLocalName = currentAttribute.getLocalName();
      String currentPrefix = currentAttribute.getPrefix();
      if (searchPrefix.equals(currentLocalName) && XML_NAMESPACE.equals(currentPrefix)) {
        return currentAttribute.getNodeValue();
      } else if (isEmpty(searchPrefix) && XML_NAMESPACE.equals(currentLocalName)
        && isEmpty(currentPrefix)) {
        return currentAttribute.getNodeValue();
      }
    }

    Node parent = el.getParentNode();

    if (parent instanceof Element) {
      return getNamespace(parent, searchPrefix);
    }

    return null;

  }

  public static String fixNull(String s) {
    return s == null ? "" : s;
  }

  public static boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

  public static boolean isNotEmpty(String s) {
    return !isEmpty(s);
  }

}
