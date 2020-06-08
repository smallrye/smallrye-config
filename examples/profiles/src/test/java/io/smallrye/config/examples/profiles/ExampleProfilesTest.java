package io.smallrye.config.examples.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExampleProfilesTest {
    @Test
    public void profiles() {
        final String myProp = ExampleProfiles.getMyProp();
        assertEquals("production", myProp);
    }
}
