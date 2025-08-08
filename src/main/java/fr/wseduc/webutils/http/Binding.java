/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.wseduc.webutils.http;

import java.util.regex.Pattern;

import fr.wseduc.webutils.security.ActionType;

public class Binding {

	private final HttpMethod method;
	private final Pattern uriPattern;
	private final String serviceMethod;
	private final ActionType actionType;
	private String override;

	public Binding(HttpMethod method, Pattern uriPattern,
			String serviceMethod, ActionType actionType,
				   String override) {
		this.method = method;
		this.uriPattern = uriPattern;
		this.serviceMethod = serviceMethod;
		this.actionType = actionType;
		this.override = override;
	}

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

	public String getOverride() {
		return override != null ? override : "";
	}

	public void setOverride(String override) {
		this.override = override;
	}
}
