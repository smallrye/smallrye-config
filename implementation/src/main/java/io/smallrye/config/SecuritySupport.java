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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.logging.Logger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class SecuritySupport {
    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    private SecuritySupport() {
    }

    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            ClassLoader tccl = null;
            try {
                tccl = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException ex) {
                LOG.warn("Unable to get context classloader instance.", ex);
            }
            return tccl;
        });
    }

}
