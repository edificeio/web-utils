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

import io.vertx.core.MultiMap;

import java.util.ArrayList;
import java.util.List;

public final class XSSUtils {

	private XSSUtils() {}

	public static MultiMap safeMultiMap(MultiMap m) {
		for (String name : m.names()) {
			List<String> values = m.getAll(name);
			List<String> safeValues = new ArrayList<>();
			if (values == null) continue;
			for (String value: values) {
				safeValues.add(stripXSS(value));
			}
			m.set(name, safeValues);
		}
		return m;
	}

	public static String stripXSS(String value) {
		return io.edifice.xss.XSSUtils.stripXSS(value);
	}

}
