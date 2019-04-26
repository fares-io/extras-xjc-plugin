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

import com.sun.tools.xjc.model.CCustomizable;

public abstract class AdapterCandidate<T extends CCustomizable> {

  private final T element;

  public AdapterCandidate(T element) {

    if (element == null) {
      throw new IllegalArgumentException("customization candidate must not be null");
    }

    this.element = element;

  }

  public T getElement() {
    return element;
  }

  public abstract String getName();

}
