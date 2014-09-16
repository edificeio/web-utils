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
