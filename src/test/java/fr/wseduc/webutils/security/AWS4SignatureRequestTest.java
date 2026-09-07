/*
 * Copyright © Open Digital Education, 2026
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

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Signs a real request against a real server, and recomputes the signature from what the server actually
 * received. Where {@code AlgorithmTest} pins the algorithm on fixed inputs, this pins the wiring: which
 * {@code host} value and which query string {@link AWS4Signature#sign(io.vertx.core.http.HttpClientRequest,
 * String, String, String, String)} feeds the canonical request.
 * <p>
 * The two defects it guards against are silent under AWS on 443 with a single query parameter, which is why
 * they went unnoticed: a bare {@code host} only breaks on an endpoint with a non standard port (MinIO, an
 * internal gateway), and an unsorted query only breaks on the routes carrying several parameters.
 */
public class AWS4SignatureRequestTest {

    private static final String REGION = "us-east-1";
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    /** An encoded key, as {@code S3Client.encodeUrlPath} produces. Vert.x hands it to the signer verbatim. */
    private static final String PATH = "/bucket/dossier%20priv%C3%A9/a%20b.pdf";
    /** Deliberately not in SigV4 order: the signer has to sort it, the wire keeps it as it is. */
    private static final String WIRE_QUERY = "uploadId=a%2Bb%3D%3D&partNumber=1";
    private static final String CANONICAL_QUERY = "partNumber=1&uploadId=a%2Bb%3D%3D";

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private Vertx vertx;
    private HttpServer server;
    private HttpClient client;

    private final CompletableFuture<MultiMap> received = new CompletableFuture<>();
    private final AtomicReference<String> receivedUri = new AtomicReference<>();

    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();
        server = await(vertx.createHttpServer()
                .requestHandler(request -> {
                    receivedUri.set(request.uri());
                    // Copied: the server request, and its header map, are recycled once the handler returns.
                    received.complete(MultiMap.caseInsensitiveMultiMap().addAll(request.headers()));
                    request.response().setStatusCode(200).end();
                })
                .listen(0));
    }

    @After
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (vertx != null) {
            await(vertx.close());
        }
    }

    @Test
    public void signsTheAuthorityAndTheSortedQueryTheServerReceives() throws Exception {
        final int port = server.actualPort();
        // The premise of the whole test: an ephemeral port is never the default one of the scheme.
        assertNotEquals(80, port);
        assertNotEquals(443, port);

        // Same shape as ResilientHttpClient / S3Client: the port lives on the client options, the request
        // itself only carries a host — which is exactly how the missing port went unnoticed.
        client = vertx.createHttpClient(new HttpClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(port));
        await(client.request(new RequestOptions()
                        .setMethod(HttpMethod.PUT)
                        .setHost("localhost")
                        .setURI(PATH + "?" + WIRE_QUERY))
                .compose(request -> {
                    try {
                        AWS4Signature.sign(request, REGION, ACCESS_KEY, SECRET_KEY, null);
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                    return request.send();
                }));

        final MultiMap headers = received.get(10, TimeUnit.SECONDS);
        final String authority = headers.get("Host");
        final String amzDate = headers.get("x-amz-date");
        final String signature = signatureOf(headers.get("Authorization"));

        // What Vert.x put on the wire, and what therefore has to be signed.
        assertEquals("localhost:" + port, authority);
        assertEquals("host;x-amz-content-sha256;x-amz-date", signedHeadersOf(headers.get("Authorization")));
        // The request is signed, not rewritten: the query reaches the server in the order it was built.
        assertEquals(PATH + "?" + WIRE_QUERY, receivedUri.get());

        // The pin: the signature that travelled is the one over the authority the server received, and over
        // the query sorted as SigV4 wants it.
        assertEquals(signatureFor(authority, CANONICAL_QUERY, amzDate), signature);

        // And not the signature the bare host would have produced: without this, going back to
        // request.getHost() would leave the suite green.
        assertNotEquals(signatureFor("localhost", CANONICAL_QUERY, amzDate), signature);
        // The sort is pinned by the assertion above, not here: the query reached the server unsorted, so a
        // signer that stopped sorting would no longer match CANONICAL_QUERY. What is left to check is that
        // the query takes part in the canonical request at all.
        assertNotEquals(signatureFor(authority, "partNumber=2&uploadId=a%2Bb%3D%3D", amzDate), signature);
    }

    /** Recomputes a signature straight from the specification, on the values the server reported. */
    private static String signatureFor(String host, String canonicalQuery, String amzDate) throws Exception {
        final MultiMap canonicalHeaders = MultiMap.caseInsensitiveMultiMap();
        canonicalHeaders.add("host", host);
        canonicalHeaders.add("x-amz-content-sha256", AWS4Signature.EMPTY_PAYLOAD_SHA256);
        canonicalHeaders.add("x-amz-date", amzDate);
        return AWS4Signature.sign("PUT", PATH, canonicalQuery, canonicalHeaders,
                REGION, ACCESS_KEY, SECRET_KEY, null, instantOf(amzDate));
    }

    /**
     * The signed instant, read back from the header the signer posted. {@code x-amz-date} is second
     * precision, and the signature derives from that same truncated value, so this is exact.
     */
    private static Instant instantOf(String amzDate) {
        return LocalDateTime.parse(amzDate, AMZ_DATE).toInstant(ZoneOffset.UTC);
    }

    private static String signatureOf(String authorization) {
        return valueOf(authorization, "Signature=");
    }

    private static String signedHeadersOf(String authorization) {
        return valueOf(authorization, "SignedHeaders=");
    }

    private static String valueOf(String authorization, String field) {
        final int from = authorization.indexOf(field) + field.length();
        final int to = authorization.indexOf(',', from);
        return to < 0 ? authorization.substring(from) : authorization.substring(from, to);
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

}
