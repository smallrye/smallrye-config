package io.streamzi.zkMicroProfileConfig.rest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@ApplicationScoped
@Path("/zkmpcs")
public class HelloWorldEndpoint {

    Logger logger = Logger.getLogger(HelloWorldEndpoint.class.getName());

    @Inject
    @ConfigProperty(name = "intProp", defaultValue = "8")
    int intProp;

    @Inject
    @ConfigProperty(name = "strProp", defaultValue = "default_value")
    String strProp;

    @GET
    @Path("/injectedValues")
    @Produces("text/plain")
    public Response getInjected() {
        logger.info("getInjected()");

        String values = "*** Injected Property Values ***\n" +
                "strProp: " + strProp + "\n" +
                "intProp: " + intProp;

        return Response.ok(values).build();
    }

    @GET
    @Path("/values")
    @Produces("text/plain")
    public Response getProgramatically() {
        logger.info("doGet()");

        final Config cfg = ConfigProvider.getConfig();
        final int refreshedIntProp = cfg.getValue("intProp", Integer.class);
        final String refreshedStrProp = cfg.getValue("strProp", String.class);

        String values = "*** Retrieved Property Values ***\n" +
                "refreshedStrProp: " + refreshedStrProp + "\n" +
                "refreshedIntProp: " + refreshedIntProp;

        return Response.ok(values).build();
    }
}