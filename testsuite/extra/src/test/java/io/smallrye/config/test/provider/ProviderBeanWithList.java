package io.smallrye.config.test.provider;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ProviderBeanWithList {

    @Inject
    @ConfigProperty(name = "objectIds", defaultValue = "")
    Provider<List<String>> objectdIds;

    @Inject
    @ConfigProperty(name = "numbers", defaultValue = "")
    Provider<List<Integer>> numbers;
}
