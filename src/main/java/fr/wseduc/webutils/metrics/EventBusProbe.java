package fr.wseduc.webutils.metrics;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static java.lang.System.currentTimeMillis;

/**
 * Checks the round-time trip of a message on the event bus.
 */
public class EventBusProbe implements HealthCheckProbe {
  private String probesSubject;
  private Vertx vertx;
  private EventBus eventBus;
  private String probeName;
  private boolean local;
  private long maxTime;
  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    final JsonObject configuration = config == null ? new JsonObject() : config;
    if(probesSubject != null && !probesSubject.isEmpty()) {
      throw new IllegalStateException("probe already initialized");
    }
    local = configuration.getBoolean("local", true);
    maxTime = configuration.getLong("max-time", 20L);
    probeName = "eventbus-" + (local ? "local" : "remote");
    probesSubject = "probes." + currentTimeMillis();
    this.vertx = vertx;
    eventBus = vertx.eventBus();
    final Handler<Message<Object>> handler = e -> e.reply(new JsonObject().put("status", "ok"));
    if(local) {
      eventBus.localConsumer(probesSubject).handler(handler);
    } else {
      eventBus.consumer(probesSubject).handler(handler);
    }
    return Future.succeededFuture();
  }

  @Override
  public String getName() {
    return probeName;
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    final long start = currentTimeMillis();
    return eventBus.request(probesSubject, new JsonObject().put("payload", "42"), new DeliveryOptions().setLocalOnly(local))
      .map(response -> new HealthCheckProbeResult(getName(), (currentTimeMillis() - start) <= maxTime, null));
  }
}
