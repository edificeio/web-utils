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

import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.security.NoSuchAlgorithmException;
import java.util.*;

public class JsonUtils {

	public enum HashAlgorithm { SHA256, MD5 }

	public static Map<String, Object> convertMap(io.vertx.core.json.JsonObject json) {
		final Map<String, Object> converted = new LinkedHashMap<>(json.size());
		for (Map.Entry<String, Object> entry : json.getMap().entrySet()) {
			Object obj = entry.getValue();
			if (obj instanceof io.vertx.core.json.JsonObject) {
				io.vertx.core.json.JsonObject jm = (io.vertx.core.json.JsonObject) obj;
				converted.put(entry.getKey(), convertMap(jm));
			} else if (obj instanceof io.vertx.core.json.JsonArray) {
				io.vertx.core.json.JsonArray list = (io.vertx.core.json.JsonArray) obj;
				converted.put(entry.getKey(), convertList(list));
			} else {
				converted.put(entry.getKey(), obj);
			}
		}
		return converted;
	}

	public static List<Object> convertList(io.vertx.core.json.JsonArray list) {
		final List<Object> arr = new ArrayList<>(list.size());
		for (Object obj : list) {
			if (obj instanceof io.vertx.core.json.JsonObject) {
				arr.add(convertMap((io.vertx.core.json.JsonObject) obj));
			} else if (obj instanceof io.vertx.core.json.JsonArray) {
				arr.add(convertList((io.vertx.core.json.JsonArray) obj));
			} else {
				arr.add(obj);
			}
		}
		return arr;
	}

	public static String checksum(JsonObject object) throws NoSuchAlgorithmException {
		return checksum(object, HashAlgorithm.SHA256);
	}

	public static String checksum(JsonObject object, HashAlgorithm hashAlgorithm) throws NoSuchAlgorithmException {
		if (object == null) {
			return null;
		}
		final JsonObject j = sortJsonObject(object);
		switch (hashAlgorithm) {
			case MD5:
				return Md5.hash(j.encode());
			default:
				return Sha256.hash(j.encode());
		}
	}

	private static JsonObject sortJsonObject(JsonObject object) {
		final TreeSet<String> sorted = new TreeSet<>(object.fieldNames());
		final JsonObject j = new JsonObject();
		for (String attr : sorted) {
			j.put(attr, object.getValue(attr));
		}
		return j;
	}

	public static String checksum(JsonArray object) throws NoSuchAlgorithmException {
		return checksum(object, HashAlgorithm.SHA256);
	}

	public static String checksum(JsonArray object, HashAlgorithm hashAlgorithm) throws NoSuchAlgorithmException {
		if (object == null) {
			return null;
		}
		final TreeSet<String> sorted = new TreeSet<>();
		for (Object o: object) {
			if (o instanceof JsonObject) {
				sorted.add(sortJsonObject((JsonObject) o).encode());
			} else if (o != null) {
				sorted.add(o.toString());
			}
		}
		final String value = Joiner.on(",").join(sorted);
		switch (hashAlgorithm) {
			case MD5:
				return Md5.hash(value);
			default:
				return Sha256.hash(value);
		}
	}

}
