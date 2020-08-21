package io.smallrye.config.examples.mapping;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "server")
public interface Server {
    String host();

    int port();

    @WithConverter(DurationConverter.class)
    Duration timeout();

    @WithName("io-threads")
    int threads();

    Map<String, String> form();

    Optional<Ssl> ssl();

    Optional<Proxy> proxy();

    Log log();

    interface Ssl {
        int port();

        String certificate();

        @WithDefault("TLSv1.3,TLSv1.2")
        List<String> protocols();
    }

    interface Proxy {
        boolean enable();
    }

    interface Log {
        @WithDefault("false")
        boolean enabled();

        @WithDefault(".log")
        String suffix();

        @WithDefault("true")
        boolean rotate();

        @WithDefault("COMMON")
        Pattern pattern();

        enum Pattern {
            COMMON,
            SHORT,
            COMBINED,
            LONG;
        }
    }
}
