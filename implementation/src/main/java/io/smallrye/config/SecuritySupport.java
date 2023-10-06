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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import io.smallrye.config._private.ConfigLogging;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
class SecuritySupport {
    private SecuritySupport() {
    }

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                ClassLoader tccl = null;
                try {
                    tccl = Thread.currentThread().getContextClassLoader();
                } catch (SecurityException ex) {
                    ConfigLogging.log.failedToRetrieveClassloader(ex);
                }
                return tccl;
            });
        }
    }

    static void setAccessible(AccessibleObject object, boolean flag) {
        if (System.getSecurityManager() == null) {
            object.setAccessible(flag);
        } else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {

                try {
                    object.setAccessible(flag);
                } catch (SecurityException ex) {
                    ConfigLogging.log.failedToSetAccessible(ex, object.toString());
                }
                return null;
            });
        }
    }

    static <T> Constructor<? extends T> getDeclaredConstructor(Class<T> clazz, Class<?>... paramTypes)
            throws NoSuchMethodException {
        if (System.getSecurityManager() == null) {
            return clazz.getDeclaredConstructor(paramTypes);
        } else {
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Constructor<? extends T>>) () -> {
                    Constructor<? extends T> constructor = null;
                    try {
                        constructor = clazz.getDeclaredConstructor(paramTypes);

                    } catch (SecurityException ex) {
                        ConfigLogging.log.failedToRetrieveDeclaredConstructor(ex, clazz.toString(),
                                Arrays.toString(paramTypes));
                    }
                    return constructor;
                });
            } catch (PrivilegedActionException e) {
                Exception e2 = e.getException();
                if (e2 instanceof NoSuchMethodException) {
                    throw (NoSuchMethodException) e2;
                } else {
                    throw new RuntimeException(e2);
                }
            }
        }
    }

}
