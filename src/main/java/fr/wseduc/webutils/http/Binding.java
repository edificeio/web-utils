package fr.wseduc.webutils.http;

import java.util.regex.Pattern;

import fr.wseduc.webutils.security.ActionType;

public class Binding {

	private final HttpMethod method;
	private final Pattern uriPattern;
	private final String serviceMethod;
	private final ActionType actionType;

	public Binding(HttpMethod method, Pattern uriPattern,
			String serviceMethod, ActionType actionType) {
		this.method = method;
		this.uriPattern = uriPattern;
		this.serviceMethod = serviceMethod;
		this.actionType = actionType;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public Pattern getUriPattern() {
		return uriPattern;
	}

	public String getServiceMethod() {
		return serviceMethod;
	}

	public ActionType getActionType() {
		return actionType;
	}

}
