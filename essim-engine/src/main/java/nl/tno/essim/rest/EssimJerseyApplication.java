/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */
package nl.tno.essim.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.rest.util.MongoInject;
import nl.tno.essim.rest.util.MongoInjectResolver;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
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
    Logger jullog = Logger.getLogger(EssimJerseyApplication.class.getName());

    public EssimJerseyApplication() {
        // Configure
        packages(
                SimulationService.class.getPackageName(),
//                OpenApiResource.class.getPackageName(),
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
        register(new LoggingFeature(jullog,  LoggingFeature.Verbosity.HEADERS_ONLY));
        property(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, 2000000);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // For payload logging
        Logger.getLogger("org.glassfish.jersey.logging").setLevel(Level.FINE);
        property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE.getName());
        property(LoggingFeature.LOGGING_FEATURE_VERBOSITY, LoggingFeature.Verbosity.PAYLOAD_ANY);

    }
}
