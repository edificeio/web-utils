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

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.http.RouteMatcher;

import java.util.*;

import static fr.wseduc.webutils.Server.createHttpServerOptions;

public abstract class BusModBaseWithHttpProbes extends BusModBase {

	protected static final Logger log = LoggerFactory.getLogger(BusModBaseWithHttpProbes.class);

	private HttpServer server;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		final Promise<Void> vertxPromise = Promise.promise();
		super.start(vertxPromise);
		vertxPromise.future().compose(x -> {
			final String id = System.getenv("HOSTNAME") == null ?
					this.getClass().getSimpleName() + "-" + UUID.randomUUID() :
					System.getenv("HOSTNAME");
			return initializeProbes(id)
		    .compose(e -> initBusModBaseServer());
		}).onFailure(ex -> log.error("Error on vertx init promise", ex))
		.onComplete(startPromise);
	}

	private Future<Void> initBusModBaseServer() {
		if(config.containsKey("port")) {
			log.info("BusVerticle: " + this.getClass().getSimpleName() + " starts on port: " + config.getInteger("port"));
			final RouteMatcher rm = new RouteMatcher();
			addLivenessAndReadinessProbes(rm);
			final HttpServerOptions httpOptions = createHttpServerOptions(null, config);
			return vertx.createHttpServer(httpOptions)
					.requestHandler(rm)
					.listen(config.getInteger("port"))
					.onSuccess(e -> {
						this.server = e;
					})
					.mapEmpty();
		} else {
			log.debug("BusVerticle: " + this.getClass().getSimpleName() + " does not start an http server because no port is configured");
			return Future.succeededFuture();
		}
	}

	@Override
	public void stop(Promise<Void> stopFuture) throws Exception {
		log.info("Closing http server with port : "+config.getInteger("port"));
		final List<Future<Void>> futures = new ArrayList<>();
		if(server!=null){
			final Promise<Void> f = Promise.promise();
			futures.add(f.future());
			server.close(f);
		}
		final Promise<Void> f = Promise.promise();
		super.stop(f);
		futures.add(f.future());
		Future.all(futures).map(e->(Void) null).onComplete(stopFuture);
	}

}
