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

package fr.wseduc.webutils.collections;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.*;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncResult;

public class AsyncLocalMap<K, V> implements AsyncMap<K, V> {

	private Vertx vertx;
	private LocalMap<K, V> localMap;

	public AsyncLocalMap(LocalMap<K, V> localMap) {
		this(localMap, null);
	}

	public AsyncLocalMap(LocalMap<K, V> localMap, Vertx vertx) {
		this.localMap = localMap;
		this.vertx = vertx;
	}

	@Override
	public void get(K k, Handler<AsyncResult<V>> resultHandler) {
		handleAsyncResult(localMap.get(k), resultHandler);
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> get(K k) {
		return Future.succeededFuture(localMap.get(k));
	}

	@Override
	public void put(K k, V v, Handler<AsyncResult<Void>> completionHandler) {
		localMap.put(k, v);
		handleAsyncResult(null, completionHandler);
	}

	@Override
	public Future<Void> put(K k, V v) {
		localMap.put(k, v);
		return Future.succeededFuture();
	}

	@Override
	public void put(K k, V v, long ttl, Handler<AsyncResult<Void>> completionHandler) {
		localMap.put(k, v);
		handleAsyncResult(null, completionHandler);
		if (vertx != null) {
			vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, v));
		}
	}

	@Override
	public Future<Void> put(K k, V v, long ttl) {
		localMap.put(k, v);
		if (vertx != null) {
			vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, v));
		}
		return Future.succeededFuture();
	}

	@Override
	public void putIfAbsent(K k, V v, Handler<AsyncResult<V>> completionHandler) {
		handleAsyncResult(localMap.putIfAbsent(k, v), completionHandler);
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> putIfAbsent(K k, V v) {
		final Promise<V> promise = Promise.promise();
		putIfAbsent(k, v, e -> {
			if(e.succeeded()) {
				promise.complete(e.result());
			} else {
				promise.fail(e.cause());
			}
		});
		return promise.future();
	}

	@Override
	public void putIfAbsent(K k, V v, long ttl, Handler<AsyncResult<V>> completionHandler) {
		handleAsyncResult(localMap.putIfAbsent(k, v), completionHandler);
		if (vertx != null) {
			vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, v));
		}
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> putIfAbsent(K k, V v, long ttl) {
		final Promise<V> promise = Promise.promise();
		putIfAbsent(k, v, ttl, e -> {
			if(e.succeeded()) {
				promise.complete(e.result());
			} else {
				promise.fail(e.cause());
			}
		});
		return promise.future();
	}

	@Override
	public void remove(K k, Handler<AsyncResult<V>> resultHandler) {
		handleAsyncResult(localMap.remove(k), resultHandler);
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> remove(K k) {
		final Promise<V> promise = Promise.promise();
		remove(k, e -> {
			if(e.succeeded()) {
				promise.complete(e.result());
			} else {
				promise.fail(e.cause());
			}
		});
		return promise.future();
	}

	@Override
	public void removeIfPresent(K k, V v, Handler<AsyncResult<Boolean>> resultHandler) {
		handleAsyncResult(localMap.removeIfPresent(k, v), resultHandler);
	}

	@Override
	public Future<Boolean> removeIfPresent(K k, V v) {
		final Promise<Boolean> promise = Promise.promise();
		removeIfPresent(k, v, e -> {
			if(e.succeeded()) {
				promise.complete(e.result());
			} else {
				promise.fail(e.cause());
			}
		});
		return promise.future();
	}

	@Override
	public void replace(K k, V v, Handler<AsyncResult<V>> resultHandler) {
		handleAsyncResult(localMap.replace(k, v), resultHandler);
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> replace(K k, V v) {
		return Future.succeededFuture(localMap.replace(k, v));
	}

	@Override
	public void replace(K k, V v, long ttl, Handler<AsyncResult<@io.vertx.codegen.annotations.Nullable V>> asyncResultHandler) {
		localMap.replace(k, v);
		if (vertx != null) {
			vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, v));
		}
	}

	@Override
	public Future<@io.vertx.codegen.annotations.Nullable V> replace(K k, V v, long ttl) {
		final V replaced = localMap.replace(k, v);
		if (vertx != null) {
			vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, v));
		}
		return Future.succeededFuture(replaced);
	}

	@Override
	public void replaceIfPresent(K k, V oldValue, V newValue, Handler<AsyncResult<Boolean>> resultHandler) {
		handleAsyncResult(localMap.replaceIfPresent(k, oldValue, newValue), resultHandler);
	}

	@Override
	public Future<Boolean> replaceIfPresent(K k, V oldValue, V newValue) {
		return Future.succeededFuture(localMap.replaceIfPresent(k, oldValue, newValue));
	}

	@Override
	public void replaceIfPresent(K k, V oldValue, V newValue, long ttl, Handler<AsyncResult<Boolean>> resultHandler) {
		replaceIfPresent(k, oldValue, newValue, ttl)
		.onComplete(e -> resultHandler.handle(new DefaultAsyncResult<>(e.succeeded())));
	}

	@Override
	public Future<Boolean> replaceIfPresent(K k, V oldValue, V newValue, long ttl) {
		return replaceIfPresent(k, oldValue, newValue).onComplete(e -> {
			if(e.succeeded()) {
				vertx.setTimer(ttl, event -> localMap.removeIfPresent(k, newValue));
			}
		});
	}

	@Override
	public void clear(Handler<AsyncResult<Void>> resultHandler) {
		localMap.clear();
		handleAsyncResult(null, resultHandler);
	}

	@Override
	public Future<Void> clear() {
		localMap.clear();
		return Future.succeededFuture();
	}

	@Override
	public void size(Handler<AsyncResult<Integer>> resultHandler) {
		handleAsyncResult(localMap.size(), resultHandler);
	}

	@Override
	public Future<Integer> size() {
		return Future.succeededFuture(localMap.size());
	}

	@Override
	public void keys(Handler<AsyncResult<Set<K>>> resultHandler) {
		handleAsyncResult(localMap.keySet(), resultHandler);
	}

	@Override
	public Future<Set<K>> keys() {
		return Future.succeededFuture(localMap.keySet());
	}

	@Override
	public void values(Handler<AsyncResult<List<V>>> resultHandler) {
		handleAsyncResult((List<V>) localMap.values(), resultHandler);
	}

	@Override
	public Future<List<V>> values() {
		return Future.succeededFuture((List<V>)localMap.values());
	}

	@Override
	public void entries(Handler<AsyncResult<Map<K, V>>> resultHandler) {
		final HashMap<K,V> m = new HashMap<>();
		for (Map.Entry<K, V> e: localMap.entrySet()) {
			m.put(e.getKey(), e.getValue());
		}
		handleAsyncResult(m, resultHandler);
	}

	@Override
	public Future<Map<K, V>> entries() {
		final Promise<Map<K,V>> promise = Promise.promise();
		entries(e -> {
			if(e.succeeded()) {
				promise.complete(e.result());
			} else {
				promise.fail(e.cause());
			}
		});
		return promise.future();
	}



}
