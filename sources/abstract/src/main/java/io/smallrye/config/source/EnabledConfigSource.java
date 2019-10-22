package io.smallrye.config.source;

import java.util.Collections;
import java.util.Map;

/**
 * A config source that can be disabled by class or by instance (all vs each).
 * <p>
 * Instance keys take precedence over class keys, so individual sources can be
 * turned back on if all sources have been turned off.
 * <p>
 * Config sources are enabled by default.
 *
 * @author <a href="mailto:dpmoore@acm.org">Derek P. Moore</a>
 */
public abstract class EnabledConfigSource extends SelfConfiguringConfigSource {
    
    /**
     * Called to return the properties in this config source when it is enabled
     * 
     * @return the map containing the properties in this config source
     */
    abstract protected Map<String, String> getPropertiesIfEnabled();

    /**
     * Return the properties, unless disabled return empty
     * 
     * @return the map containing the properties in this config source or empty
     *         if disabled
     */
    @Override
    public Map<String, String> getProperties() {
        return isEnabled() ? getPropertiesIfEnabled() : Collections.emptyMap();
    }

    protected boolean isEnabled() {
        boolean isSet = configPropertyIsSet(getPrefixWithKey(ENABLED));
        if (isSet) {
            return config.getOptionalValue(getPrefixWithKey(ENABLED), Boolean.class).orElse(true);
        }
        return true; // default;

    }

    protected String getClassKeyPrefix() {
        return getClass().getSimpleName();
    }

    protected boolean configPropertyIsSet(String key) {
        for (String name : config.getPropertyNames()) {
            if (name.equals(key))
                return true;
        }
        return false;
    }

    private static final String ENABLED = "enabled";
}
