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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import fr.wseduc.webutils.Utils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientRequest;

public class AWS4Signature {

    public static final String EMPTY_PAYLOAD_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Z"));
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("Z"));

    public static String sign(String httpMethod, String canonicalUri, String canonicalQueryString, MultiMap canonicalHeaders,
            String region, String accessKey, String secretKey, String payloadSha256)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

        final StringBuilder canonicalRequest = new StringBuilder()
                .append(httpMethod).append("\n")
                .append(canonicalUri).append("\n")
                .append(canonicalQueryString).append("\n");

        final String hashPayload = (payloadSha256 != null ? payloadSha256: EMPTY_PAYLOAD_SHA256);
        final Instant now = Instant.now();

        for (Map.Entry<String, String> h : canonicalHeaders.entries()) { // TODO add uri encode
            canonicalRequest.append(h.getKey().toLowerCase()).append(":").append(h.getValue()).append("\n");
        }
        canonicalRequest.append("\n");
        canonicalRequest.append(canonicalHeaders.names().stream().collect(Collectors.joining(";"))).append("\n");
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
        final String signature = new BigInteger(1, HmacSha256.sign(stringToSign.toString(), signingKey)).toString(16);

        return signature;
    }

    public static void sign(HttpClientRequest request, String region, String accessKey, String secretKey, String payloadSha256)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

        final String hashPayload = (payloadSha256 != null ? payloadSha256: EMPTY_PAYLOAD_SHA256);
        final String now = DATETIME_FORMAT.format(Instant.now());
        MultiMap canonicalHeaders = MultiMap.caseInsensitiveMultiMap();
		canonicalHeaders.add("host", request.getHost());
		canonicalHeaders.add("x-amz-content-sha256", hashPayload);
		canonicalHeaders.add("x-amz-date", now);

        final String signature = sign(request.method().name(), request.path(), Utils.getOrElse(request.query(), ""),
                canonicalHeaders, region, accessKey, secretKey, payloadSha256);
        request.putHeader("Authorization",
                "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + DATE_FORMAT.format(Instant.now()) + "/" + region + "/s3/aws4_request, " +
                "SignedHeaders=" + canonicalHeaders.names().stream().collect(Collectors.joining(";")) + ", " +
                "Signature=" + signature
        );
        request.putHeader("x-amz-content-sha256", hashPayload);
        request.putHeader("x-amz-date", now);
    }

}
