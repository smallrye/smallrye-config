package io.smallrye.config;

import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

public class InterceptorChainTest {
    @Test
    public void chain() {
        final Config config = buildConfig(
                "my.prop", "1", // original property
                "%my.prop.profile", "2", // profile property with expansion
                "%prof.my.prop.profile", "3",
                "my.prop.relocate", "4", // relocation
                "%prof.my.prop.relocate", "${%prof.my.prop.profile}", // profile with relocation
                SMALLRYE_PROFILE, "prof" // profile to use
        );
        assertEquals("3", config.getValue("my.prop", String.class));
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(
                        new RelocateConfigSourceInterceptor(s -> s.replaceAll("my\\.prop", "my.prop.relocate")))
                .build();
    }
}
