package io.smallrye.config.examples.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExampleProfilesTest {
    @Test
    void profiles() {
        final String myProp = ExampleProfiles.getMyProp();
        assertEquals("production", myProp);
    }
}
