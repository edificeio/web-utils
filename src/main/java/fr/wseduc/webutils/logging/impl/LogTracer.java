package fr.wseduc.webutils.logging.impl;


import fr.wseduc.webutils.logging.Tracer;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class LogTracer implements Tracer {

	private static final Logger log = LoggerFactory.getLogger(LogTracer.class);

	@Override
	public void setName(String name) {

	}

	@Override
	public void info(String logMessage) {
		log.info(logMessage);
	}

	@Override
	public void error(String logMessage) {
		log.error(logMessage);
	}

}
