package fr.wseduc.webutils.logging;

import fr.wseduc.webutils.logging.impl.LogTracer;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TracerFactory {

	private static final ConcurrentMap<String, Tracer> tracers = new ConcurrentHashMap<>();

	public static Tracer getTracer(String name) {
		Tracer tracer = tracers.get(name);
		if (tracer == null) {
			ServiceLoader<Tracer> t = ServiceLoader.load(Tracer.class);
			if (t.iterator().hasNext()) {
				tracer = t.iterator().next();
			} else {
				tracer = new LogTracer();
			}
			tracer.setName(name);
			tracers.putIfAbsent(name, tracer);
		}
		return tracer;
	}

}
