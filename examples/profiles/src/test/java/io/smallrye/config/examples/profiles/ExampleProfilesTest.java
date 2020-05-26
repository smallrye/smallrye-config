package io.smallrye.config.examples.profiles;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExampleProfilesTest {
    @Test
    public void profiles() {
        final String myProp = ExampleProfiles.getMyProp();
        assertEquals("production", myProp);
    }
}
