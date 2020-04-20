package io.smallrye.config.examples.expansion;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
