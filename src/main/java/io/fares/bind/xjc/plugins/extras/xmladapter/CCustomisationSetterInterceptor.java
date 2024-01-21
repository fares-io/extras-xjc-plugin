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

import com.sun.tools.xjc.model.CCustomizable;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.Model;
import io.fares.bind.xjc.plugins.extras.Utils;
import net.bytebuddy.asm.Advice;

import static io.fares.bind.xjc.plugins.extras.xmladapter.AdapterPlugin.COMPLEX_XML_ADAPTER_NAME;

public class CCustomisationSetterInterceptor {

  // apply to void setParent(Model model,CCustomizable owner)
  @Advice.OnMethodEnter
  private static void enter(@Advice.Argument(0) Model model, @Advice.Argument(1) CCustomizable owner) {

    if (owner == null || owner.getCustomizations() == null) {
      return;
    }

    for (CPluginCustomization c : owner.getCustomizations()) {
      if (Utils.matchesCustomization(c, COMPLEX_XML_ADAPTER_NAME)) {
//        System.err.println("ack " + c.element.getTagName() + " at " + Utils.toLocation(owner.getLocator()));
        c.markAsAcknowledged();
      }
    }

  }

}
