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

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable snapshot of translations overriding those of the i18n files, defined per tenant and
 * per domain (see {@link I18n#setOverrides(I18nOverrides)}).
 * <p>
 * Overrides apply to a language (all languages when none is given) and to a theme (all themes when
 * none is given). A translation is looked up, from the most to the least specific scope:
 * <ol>
 *     <li>in the overrides of the tenant of the user, if known;</li>
 *     <li>in those of the domain of the request;</li>
 *     <li>in those of {@value I18n#DEFAULT_DOMAIN};</li>
 * </ol>
 * and within each scope, for the language then for all languages, for the theme then for all
 * themes: (theme, language), (any theme, language), (theme, any language), (any theme, any language).
 */
public final class I18nOverrides {

	/** Language or theme of the overrides applying to all languages or themes. */
	private static final String ANY = "";

	public static final I18nOverrides EMPTY = new I18nOverrides(
			Collections.<String, Map<String, Map<String, JsonObject>>>emptyMap(),
			Collections.<String, Map<String, Map<String, JsonObject>>>emptyMap());

	/** Scope (tenant id or domain) → theme → language → overridden translations */
	private final Map<String, Map<String, Map<String, JsonObject>>> byTenant;
	private final Map<String, Map<String, Map<String, JsonObject>>> byDomain;

	private I18nOverrides(Map<String, Map<String, Map<String, JsonObject>>> byTenant,
						  Map<String, Map<String, Map<String, JsonObject>>> byDomain) {
		this.byTenant = byTenant;
		this.byDomain = byDomain;
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean isEmpty() {
		return byTenant.isEmpty() && byDomain.isEmpty();
	}

	/**
	 * @param tenantId tenant of the user, null if unknown
	 * @param domain   domain of the request (a port is ignored), null if unknown
	 * @param theme    theme of the user, null if unknown
	 * @return the overridden translation of the key, null if it is not overridden
	 */
	public String find(String key, String tenantId, String domain, String theme, Locale locale) {
		if (key == null || isEmpty()) {
			return null;
		}
		final List<String> variants = variants(theme, locale);
		for (Map<String, Map<String, JsonObject>> scope : scopes(tenantId, domain)) {
			for (int i = 0; i < variants.size(); i += 2) {
				final JsonObject translations = get(scope, variants.get(i), variants.get(i + 1));
				if (translations != null) {
					final String text = translations.getString(key);
					if (text != null) {
						return text;
					}
				}
			}
		}
		return null;
	}

	/**
	 * A copy of the given translations, with the overrides applying to the given tenant, domain,
	 * theme and locale on top (see {@link #find(String, String, String, String, Locale)}).
	 */
	public JsonObject applyTo(JsonObject translations, String tenantId, String domain, String theme, Locale locale) {
		final JsonObject result = translations == null ? new JsonObject() : translations.copy();
		if (isEmpty()) {
			return result;
		}
		final List<String> variants = variants(theme, locale);
		final List<Map<String, Map<String, JsonObject>>> scopes = scopes(tenantId, domain);
		// From the least to the most specific, the most specific winning
		for (int s = scopes.size() - 1; s >= 0; s--) {
			for (int i = variants.size() - 2; i >= 0; i -= 2) {
				final JsonObject overrides = get(scopes.get(s), variants.get(i), variants.get(i + 1));
				if (overrides != null) {
					result.mergeIn(overrides);
				}
			}
		}
		return result;
	}

	/** Scopes from the most to the least specific. */
	private List<Map<String, Map<String, JsonObject>>> scopes(String tenantId, String domain) {
		final List<Map<String, Map<String, JsonObject>>> scopes = new ArrayList<>(3);
		if (tenantId != null && byTenant.containsKey(tenantId)) {
			scopes.add(byTenant.get(tenantId));
		}
		final String normalizedDomain = normalizeDomain(domain);
		if (normalizedDomain != null && !I18n.DEFAULT_DOMAIN.equals(normalizedDomain)
				&& byDomain.containsKey(normalizedDomain)) {
			scopes.add(byDomain.get(normalizedDomain));
		}
		if (byDomain.containsKey(I18n.DEFAULT_DOMAIN)) {
			scopes.add(byDomain.get(I18n.DEFAULT_DOMAIN));
		}
		return scopes;
	}

	/** (theme, language) pairs from the most to the least specific, flattened. */
	private static List<String> variants(String theme, Locale locale) {
		final String language = locale == null ? ANY : normalizeLanguage(locale.getLanguage());
		final List<String> variants = new ArrayList<>(8);
		final boolean hasTheme = theme != null && !theme.trim().isEmpty();
		if (!language.isEmpty()) {
			if (hasTheme) {
				variants.add(theme.trim());
				variants.add(language);
			}
			variants.add(ANY);
			variants.add(language);
		}
		if (hasTheme) {
			variants.add(theme.trim());
			variants.add(ANY);
		}
		variants.add(ANY);
		variants.add(ANY);
		return variants;
	}

	private static JsonObject get(Map<String, Map<String, JsonObject>> scope, String theme, String language) {
		final Map<String, JsonObject> ofTheme = scope.get(theme);
		return ofTheme == null ? null : ofTheme.get(language);
	}

	/** Lower-cased, without trailing dot nor port: the form domains are stored in by the tenants. */
	static String normalizeDomain(String domain) {
		if (domain == null || domain.trim().isEmpty()) {
			return null;
		}
		String normalized = domain.trim().toLowerCase(Locale.ROOT);
		final int port = normalized.lastIndexOf(':');
		if (port > 0 && normalized.indexOf(':') == port) {
			normalized = normalized.substring(0, port);
		}
		return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
	}

	private static String normalizeLanguage(String language) {
		return language == null ? ANY : language.trim().toLowerCase(Locale.ROOT);
	}

	public static final class Builder {

		private final Map<String, Map<String, Map<String, JsonObject>>> byTenant = new HashMap<>();
		private final Map<String, Map<String, Map<String, JsonObject>>> byDomain = new HashMap<>();

		private Builder() {
		}

		/**
		 * Adds overrides applying to the users of a tenant; later ones win over earlier ones.
		 *
		 * @param language null for all languages
		 * @param theme    null for all themes
		 */
		public Builder addTenantOverrides(String tenantId, String language, String theme, JsonObject translations) {
			add(byTenant, tenantId, language, theme, translations);
			return this;
		}

		/**
		 * Adds overrides applying to the requests made at a domain ({@value I18n#DEFAULT_DOMAIN}
		 * applying to all domains); later ones win over earlier ones.
		 *
		 * @param language null for all languages
		 * @param theme    null for all themes
		 */
		public Builder addDomainOverrides(String domain, String language, String theme, JsonObject translations) {
			add(byDomain, normalizeDomain(domain), language, theme, translations);
			return this;
		}

		private static void add(Map<String, Map<String, Map<String, JsonObject>>> scopes, String scope,
								String language, String theme, JsonObject translations) {
			if (scope == null || translations == null || translations.isEmpty()) {
				return;
			}
			final String themeKey = theme == null ? ANY : theme.trim();
			Map<String, Map<String, JsonObject>> ofScope = scopes.get(scope);
			if (ofScope == null) {
				ofScope = new HashMap<>();
				scopes.put(scope, ofScope);
			}
			Map<String, JsonObject> ofTheme = ofScope.get(themeKey);
			if (ofTheme == null) {
				ofTheme = new HashMap<>();
				ofScope.put(themeKey, ofTheme);
			}
			final String languageKey = normalizeLanguage(language);
			final JsonObject existing = ofTheme.get(languageKey);
			if (existing == null) {
				ofTheme.put(languageKey, translations.copy());
			} else {
				existing.mergeIn(translations);
			}
		}

		public I18nOverrides build() {
			if (byTenant.isEmpty() && byDomain.isEmpty()) {
				return EMPTY;
			}
			return new I18nOverrides(freeze(byTenant), freeze(byDomain));
		}

		private static Map<String, Map<String, Map<String, JsonObject>>> freeze(
				Map<String, Map<String, Map<String, JsonObject>>> scopes) {
			final Map<String, Map<String, Map<String, JsonObject>>> frozen = new HashMap<>();
			for (Map.Entry<String, Map<String, Map<String, JsonObject>>> scope : scopes.entrySet()) {
				final Map<String, Map<String, JsonObject>> themes = new HashMap<>();
				for (Map.Entry<String, Map<String, JsonObject>> theme : scope.getValue().entrySet()) {
					final Map<String, JsonObject> languages = new HashMap<>();
					for (Map.Entry<String, JsonObject> language : theme.getValue().entrySet()) {
						languages.put(language.getKey(), language.getValue().copy());
					}
					themes.put(theme.getKey(), Collections.unmodifiableMap(languages));
				}
				frozen.put(scope.getKey(), Collections.unmodifiableMap(themes));
			}
			return Collections.unmodifiableMap(frozen);
		}
	}
}
