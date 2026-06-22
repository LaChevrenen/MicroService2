package infres.ws.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {
    public RestApplication() {
        packages("infres.ws.rest");
        register(JacksonFeature.class);
    }
}
