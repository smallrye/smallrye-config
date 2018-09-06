package io.smallrye.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Provide string interpolation of mp-config variables
 * @author kg6zvp
 */
public class StringInterpolator {
    public static final Pattern configPattern = Pattern.compile("\\$\\{(.*?)\\}");

    public static String interpolate(String value) {
        Matcher m = configPattern.matcher(value);
        Map<String, String> vars = new HashMap<>();
        while(m.find()) {
            String varName = m.group(1);
            vars.put(varName, ConfigProvider.getConfig().getValue(varName, String.class));
        }
        String outputString = value;
        for(String key : vars.keySet()) {
            outputString = outputString.replaceAll(Pattern.quote("${" + key + "}"), vars.get(key));
        }
        return outputString;
    }
}
