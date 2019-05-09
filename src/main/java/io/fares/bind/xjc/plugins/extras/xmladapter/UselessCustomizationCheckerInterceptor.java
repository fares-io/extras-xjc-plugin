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

import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.xml.xsom.XSComponent;
import net.bytebuddy.asm.Advice;

import javax.xml.namespace.QName;

import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.COMPLEX_XML_ADAPTER_NAME;

/**
 * This advice will ensure that the {@link com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXPluginCustomization}
 * for complex XmlAdapter is acknowledged.
 */
public class UselessCustomizationCheckerInterceptor {

  @Advice.OnMethodEnter
  private static void check(@Advice.Argument(0) BIDeclaration decl, @Advice.Argument(1) XSComponent xc, @Advice.Origin Class<?> type) {

    QName declName = decl.getName();

    if (declName != null &&
      COMPLEX_XML_ADAPTER_NAME.getNamespaceURI().equals(declName.getNamespaceURI()) &&
      COMPLEX_XML_ADAPTER_NAME.getLocalPart().equals(declName.getLocalPart())) {
//      System.err.println("ack decl " + decl.getClass().getName());
      decl.markAsAcknowledged();
    }

  }

}
