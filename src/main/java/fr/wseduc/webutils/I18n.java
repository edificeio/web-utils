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

package fr.wseduc.webutils;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;


/*
 * Dummy implementation
 */
public class I18n {

	private Logger log;
	private final static String messagesDir = "./i18n";
	private final static Locale defaultLocale = Locale.FRENCH;
	private Map<Locale, JsonObject> messages = new HashMap<>();

	private I18n(){}

	private static class I18nHolder {
		private static final I18n instance = new I18n();
	}

	public static I18n getInstance() {
		return I18nHolder.instance;
	}

	public void init(Container container, Vertx vertx) {
		try {
			log = container.logger();
			if (vertx.fileSystem().existsSync(messagesDir)) {
				for(String path : vertx.fileSystem().readDirSync(messagesDir)) {
					if (vertx.fileSystem().propsSync(path).isRegularFile()) {
						Locale l = Locale.forLanguageTag(new File(path).getName().split("\\.")[0]);
						JsonObject jo = new JsonObject(vertx.fileSystem().readFileSync(path).toString());
						messages.put(l,jo);
					}
				}
			} else {
				log.warn("I18n directory " + messagesDir + " doesn't exist.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public String translate(String key, String acceptLanguage, String... args) {
		return translate(key, getLocale(acceptLanguage), args);
	}

	public String translate(String key, Locale locale, String... args) {
		JsonObject bundle = messages.get(locale) != null ? messages.get(locale) : messages.get(defaultLocale);
		if (bundle == null) {
			return key;
		}
		String text =  bundle.getString(key) != null ? bundle.getString(key) : key;
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				text = text.replaceAll("\\{" + i + "\\}", args[i]);
			}
		}
		return text;
	}

	public JsonObject load(String acceptLanguage) {
		Locale l = getLocale(acceptLanguage);
		JsonObject bundle = messages.get(l) != null ? messages.get(l) : messages.get(defaultLocale);
		if (bundle == null) {
			bundle = messages.get(defaultLocale);
		}
		return bundle;
	}

	/* Dummy implementation. Just use the first langage option ...
	 * Header example : "Accept-Language:fr,en-us;q=0.8,fr-fr;q=0.5,en;q=0.3"
	 */
	private Locale getLocale(String acceptLanguage) {
		if (acceptLanguage == null) {
			acceptLanguage = "fr";
		}
		String[] langs = acceptLanguage.split(",");
		return Locale.forLanguageTag(langs[0].split("-")[0]);
	}

	public static String acceptLanguage(HttpServerRequest request) {
		String acceptLanguage = request.headers().get("Accept-Language");
		return acceptLanguage != null ? acceptLanguage : "fr";
	}

	public void add(Locale locale, JsonObject keys) {
		JsonObject m = messages.get(locale);
		if (m == null) {
			messages.put(locale, keys);
		} else {
			m.mergeIn(keys);
		}
	}

}
