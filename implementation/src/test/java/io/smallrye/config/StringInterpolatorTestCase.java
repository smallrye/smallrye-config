package io.smallrye.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author kg6zvp
 */
public class StringInterpolatorTestCase {
    @Test
    public void testInterpolation() {
        System.setProperty("prop.value", "twinkie");
        System.setProperty("other_value", "pretzel");
        assertEquals("twinkie-settings-pretzel", StringInterpolator.interpolate("${prop.value}-settings-${other_value}"));
    }
}
