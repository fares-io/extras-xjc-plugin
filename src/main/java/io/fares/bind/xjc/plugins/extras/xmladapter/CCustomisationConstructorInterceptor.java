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

import com.sun.tools.xjc.model.CPluginCustomization;
import io.fares.bind.xjc.plugins.extras.Utils;
import net.bytebuddy.asm.Advice;

import java.util.Collection;

import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.COMPLEX_XML_ADAPTER_NAME;

public class CCustomisationConstructorInterceptor {

  // apply to public CCustomizations(Collection<? extends CPluginCustomization> cPluginCustomizations)
  @Advice.OnMethodEnter
  private static void enter(@Advice.AllArguments Object[] params) {

    if (params == null || params.length != 1 || !(params[0] instanceof Collection)) {
      return;
    }

    Collection<?> customizations = (Collection) params[0];

    for (Object o : customizations) {
      if (o instanceof CPluginCustomization) {
        CPluginCustomization c = (CPluginCustomization) o;
        if (Utils.matchesCustomization(c, COMPLEX_XML_ADAPTER_NAME)) {
//          System.err.println("ack " + c.element.getTagName() + " -> " + c.toString() + " " + Utils.toLocation(c.locator));
          c.markAsAcknowledged();
        }
      }
    }

  }


}
