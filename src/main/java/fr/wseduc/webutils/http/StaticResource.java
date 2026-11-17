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

package fr.wseduc.webutils.http;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class StaticResource {

	private static final SimpleDateFormat format =
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.UK);
	static {
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/*
	 * Browser caching policy for static files (outside dev mode).
	 * - Content-hashed build outputs never change under the same name: cached for a year without revalidation.
	 * - Any other file keeps its name across releases: the browser must revalidate (Last-Modified /
	 *   If-Modified-Since => 304) so that a rollout is visible immediately. Shared caches still keep
	 *   it, their TTL is set on their side so they have to be purged after each deploy.
	 * - Files whose content depends on the user (theme, session, host...) must not be stored by shared caches.
	 */
	public static final String CACHE_CONTROL_REVALIDATE = "public, no-cache";
	public static final String CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable";
	public static final String CACHE_CONTROL_PRIVATE = "private, no-cache";
	public static final String CACHE_CONTROL_DEV = "max-age=0, no-cache, must-revalidate";
	private static final Pattern CONTENT_HASHED_FILE =
			Pattern.compile(".*[-.](?=[A-Za-z0-9_-]*[0-9A-Z])[A-Za-z0-9_-]{8}\\.(?:js|mjs|css)$");

	public static boolean isContentHashed(String path) {
		return path != null && CONTENT_HASHED_FILE.matcher(path).matches();
	}

	public static String cacheControl(String path, boolean dev, boolean shared) {
		if (dev) {
			return CACHE_CONTROL_DEV;
		}
		if (!shared) {
			return CACHE_CONTROL_PRIVATE;
		}
		return isContentHashed(path) ? CACHE_CONTROL_IMMUTABLE : CACHE_CONTROL_REVALIDATE;
	}

	public static void addLastModifiedHeader(HttpServerResponse response, Date resourceLastModified) {
		response.headers().add("Last-Modified", format.format(resourceLastModified));
	}

	public static void addLastModifiedHeader(HttpServerResponse response, String resourceLastModified) {
		response.headers().add("Last-Modified", resourceLastModified);
	}

	public static boolean checkLastModified(HttpServerRequest request, String resourceLastModified) {
		String ims = request.headers().get("If-Modified-Since");
		if (ims != null && resourceLastModified != null) {
			try {
				Date imsDate = format.parse(ims);
				Date rlm = format.parse(resourceLastModified);
				return imsDate != null && rlm != null &&
						imsDate.getTime() >= rlm.getTime();
			} catch (ParseException | NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	public static void serveRessource(HttpServerRequest request, String ressourcePath,
			String resourceLastModified) {
		serveRessource(request, ressourcePath, resourceLastModified, false);
	}

	public static void serveRessource(HttpServerRequest request, String ressourcePath,
			String resourceLastModified, boolean dev) {
		serveRessource(request, ressourcePath, resourceLastModified, dev, true);
	}

	/**
	 * @param shared false when the file served for this URL depends on the user (theme cookie, session
	 *               preferences, host...): it is then marked private so that shared caches never store it.
	 */
	public static void serveRessource(HttpServerRequest request, String ressourcePath,
			String resourceLastModified, boolean dev, boolean shared) {
		request.response().headers().set("Cache-Control", cacheControl(ressourcePath, dev, shared));
		addLastModifiedHeader(request.response(), resourceLastModified);
		if (checkLastModified(request, resourceLastModified)) {
			request.response().setStatusCode(304).end();
		} else {
			request.response().sendFile(ressourcePath, ar -> {
				if (ar.failed() && !request.response().ended()) {
					Renders.notFound(request);
				}
			});
		}
	}

	public static String formatDate(long lastModified) {
		return formatDate(new Date(lastModified));
	}

	public static String formatDate(Date lastModified) {
		return format.format(lastModified);
	}

}
