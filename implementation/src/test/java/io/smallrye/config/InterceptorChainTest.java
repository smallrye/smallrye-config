package io.smallrye.config;

import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static org.junit.Assert.assertEquals;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class InterceptorChainTest {
    @Test
    public void chain() {
        final Config config = buildConfig(
                "my.prop", "1", // original property
                "%prof.my.prop", "${%prof.my.prop.profile}", // profile property with expansion
                "%prof.my.prop.profile", "2",
                "my.prop.relocate", "3", // relocation
                "%prof.my.prop.relocate", "4", // profile with relocation
                SMALLRYE_PROFILE, "prof" // profile to use
        );
        assertEquals("4", config.getValue("my.prop", String.class));
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(
                        new RelocateConfigSourceInterceptor(s -> {
                            if (s.contains("my.prop.profile")) {
                                return "my.prop.relocate";
                            }
                            return s;
                        }))
                // Add the Profile Interceptor again because relocation may require a new search in the profiles
                .withInterceptorFactories(
                        new ConfigSourceInterceptorFactory() {
                            @Override
                            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                                return new ProfileConfigSourceInterceptor(context);
                            }

                            @Override
                            public OptionalInt getPriority() {
                                return OptionalInt.of(399);
                            }
                        })
                .build();
    }
}
