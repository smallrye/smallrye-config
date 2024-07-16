/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@Deprecated(forRemoval = true)
class SecuritySupport {
    private SecuritySupport() {
    }

    static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    static void setAccessible(AccessibleObject object, boolean flag) {
        object.setAccessible(flag);
    }

    static <T> Constructor<? extends T> getDeclaredConstructor(Class<T> clazz, Class<?>... paramTypes)
            throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(paramTypes);
    }
}
