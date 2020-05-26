package io.smallrye.config.examples.expansion;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExampleExpansionTest {
    @Test
    public void expand() {
        final String myProp = ExampleExpansion.getMyProp();
        assertEquals("expanded", myProp);
    }
}
