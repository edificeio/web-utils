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

package fr.wseduc.webutils.collections;

import java.util.Arrays;

public class Joiner {

	private final String separator;

	private Joiner(String separator) {
		this.separator = separator;
	}

	public static Joiner on(String separator) {
		if (separator != null) {
			return new Joiner(separator);
		}
		return null;
	}

	public String join(Iterable<?> items) {
		StringBuilder sb = new StringBuilder();
		for (Object item: items) {
			sb.append(separator).append(item.toString());
		}
		return (sb.length() > separator.length()) ? sb.substring(separator.length()) : "";
	}

	public String join(Object[] items) {
		return join(Arrays.asList(items));
	}

}
