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

import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;

/**
 * Represents adapter customization inherited from a property type.
 */
public class AdapterCPropertyInfoRefCandidate extends AdapterCandidate<CPropertyInfo> {

  private final CTypeInfo ref;

  /**
   * Creates a candidate for a property and its referenced type.
   *
   * @param element the customized XJC property
   * @param ref     the referenced XJC type
   */
  public AdapterCPropertyInfoRefCandidate(CPropertyInfo element, CTypeInfo ref) {
    super(element);
    this.ref = ref;
  }

  @Override
  public String getName() {
    return getElement().displayName() + " ref " + getRef().getType().fullName();
  }

  /**
   * Returns the type that declares the adapter customization.
   *
   * @return the referenced XJC type
   */
  public CTypeInfo getRef() {
    return ref;
  }

}
