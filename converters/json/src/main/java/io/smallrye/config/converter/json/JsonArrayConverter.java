package io.smallrye.config.converter.json;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Converts a json string to a JSonArray
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class JsonArrayConverter implements Converter<JsonArray> {

    @Override
    public JsonArray convert(String input) {
        if (isNullOrEmpty(input))
            return null;

        try (JsonReader jsonReader = Json.createReader(new StringReader(input))) {
            return jsonReader.readArray();
        }
    }

    private boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

}
