package nl.tno.essim.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class EssimApplication extends Application {
	
	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> resources = new HashSet<>();

		resources.add(RestSimulation.class);
		resources.add(DebugMapper.class);

		return resources;
	}
}
