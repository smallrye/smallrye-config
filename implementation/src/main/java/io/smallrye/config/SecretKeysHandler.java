package io.smallrye.config;

import io.smallrye.common.annotation.Experimental;

@Experimental("")
public interface SecretKeysHandler {
    String decode(String secret);

    String getName();
}
