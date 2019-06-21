/*
 * Copyright 2019 Red Hat, Inc.
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
package io.smallrye.config.events;

import java.io.Serializable;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * an Event on config element
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Data @AllArgsConstructor
public class ChangeEvent implements Serializable {
    
    private Type type;
    private String key;
    private Optional<String> oldValue;
    private String newValue;   
    private String fromSource;
    
}
