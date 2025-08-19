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

package fr.wseduc.webutils.security;

public class SecuredAction {

	private final String name;
	private final String displayName;
	private final String type;
	private final String right;

	public SecuredAction(String name, String displayName, String type, String override) {
		this.name = name;
		this.displayName = displayName;
		this.type = type;
        this.right = override;
    }

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getType() {
		return type;
	}

	public String getRight() {
		return right;
	}

}
