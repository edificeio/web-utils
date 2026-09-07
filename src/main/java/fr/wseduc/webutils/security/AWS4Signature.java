/*
 * Copyright © Open Digital Education, 2022
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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.wseduc.webutils.Utils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientRequest;

public class AWS4Signature {

    public static final String EMPTY_PAYLOAD_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    /** Prefix of the headers SigV4 makes mandatory to sign. */
    private static final String AMZ_HEADER_PREFIX = "x-amz-";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Z"));
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("Z"));

    public static String sign(String httpMethod, String canonicalUri, String canonicalQueryString, MultiMap canonicalHeaders,
            String region, String accessKey, String secretKey, String payloadSha256, Instant now)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

        final StringBuilder canonicalRequest = new StringBuilder()
                .append(httpMethod).append("\n")
                .append(canonicalUri).append("\n")
                .append(canonicalQueryString(canonicalQueryString)).append("\n");

        final String hashPayload = (payloadSha256 != null ? payloadSha256: EMPTY_PAYLOAD_SHA256);

        // Canonical headers: lowercase names, sorted, values trimmed — never URL-encoded, which would
        // not match what the server recomputes for any value holding a special character.
        for (Map.Entry<String, String> h : canonicalHeaders(canonicalHeaders).entrySet()) {
            canonicalRequest.append(h.getKey()).append(":").append(h.getValue()).append("\n");
        }
        canonicalRequest.append("\n");
        canonicalRequest.append(signedHeaders(canonicalHeaders)).append("\n");
        canonicalRequest.append(hashPayload);

        final String day = DATE_FORMAT.format(now);

        final StringBuilder stringToSign = new StringBuilder()
            .append("AWS4-HMAC-SHA256").append("\n")
            .append(DATETIME_FORMAT.format(now)).append("\n")
            .append(day).append("/").append(region).append("/s3/aws4_request\n")
            .append(Sha256.hash(canonicalRequest.toString()));

        final byte[] dateKey = HmacSha256.sign(day, ("AWS4" + secretKey).getBytes("UTF-8"));
        final byte[] dateRegionKey = HmacSha256.sign(region, dateKey);
        final byte[] dateRegionServiceKey = HmacSha256.sign("s3", dateRegionKey);
        final byte[] signingKey = HmacSha256.sign("aws4_request", dateRegionServiceKey);
        // final String signature = new BigInteger(1, HmacSha256.sign(stringToSign.toString(), signingKey)).toString(16);
        final String signature = byteArrayToHex(HmacSha256.sign(stringToSign.toString(), signingKey));
        return signature;
    }

    /**
     * The {@code SignedHeaders} part of the Authorization header: lowercase header names, sorted, joined
     * by a semicolon. Shared with the canonical request so the two can never disagree.
     */
    public static String signedHeaders(MultiMap headers) {
        return String.join(";", canonicalHeaders(headers).keySet());
    }

    /**
     * Headers in canonical form, as SigV4 requires them: keyed by lowercase name, sorted by that name,
     * values trimmed with sequential spaces collapsed, and repeated headers joined by a comma.
     */
    private static SortedMap<String, String> canonicalHeaders(MultiMap headers) {
        final SortedMap<String, String> canonical = new TreeMap<>();
        for (String name : headers.names()) {
            final StringBuilder value = new StringBuilder();
            for (String v : headers.getAll(name)) {
                if (value.length() > 0) {
                    value.append(",");
                }
                value.append(v == null ? "" : v.trim().replaceAll("\\s+", " "));
            }
            canonical.put(name.toLowerCase(Locale.ROOT), value.toString());
        }
        return canonical;
    }

    /**
     * The authority Vert.x will actually write in the {@code Host} header, which is the value that has to be
     * signed. {@link HttpClientRequest#getHost()} alone drops the port, while Vert.x appends it as soon as it
     * is not the default one for the scheme — signing the bare host against an endpoint on a non standard port
     * (MinIO, an internal gateway) yields SignatureDoesNotMatch.
     */
    public static String authority(HttpClientRequest request) {
        return authority(request.getHost(), request.getPort(), request.connection().isSsl());
    }

    /**
     * Same rule as {@code HttpClientRequestBase.authority()}, kept here so that the signed value and the one
     * put on the wire cannot drift apart.
     */
    public static String authority(String host, int port, boolean ssl) {
        if (port < 0 || (port == 80 && !ssl) || (port == 443 && ssl)) {
            return host;
        }
        return host + ":" + port;
    }

    /**
     * The {@code CanonicalQueryString} of SigV4: parameters split on {@code &}, each given an explicit
     * {@code =}, and the whole sorted by parameter name — then by value for a repeated name.
     * <p>
     * Values are passed through exactly as they already sit in the URI: they are what goes on the wire, and
     * re-encoding them here would turn every {@code %} into {@code %25}. Encoding stays the caller's job,
     * ordering is ours — a query built in any other order used to sign against a canonical request the
     * server never rebuilds the same way, and answer SignatureDoesNotMatch.
     */
    public static String canonicalQueryString(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        final List<String[]> params = new ArrayList<>();
        for (String param : query.split("&")) {
            if (param.isEmpty()) {
                continue;
            }
            final int separator = param.indexOf('=');
            params.add(separator < 0
                    ? new String[] { param, "" }
                    : new String[] { param.substring(0, separator), param.substring(separator + 1) });
        }
        params.sort((a, b) -> {
            final int byName = a[0].compareTo(b[0]);
            return byName != 0 ? byName : a[1].compareTo(b[1]);
        });
        final StringBuilder canonical = new StringBuilder();
        for (String[] param : params) {
            if (canonical.length() > 0) {
                canonical.append("&");
            }
            canonical.append(param[0]).append("=").append(param[1]);
        }
        return canonical.toString();
    }

    public static String byteArrayToHex(byte[] a) {
        final StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static void sign(HttpClientRequest request, String region, String accessKey, String secretKey, String payloadSha256)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

        final String hashPayload = (payloadSha256 != null ? payloadSha256: EMPTY_PAYLOAD_SHA256);
        final Instant instant = Instant.now();
        final String now = DATETIME_FORMAT.format(instant);
        final MultiMap canonicalHeaders = MultiMap.caseInsensitiveMultiMap();
        canonicalHeaders.add("host", authority(request));
        // Every x-amz-* header carried by the request MUST be signed: S3 implementations reject the
        // request otherwise, with AccessDenied / HeadersNotSigned naming the offending header. This
        // covers object metadata (x-amz-meta-*), SSE-C keys and copy-source headers alike.
        for (String name : request.headers().names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(AMZ_HEADER_PREFIX)) {
                for (String value : request.headers().getAll(name)) {
                    canonicalHeaders.add(name, value);
                }
            }
        }
        // Added last, and after removal, so that re-signing an already signed request (a retry) keeps a
        // single, up to date value rather than two conflicting ones.
        canonicalHeaders.remove("x-amz-content-sha256");
        canonicalHeaders.remove("x-amz-date");
        canonicalHeaders.add("x-amz-content-sha256", hashPayload);
        canonicalHeaders.add("x-amz-date", now);

        final String signature = sign(request.getMethod().name(), request.path(), Utils.getOrElse(request.query(), ""),
                canonicalHeaders, region, accessKey, secretKey, payloadSha256, instant);
        request.putHeader("Authorization",
                "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + DATE_FORMAT.format(instant) + "/" + region + "/s3/aws4_request, " +
                "SignedHeaders=" + signedHeaders(canonicalHeaders) + ", " +
                "Signature=" + signature
        );
        request.putHeader("x-amz-content-sha256", hashPayload);
        request.putHeader("x-amz-date", now);
    }

}
