/*
 * Copyright 2020 Red Hat, Inc.
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
package io.smallrye.config.test.converter;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ConverterBean {

    @Inject
    @ConfigProperty(name = "myInt", defaultValue = "1")
    private int myInt;

    @Inject
    @ConfigProperty(name = "myInteger", defaultValue = "1")
    private Integer myInteger;

    public int getInt() {
        return myInt;
    }

    public Integer getInteger() {
        return myInteger;
    }
}