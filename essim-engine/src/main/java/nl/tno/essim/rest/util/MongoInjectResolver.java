package nl.tno.essim.rest.util;

import nl.tno.essim.mongo.MongoBackend;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

public class MongoInjectResolver implements InjectionResolver<MongoInject> {
    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        return MongoBackend.getInstance();
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
