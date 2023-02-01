package io.smallrye.config.examples.expansion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ExampleExpansionBean {
    @Inject
    @ConfigProperty(name = "my.prop")
    private String myProperty;

    public String getMyProperty() {
        return myProperty;
    }
}
