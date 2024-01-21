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

import java.security.AccessController;
import java.security.PrivilegedAction;

public class SecureLoader {

  /**
   * Get the context classloader for the current execution process.
   *
   * @return the context classloader or system one if no context loader
   */
  public static ClassLoader getContextClassLoader() {
    return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null)
        cl = ClassLoader.getSystemClassLoader();
      return cl;
    });
  }

}
