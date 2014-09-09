package fr.wseduc.webutils.logging;

public interface Tracer {

	void setName(String name);

	void info(String logMessage);

	void error(String logMessage);

}
