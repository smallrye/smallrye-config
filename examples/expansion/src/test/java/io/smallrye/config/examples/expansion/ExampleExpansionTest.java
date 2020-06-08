package io.smallrye.config.examples.expansion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExampleExpansionTest {
    @Test
    public void expand() {
        final String myProp = ExampleExpansion.getMyProp();
        assertEquals("expanded", myProp);
    }
}
