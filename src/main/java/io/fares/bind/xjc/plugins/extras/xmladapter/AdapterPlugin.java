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

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import org.jvnet.jaxb2_commons.plugin.AbstractParameterizablePlugin;
import org.xml.sax.ErrorHandler;

import javax.xml.namespace.QName;
import java.util.*;

import static java.lang.String.format;

public class AdapterPlugin extends AbstractParameterizablePlugin {

  public static final String NS = "http://jaxb2-commons.dev.java.net/xjc/extras";

  public static final String COMPLEX_XML_ADAPTER = "xml-adapter";

  public static final QName COMPLEX_XML_ADAPTER_NAME = new QName(NS, COMPLEX_XML_ADAPTER);

  @Override
  public String getOptionName() {
    return "Xxml-adapter";
  }

  @Override
  public String getUsage() {
    return "  -Xxml-adapter          : allow adding xml adapters to map complexType";
  }

  @Override
  public List<String> getCustomizationURIs() {
    return Collections.singletonList(NS);
  }

  @Override
  public Collection<QName> getCustomizationElementNames() {
    return Collections.singletonList(COMPLEX_XML_ADAPTER_NAME);
  }

  @Override
  public void postProcessModel(Model model, ErrorHandler errorHandler) {

    AdapterInspector inspector = new AdapterInspector();

    for (final CClassInfo classInfo : model.beans().values()) {
      classInfo.accept(inspector);
    }

    // report

    logger.info("scanning for XMLAdapter candidates");

    Map<String, Stack<AdapterCandidate>> types = inspector.getTypeCandidates();
    Map<String, Stack<AdapterCandidate>> fields = inspector.getFieldCandidates();

    for (Map.Entry<String, Stack<AdapterCandidate>> entry : types.entrySet()) {

      if (!entry.getValue().isEmpty()) {

        logger.info("");
        logger.info(format("XMLAdapter: %s", entry.getKey()));
        logger.info("");
        logger.info("Registered Types:");

        for (AdapterCandidate candidate : entry.getValue()) {
          logger.info(format(" [+]: %s", candidate.getName()));
        }

      }

      if (fields.containsKey(entry.getKey())) {

        logger.info("");
        logger.info(" Field Candidates:");

        for (AdapterCandidate candidate : fields.get(entry.getKey())) {
          logger.info(format(" [+]: %s", candidate.getName()));
        }

        logger.info(format(" %s candidate(s) being considered", fields.get(entry.getKey()).size()));

      }

    }

    for (Map.Entry<String, Stack<AdapterCandidate>> entry : fields.entrySet()) {

      if (!types.containsKey(entry.getKey()) && !entry.getValue().isEmpty()) {

        logger.info("");
        logger.info(format("XMLAdapter: %s", entry.getKey()));
        logger.info("");
        logger.info("Field Candidates:");

        for (AdapterCandidate candidate : fields.get(entry.getKey())) {
          logger.info(format(" [+]: %s", candidate.getName()));
        }

        logger.info(format(" %s candidate(s) being considered", fields.get(entry.getKey()).size()));


      }

    }

    logger.info("");

    for (final CClassInfo classInfo : model.beans().values()) {
      postProcessClassInfo(model, inspector, classInfo);
    }

  }

  private void postProcessClassInfo(final Model model, AdapterInspector inspector, final CClassInfo classInfo) {

    for (CPropertyInfo property : classInfo.getProperties()) {
      property.accept(new AdapterCustomizer(model, inspector));
    }

  }

  @Override
  public boolean run(Outline outline, Options opt) {
    return true;
  }

}


