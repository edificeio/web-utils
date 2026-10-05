package fr.wseduc.webutils;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class I18nOverridesTest {

	private static final Locale FR = Locale.FRENCH;

	private static JsonObject json(String... keyValues) {
		final JsonObject json = new JsonObject();
		for (int i = 0; i < keyValues.length; i += 2) {
			json.put(keyValues[i], keyValues[i + 1]);
		}
		return json;
	}

	@Test
	public void lookupGoesFromTheTenantToTheDomainToTheDefaultDomainThenFromTheLanguageAndThemeToAll() {
		final I18nOverrides overrides = I18nOverrides.builder()
				.addDomainOverrides(I18n.DEFAULT_DOMAIN, null, null, json("k", "default any", "default", "default any"))
				.addDomainOverrides(I18n.DEFAULT_DOMAIN, "fr", null, json("k", "default fr"))
				.addDomainOverrides("ENT.example.org.", "fr", null, json("k", "domain fr", "domain", "domain fr"))
				.addDomainOverrides("ent.example.org", null, "panda", json("k", "domain panda", "themed", "domain panda"))
				.addDomainOverrides("ent.example.org", "fr", "panda", json("themed", "domain fr panda"))
				.addTenantOverrides("t1", "fr", null, json("k", "tenant fr"))
				.addTenantOverrides("t1", "en", null, json("tenant", "tenant en"))
				.build();

		eq("tenant fr", overrides.find("k", "t1", "ent.example.org", "panda", FR));
		eq("domain fr", overrides.find("k", "unknown", "ent.example.org:8443", "panda", FR),
				"a language match wins over a theme match");
		eq("domain panda", overrides.find("k", null, "ent.example.org", "panda", Locale.ENGLISH));
		eq("domain fr panda", overrides.find("themed", null, "ent.example.org", "panda", FR));
		eq("domain panda", overrides.find("themed", null, "ent.example.org", "panda", Locale.ENGLISH));
		isNull(overrides.find("themed", null, "ent.example.org", null, FR), "themed overrides need the theme");
		eq("default fr", overrides.find("k", null, "other.example.org", null, FR));
		eq("default any", overrides.find("k", null, null, null, Locale.GERMAN));
		eq("default any", overrides.find("default", "t1", "ent.example.org", "panda", FR),
				"the default domain applies to every domain");
		isNull(overrides.find("tenant", "t1", null, null, FR), "the language of an override must match");
		isNull(overrides.find("missing", "t1", "ent.example.org", "panda", FR));
	}

	@Test
	public void appliedOverridesGoOnTopOfTheTranslationsTheMostSpecificWinning() {
		final I18nOverrides overrides = I18nOverrides.builder()
				.addDomainOverrides(I18n.DEFAULT_DOMAIN, null, null, json("a", "default", "b", "default"))
				.addDomainOverrides("ent.example.org", "fr", null, json("b", "domain", "c", "domain"))
				.addTenantOverrides("t1", null, null, json("c", "tenant any"))
				.addTenantOverrides("t1", "fr", "panda", json("d", "tenant fr panda"))
				.build();
		final JsonObject translations = json("a", "file", "z", "file");

		final JsonObject result = overrides.applyTo(translations, "t1", "ent.example.org", "panda", FR);

		final Map<String, Object> expected = new HashMap<>();
		expected.put("a", "default");
		expected.put("b", "domain");
		expected.put("c", "tenant any");
		expected.put("d", "tenant fr panda");
		expected.put("z", "file");
		eq(expected, result.getMap());
		eq(json("a", "file", "z", "file"), translations, "the translations themselves are left untouched");
	}

	@Test
	public void laterOverridesWinAndEmptyOnesAreIgnored() {
		final I18nOverrides overrides = I18nOverrides.builder()
				.addTenantOverrides("t1", "FR", null, json("k", "first", "other", "first"))
				.addTenantOverrides("t1", "fr", null, json("k", "second"))
				.addTenantOverrides(null, "fr", null, json("k", "ignored"))
				.build();
		eq("second", overrides.find("k", "t1", null, null, FR));
		eq("first", overrides.find("other", "t1", null, null, FR));
		assertSame(I18nOverrides.EMPTY, I18nOverrides.builder().addDomainOverrides("d", null, null, new JsonObject()).build());
		assertTrue(I18nOverrides.EMPTY.applyTo(null, "t1", "d", null, FR).isEmpty());
	}

	private static void eq(Object expected, Object actual) {
		org.junit.Assert.assertEquals(expected, actual);
	}

	private static void eq(Object expected, Object actual, String message) {
		org.junit.Assert.assertEquals(message, expected, actual);
	}

	private static void isNull(Object actual) {
		org.junit.Assert.assertNull(actual);
	}

	private static void isNull(Object actual, String message) {
		org.junit.Assert.assertNull(message, actual);
	}
}
