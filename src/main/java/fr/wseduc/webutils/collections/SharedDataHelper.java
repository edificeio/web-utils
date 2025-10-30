package fr.wseduc.webutils.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Lock;

public class SharedDataHelper {

    private static final Logger log = LoggerFactory.getLogger(SharedDataHelper.class);
    private Vertx vertx;

    private SharedDataHelper(){}

	private static class SharedDataHolder {
		private static final SharedDataHelper instance = new SharedDataHelper();
	}

	public static SharedDataHelper getInstance() {
		return SharedDataHolder.instance;
	}

	public void init(Vertx vertx) {
		this.vertx = vertx;
	}

    public Future<Lock> getLock(final String lockName, final long timeout) {
        return this.vertx.sharedData().getLockWithTimeout(lockName, timeout);
    }

    public Future<Void> releaseLockAfterDelay(final Lock lock, final long delay) {
        return Future.future(p -> {
            vertx.setTimer(delay, e -> {
                lock.release();
                p.complete();
            });
        });
    }

    public <K, V> Future<AsyncMap<K, V>> getLocalAsyncMap(String mapName) {
        final Promise<AsyncMap<K, V>> promise = Promise.promise();
        vertx.sharedData().<K, V>getLocalAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                promise.complete(ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    public <K, V> Future<AsyncMap<K, V>> getAsyncMap(String mapName) {
        final Promise<AsyncMap<K, V>> promise = Promise.promise();
        vertx.sharedData().<K, V>getAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                promise.complete(ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    public <K, V> Future<V> get(String mapName, K key) {
        final Promise<V> promise = Promise.promise();
        vertx.sharedData().<K, V>getAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                getValue(key, promise, ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    public <K, V> Future<V> getLocal(String mapName, K key) {
        final Promise<V> promise = Promise.promise();
        vertx.sharedData().<K, V>getLocalAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                getValue(key, promise, ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    private <K, V> Future<V> getValue(K key, final Promise<V> promise, AsyncMap<K, V> asyncMap) {
        asyncMap.get(key, ar2 -> {
            if (ar2.succeeded()) {
                try {
                    promise.complete(ar2.result());
                } catch (ClassCastException e) {
                    log.error("Cast error on key " + key + " : " + ar2.result(), e);
                }
            } else {
                promise.fail(ar2.cause());
            }
        });
        return promise.future();
    }

    public <K, V> Future<V> getValue(K key, AsyncMap<K, V> asyncMap) {
        return getValue(key, Promise.promise(), asyncMap);
    }

    public <K, V> Future<Map<K, V>> getMulti(String mapName, K... keys) {
        final Promise<Map<K, V>> promise = Promise.promise();
        vertx.sharedData().<K, V>getAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                final List<Future> futures = new ArrayList<>();
                for (K key: keys) {
                    futures.add(getValue(key, ar.result()));
                }
                CompositeFuture.all(futures).onSuccess(cf -> {
                    final Map<K, V> res = new HashMap<>();
                    int i = 0;
                    for (K key: keys) {
                        res.put(key, (V) futures.get(i++).result());
                    }
                    promise.complete(res);
                }).onFailure(ex -> promise.fail(ex));
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    public <K, V> Future<Map<K, V>> getLocalMulti(String mapName, K... keys) {
        final Promise<Map<K, V>> promise = Promise.promise();
        vertx.sharedData().<K, V>getLocalAsyncMap(mapName, ar -> {
            if (ar.succeeded()) {
                final List<Future> futures = new ArrayList<>();
                for (K key: keys) {
                    futures.add(getValue(key, ar.result()));
                }
                CompositeFuture.all(futures).onSuccess(cf -> {
                    final Map<K, V> res = new HashMap<>();
                    int i = 0;
                    for (K key: keys) {
                        res.put(key, (V) futures.get(i++).result());
                    }
                    promise.complete(res);
                }).onFailure(ex -> promise.fail(ex));
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

}
