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

import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class I18n {

	private static final Logger log = LoggerFactory.getLogger(I18n.class);
	private final static String messagesDir = FileResolver.absolutePath("i18n");
	private final static Locale defaultLocale = Locale.ENGLISH;
	private final static Locale defaultLocale2 = Locale.FRENCH;
	public final static String DEFAULT_DOMAIN = "default-domain";
	private Map<String, Map<Locale, JsonObject>> messagesByDomains = new HashMap<>();
	private Map<String, Map<Locale, JsonObject>> messagesByThemes = new HashMap<>();
	/** Translations overriding those of the files, replaced as a whole when they change. */
	private volatile I18nOverrides overrides = I18nOverrides.EMPTY;

	public I18n(){}

	private static class I18nHolder {
		private static final I18n instance = new I18n();
	}

	public static I18n getInstance() {
		return I18nHolder.instance;
	}

	public void init(Vertx vertx) {
		try {
			if (vertx.fileSystem().existsBlocking(messagesDir)) {
				Map<Locale, JsonObject> messages = messagesByDomains.get(DEFAULT_DOMAIN);
				if (messages == null) {
					messages = new HashMap<>();
					messagesByDomains.put(DEFAULT_DOMAIN, messages);
				}
				for(String path : vertx.fileSystem().readDirBlocking(messagesDir)) {
					if (vertx.fileSystem().propsBlocking(path).isRegularFile()) {
						Locale l = Locale.forLanguageTag(new File(path).getName().split("\\.")[0]);
						JsonObject jo = new JsonObject(vertx.fileSystem().readFileBlocking(path).toString());
						messages.put(l,jo);
					}
				}
			} else {
				log.warn("I18n directory " + messagesDir + " doesn't exist.");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public String translate(String key, String domain, String acceptLanguage, String... args) {
		return translate(key, domain, getLocale(acceptLanguage), args);
	}

	@Deprecated
	public String translate(String key, Locale locale, String... args) {
		return translate(key, DEFAULT_DOMAIN, locale, args);
	}

	public String translate(String key, String domain, Locale locale, String... args) {
		return translate(key, domain, null, locale, args);
	}
		
	/**
	 * Translates a key for the requests made at a domain, without tenant: the overrides of the
	 * domain apply (see {@link #translate(String, String, String, String, Locale, String...)}).
	 */
	public String translate(String key, String domain, String theme, Locale locale, String... args) {
		return translate(key, domain, null, theme, locale, args);
	}

	/**
	 * Translates a key: its override if any (see {@link I18nOverrides}), else its translation in the
	 * files of the theme, then of the domain, then of {@value #DEFAULT_DOMAIN}, else the key itself.
	 *
	 * @param domain   domain of the request
	 * @param tenantId tenant of the user, null if unknown
	 * @param theme    theme of the user, null if unknown
	 * @param args     values of the {0}, {1}... placeholders of the translation
	 */
	public String translate(String key, String domain, String tenantId, String theme, Locale locale, String... args) {
		if (key == null) return "";
		final String overridden = overrides.find(key, tenantId, domain, theme, locale);
		if (overridden != null) {
			return format(overridden, args);
		}
		return translateFromFiles(key, domain, theme, locale, args);
	}

	private String translateFromFiles(String key, String domain, String theme, Locale locale, String... args) {
		Map<Locale, JsonObject> messages = getMessagesMap(theme != null ? theme : domain, theme != null); // Theme gets precedence over domain, domain is a fallback

		if (messages == null) {
			return theme != null ? translateFromFiles(key, domain, null, locale, args) : key;
		}
		JsonObject bundle = messages.get(locale) != null ? messages.get(locale) : messages.get(defaultLocale);
		if (bundle == null) {
			return theme != null ? translateFromFiles(key, domain, null, locale, args) : key;
		}
		String text = bundle.getString(key);
		if(text != null)
		{
			text = format(text, args);
		}
		else
			text = theme != null ? translateFromFiles(key, domain, null, locale, args) : key;
		return text;
	}

	/** Replaces the {0}, {1}... placeholders of a translation by the given values. */
	private static String format(String text, String... args) {
		if (args.length > 0) {
			try {
				for (int i = 0; i < args.length; i++) {
					text = text.replaceAll("\\{" + i + "\\}", args[i]);
				}
			} catch (RuntimeException e) {
				log.error("Error replacing i18n variable", e);
			}
		}
		return text;
	}

	/**
	 * Replaces the translations overriding those of the files, e.g. when they changed. The given
	 * snapshot is used as is by the translations made from now on.
	 */
	public void setOverrides(I18nOverrides overrides) {
		this.overrides = overrides == null ? I18nOverrides.EMPTY : overrides;
	}

	public I18nOverrides getOverrides() {
		return overrides;
	}

	private Map<Locale, JsonObject> getMessagesMap(String domain) {
		return getMessagesMap(domain, false);
	}

	private Map<Locale, JsonObject> getMessagesMap(String domain, Boolean byTheme) {
		final Map<String, Map<Locale, JsonObject>> map = byTheme ? messagesByThemes : messagesByDomains;
		Map<Locale, JsonObject> messages = map.get(domain);
		if (messages == null) {
			messages = messagesByDomains.get(DEFAULT_DOMAIN);
		}
		return messages;
	}

	@Deprecated
	public JsonObject load(String acceptLanguage) {
		return load(acceptLanguage, DEFAULT_DOMAIN);
	}

	@Deprecated
	public JsonObject load(String acceptLanguage, String domain) {
		Map<Locale, JsonObject> messages = getMessagesMap(domain);
		if (messages == null) {
			return new JsonObject();
		}
		Locale l = getLocale(acceptLanguage);
		JsonObject bundle = messages.get(l) != null ? messages.get(l) : messages.get(defaultLocale);
		if (bundle == null) {
			bundle = messages.get(defaultLocale2);
		}
		return withOverrides(bundle, null, domain, null, l);
	}

	public JsonObject load(HttpServerRequest request) {
		final String domain = Renders.getHost(request);
		final String acceptLanguage = I18n.acceptLanguage(request);
		String themeName = I18n.getTheme(request);

		Map<Locale, JsonObject> messages = themeName != null ? getMessagesMap(themeName, true) : getMessagesMap(domain);
		if (messages == null) {
			return new JsonObject();
		}
		Locale l = getLocale(acceptLanguage);
		JsonObject bundle = messages.get(l) != null ? messages.get(l) : messages.get(defaultLocale);
		if (bundle == null) {
			bundle = messages.get(defaultLocale);
		}
		return withOverrides(bundle, getTenantId(request), domain, themeName, l);
	}

	/** The translations themselves without overrides, else a copy with the overrides applying on top. */
	private JsonObject withOverrides(JsonObject bundle, String tenantId, String domain, String theme, Locale locale) {
		final I18nOverrides currentOverrides = overrides;
		return currentOverrides.isEmpty() ? bundle : currentOverrides.applyTo(bundle, tenantId, domain, theme, locale);
	}

	/* Dummy implementation. Just use the first langage option ...
	 * Header example : "Accept-Language:fr,en-us;q=0.8,fr-fr;q=0.5,en;q=0.3"
	 */
	public static Locale getLocale(String acceptLanguage) {
		if (acceptLanguage == null) {
			acceptLanguage = "fr";
		}
		String[] langs = acceptLanguage.split(",");
		return Locale.forLanguageTag(langs[0].split("-")[0]);
	}

	public static String acceptLanguage(HttpServerRequest request) {
		final String acceptLanguage = request.headers().get("Accept-Language") != null ?
				request.headers().get("Accept-Language") : "fr";
		if ("true".equals(request.headers().get("X-Force-Language"))) {
			return acceptLanguage;
		}
		if (request instanceof SecureHttpServerRequest) {
			JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if (session != null && session.getJsonObject("cache") != null &&
					session.getJsonObject("cache").getJsonObject("preferences") != null &&
					Utils.isNotEmpty(session.getJsonObject("cache").getJsonObject("preferences").getString("language"))) {
				try {
					JsonObject language = new JsonObject(session.getJsonObject("cache").getJsonObject("preferences")
							.getString("language"));
					return language.getString(DEFAULT_DOMAIN, acceptLanguage);
				} catch (DecodeException e) {
					log.error("Error getting language in cache.", e);
				}
			}
		}
		return acceptLanguage;
	}

	/**
	 * The tenant of the user making the request, for the translation overrides of its tenant to apply:
	 * the one in its session (set at authentication), else the one of the X-Tenant-Id header.
	 *
	 * @return the id of the tenant, null if unknown
	 */
	public static String getTenantId(HttpServerRequest request) {
		if (request instanceof SecureHttpServerRequest) {
			final JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if (session != null && Utils.isNotEmpty(session.getString("tenantId"))) {
				return session.getString("tenantId");
			}
		}
		final String tenantId = request.headers() != null ? request.headers().get("X-Tenant-Id") : null;
		return Utils.isNotEmpty(tenantId) ? tenantId : null;
	}

	/**
	 * Lookup the theme cached in session.
	 * @param request
	 * @return The name of the theme, or null if no theme was found in cache.
	 */
	//FIXME Why is this method here ? It does not belong to "i18n" 
	public static String getTheme(HttpServerRequest request)
	{
		if (request instanceof SecureHttpServerRequest) {
			do {// Check if session cache was initialized.
				final JsonObject session = ((SecureHttpServerRequest) request).getSession();
				if (session == null)
					break;
				final JsonObject cache = session.getJsonObject("cache");
				if (cache == null)
					break;
				final JsonObject preferences = cache.getJsonObject("preferences");
				if (preferences == null)
					break;
				final String theme = preferences.getString("theme");

				// Return the theme, if found.
				if (Utils.isNotEmpty(theme))
					return theme;
			} while(false);
		}
		else
		{
			if(request.headers() != null && request.headers().get("X-ENT-Theme") != null)
				return request.headers().get("X-ENT-Theme");
		}
		// return null if theme was not found or request was not secured.
		return null;
	}

	@Deprecated
	public void add(Locale locale, JsonObject keys) {
		add(DEFAULT_DOMAIN, locale, keys);
	}

	public void add(String domain, Locale locale, JsonObject keys) {
		add(domain, locale, keys, false);
	}

	public void add(String domain, Locale locale, JsonObject keys, Boolean byTheme) {
		Map<Locale, JsonObject> messages = byTheme ?
				messagesByThemes.getOrDefault(domain, messagesByDomains.get(domain)) : messagesByDomains.get(domain);
		if (messages == null) {
			HashMap<Locale, JsonObject> defaultMessages = (HashMap<Locale, JsonObject>)
					messagesByDomains.get(DEFAULT_DOMAIN);
			if (defaultMessages == null) return;
			messages = new HashMap<>();
			for(Locale l : defaultMessages.keySet()){
				messages.put(l, defaultMessages.get(l).copy());
			}
			final Map<String, Map<Locale, JsonObject>> map = byTheme ? messagesByThemes : messagesByDomains;
			map.put(domain, messages);
		}
		JsonObject m = messages.get(locale);
		if (m == null) {
			messages.put(locale, keys);
		} else {
			m.mergeIn(keys);
		}
	}

	public JsonArray getLanguages(String domain) {
		final Map<Locale, JsonObject> messages = getMessagesMap(domain);
		final JsonArray languages = new JsonArray();
		if (messages != null) {
			for (Locale l : messages.keySet()) {
				languages.add(l.getLanguage());
			}
		}
		return languages;
	}

	/**
	 * Initializes translations by directly setting the message map for a domain.
	 * This bypasses the standard file-based initialization process.
	 * 
	 * @param messages Map of Locale to translation JsonObjects to use
	 * @param domain Domain name to initialize (will use DEFAULT_DOMAIN if null)
	 * @return The I18n instance for method chaining
	 */
	public I18n initializeMessages(Map<Locale, JsonObject> messages, String domain) {
		if (messages == null) {
			log.warn("Attempted to initialize null messages map");
			return this;
		}
		
		final String targetDomain = (domain == null || domain.isEmpty()) ? DEFAULT_DOMAIN : domain;
		log.info("Directly initializing {} translations for domain: {}", messages.size(), targetDomain);
		
		// Create a defensive copy to avoid external modification
		final Map<Locale, JsonObject> messagesCopy = new HashMap<>();
		messagesCopy.putAll(messages);
		
		// Replace existing messages for the domain
		messagesByDomains.put(targetDomain, messagesCopy);
		
		return this;
	}

	/**
	 * Initializes translations for the default domain.
	 * This is a convenience method that calls initializeMessages with DEFAULT_DOMAIN.
	 * 
	 * @param messages Map of Locale to translation JsonObjects to use
	 * @return The I18n instance for method chaining
	 */
	public I18n initializeMessages(Map<Locale, JsonObject> messages) {
		return initializeMessages(messages, DEFAULT_DOMAIN);
	}

}
