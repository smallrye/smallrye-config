package io.smallrye.config.source.kubernetes.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class ChangeEventNotifier {

    @Inject
    private Event<ChangeConfigEvent> broadcaster;

    private static ChangeEventNotifier INSTANCE;
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        INSTANCE = this;
    }

    public static ChangeEventNotifier getInstance() {
        return INSTANCE;
    }

    public void fire(ChangeConfigEvent changeConfigEvent) {
        broadcaster.fire(changeConfigEvent);
    }
}
