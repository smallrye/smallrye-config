package io.smallrye.config;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author kg6zvp
 */
public class StringInterpolatorTest {
    @Before
    public void beforeEach() {
        System.setProperty("prop.value", "twinkie");
        System.setProperty("other_value", "pretzel");
    }

    @Test
    public void testBasicInterpolation() {
        assertEquals("twinkie-settings-pretzel", StringInterpolator.interpolate("${prop.value}-settings-${other_value}"));
    }
    
    @Test
    public void testEscapedInterpolation() {
        assertEquals("${prop.value}pretzel", StringInterpolator.interpolate("\\${prop.value}${other_value}"));
    }

    @After
    public void afterEach() {
        System.clearProperty("prop.value");
        System.clearProperty("other_value");
    }
}
