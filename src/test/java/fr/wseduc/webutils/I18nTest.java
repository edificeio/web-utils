package fr.wseduc.webutils;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18nTest {

	private static I18n i18nWithFiles() {
		final Map<Locale, JsonObject> messages = new HashMap<>();
		messages.put(Locale.FRENCH, new JsonObject().put("hello", "Bonjour {0}").put("bye", "Au revoir"));
		messages.put(Locale.ENGLISH, new JsonObject().put("hello", "Hello {0}"));
		return new I18n().initializeMessages(messages);
	}

	@Test
	public void withoutOverridesTranslationsComeFromTheFiles() {
		final I18n i18n = i18nWithFiles();
		eq("Bonjour Ada", i18n.translate("hello", "ent.example.org", "fr", "Ada"));
		eq("Hello Ada", i18n.translate("hello", I18n.DEFAULT_DOMAIN, (String) null, Locale.GERMAN, "Ada"));
		eq("missing", i18n.translate("missing", "ent.example.org", null, Locale.FRENCH));
	}

	@Test
	public void overridesWinOverTheFilesAndAreReplacedAsAWhole() {
		final I18n i18n = i18nWithFiles();
		i18n.setOverrides(I18nOverrides.builder()
				.addDomainOverrides("ent.example.org", "fr", null, new JsonObject().put("hello", "Salut {0}"))
				.addTenantOverrides("t1", "fr", null, new JsonObject().put("hello", "Coucou {0}"))
				.build());

		eq("Salut Ada", i18n.translate("hello", "ent.example.org", "fr", "Ada"));
		eq("Salut Ada", i18n.translate("hello", "ent.example.org", null, Locale.FRENCH, "Ada"),
				"translations without tenant get the overrides of the domain");
		eq("Coucou Ada", i18n.translate("hello", "ent.example.org", "t1", null, Locale.FRENCH, "Ada"));
		eq("Bonjour Ada", i18n.translate("hello", "other.example.org", "fr", "Ada"));
		eq("Au revoir", i18n.translate("bye", "ent.example.org", "t1", null, Locale.FRENCH));

		i18n.setOverrides(null);
		eq("Bonjour Ada", i18n.translate("hello", "ent.example.org", "t1", null, Locale.FRENCH, "Ada"));
	}

	@Test
	public void loadedTranslationsGetTheOverridesOfTheDomainWithoutBeingModified() {
		final I18n i18n = i18nWithFiles();
		i18n.setOverrides(I18nOverrides.builder()
				.addDomainOverrides("ent.example.org", "fr", null, new JsonObject().put("bye", "Ciao"))
				.build());

		@SuppressWarnings("deprecation") final JsonObject loaded = i18n.load("fr", "ent.example.org");
		eq("Ciao", loaded.getString("bye"));
		eq("Bonjour {0}", loaded.getString("hello"));
		@SuppressWarnings("deprecation") final JsonObject fromFiles = i18n.load("fr", "other.example.org");
		eq("Au revoir", fromFiles.getString("bye"));
	}

	private static void eq(Object expected, Object actual) {
		org.junit.Assert.assertEquals(expected, actual);
	}

	private static void eq(Object expected, Object actual, String message) {
		org.junit.Assert.assertEquals(message, expected, actual);
	}
}
