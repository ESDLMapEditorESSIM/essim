package nl.tno.essim.rest.application;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.rest.SimulationService;
import nl.tno.essim.rest.util.MongoInject;
import nl.tno.essim.rest.util.MongoInjectResolver;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.annotation.security.DenyAll;
import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@DenyAll
@ApplicationPath("/essim")
@OpenAPIDefinition(
        servers = @Server(
                url = "/",
                description = "ESSIM API"
        ),
        info = @Info(
                title = "ESSIM API",
                version = "Main",
                contact = @Contact(name = "TNO", url = "https://energytransition.gitbook.io/esdl/esdl-based-tools/essim")
        ))
public class EssimJerseyApplication extends ResourceConfig {

    public EssimJerseyApplication() {
        super(Set.of(EssimJerseyApplication.class));
        // Configure
        packages(
                SimulationService.class.getPackageName(),
                OpenApiResource.class.getPackageName(),
                MongoInjectResolver.class.getPackageName()
        );
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(MongoInjectResolver.class)
                        .to(new TypeLiteral<InjectionResolver<MongoInject>>() {
                        }).in(Singleton.class);
            }
        });
        register(MultiPartFeature.class);
        property(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 2000000);

        // For payload logging
        Logger.getLogger("org.glassfish.jersey.logging").setLevel(Level.FINE);
        property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE.getName());
        property(LoggingFeature.LOGGING_FEATURE_VERBOSITY, LoggingFeature.Verbosity.PAYLOAD_ANY);

    }
}
