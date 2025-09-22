package fr.wseduc.webutils.metrics;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static java.lang.System.currentTimeMillis;

/**
 * A probe to be used to detect that an underlying component is alive and reachable or not.
 */
public interface HealthCheckProbe {
  /**
   * Initialize the probe.
   * @param vertx Vertx instance
   * @param config Probe configuration if any (can be {@code null})
   * @return A future that completes when the initialization is done
   */
  Future<Void> init(final Vertx vertx, final JsonObject config);

  /**
   * Perform the actual probe logic (will be cancelled after {@code timeout} milliseconds.
   * @param timeout Time after which the probe is cancelled
   * @return A future that returns the probe evaluation
   */
  default Future<HealthCheckProbeResult> probe(final long timeout) {
    final Promise<HealthCheckProbeResult> promise = Promise.promise();
    final Vertx vertx = getVertx();
    final long start = currentTimeMillis();
    long timer = vertx.setTimer(timeout, e -> promise.tryComplete(new HealthCheckProbeResult(
      this.getName(), false,
      new JsonObject().put("delay", currentTimeMillis() - start).put("aborted", true))));
    probe().onSuccess(e -> {
      vertx.cancelTimer(timer);
      final long delay = currentTimeMillis() - start;
      JsonObject md = e.getMetadata();
      if(md == null) {
        md = new JsonObject();
      }
      promise.tryComplete(new HealthCheckProbeResult(e.getName(), e.isOk(), md.put("delay", delay)));
    }).onFailure(promise::fail);
    return promise.future();
  }

  /** The unique name of this probe. */
  String getName();
  /** The vertx instance attached to this probe.*/
  Vertx getVertx();
  /** The actual probe logic which should be defined by the concrete implementations.*/
  Future<HealthCheckProbeResult> probe();
}
