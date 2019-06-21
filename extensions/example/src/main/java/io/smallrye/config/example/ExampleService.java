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
package io.smallrye.config.example;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Example Service. JAX-RS
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Log
@RequestScoped
@Path("/")
@Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Example service",description = "Just some example")
public class ExampleService {
    @Inject
    private Config config;
    
    @Inject @ConfigProperty(name="ysomekey")
    private Provider<String> ysomekey;
    
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "all", description = "Getting all config keys and values")
    @APIResponse(responseCode = "200", description = "Successful, returning the key-value in JSON format")
    public Response getAll(){
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for(String property:config.getPropertyNames()){
            String value = config.getValue(property, String.class);
            arrayBuilder.add(Json.createObjectBuilder().add(property, value).build());
        }
        return Response.ok(arrayBuilder.build()).build();
    }
    
    @GET
    @Path("/key/{key}")
    @Operation(operationId = "value", description = "Getting the value for a certain config key")
    @APIResponse(responseCode = "200", description = "Successful, returning the value")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getValue(@Parameter(name = "key", description = "The key for this config", required = true, allowEmptyValue = false, example = "some.key")
                                @PathParam("key") String key) {
        return Response.ok(config.getOptionalValue(key, String.class).orElse(null)).build();   
    }
    
    @GET
    @Path("/ysomekey")
    @Operation(operationId = "valueYsomevalue", description = "Getting the value for ysomekey")
    @APIResponse(responseCode = "200", description = "Successful, returning the value")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getValueYsomevalue() {
        return Response.ok(ysomekey.get()).build();   
    }
    
    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "configSources", description = "Getting all the current config sources")
    @APIResponse(responseCode = "200", description = "Successful, returning the config sources in JSON format")
    public Response getConfigSources(){
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for(ConfigSource source:config.getConfigSources()){
            arrayBuilder.add(Json.createObjectBuilder().add(String.valueOf(source.getOrdinal()), source.getName()).build());
        }
        return Response.ok(arrayBuilder.build()).build();
    }
}
