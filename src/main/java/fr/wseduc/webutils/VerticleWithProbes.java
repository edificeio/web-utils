package fr.wseduc.webutils;

import fr.wseduc.webutils.metrics.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.vertx.java.core.http.RouteMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Server.getPathPrefix;
import static io.vertx.core.Future.all;
import static io.vertx.core.Future.succeededFuture;

public abstract class VerticleWithProbes extends AbstractVerticle {

    protected Logger log;
    protected final List<HealthCheckProbe> readinessProbes = new ArrayList<>();
    protected final List<HealthCheckProbe> livenessProbes = new ArrayList<>();
    protected long probeTimeout = 10_000L;

    /**
     * Read probes from the configuration and initialize them.
     * The configuration is read from the "probes" field and is a mixed list of :
     * - string, which is the fully qualified name of the probe to instantiate
     * - object, with the fields :
     *    - name, which is the fully qualified name of the probe to instantiate
     *    - config, which is a JsonObject containing configuration parameters for the probe
     * @return A future that completes when the initialization of all probes is done.
     */
    protected Future<Void> initializeProbes(final String id) {
        log = LoggerFactory.getLogger(this.getClass());
        final JsonObject config = getConfig();
        this.probeTimeout = config.getLong("probes-timeout", 5_000L);
        return all(getDefaultReadinessProbes(id))
        .compose(ready -> {
            readinessProbes.addAll(ready.list());
            return all(getDefaultLivenessProbes(id));
        }).compose(live -> {
            livenessProbes.addAll(live.list());
            return succeededFuture();
        }).compose(defaultInitialized -> {
            final JsonArray probesConf = config.getJsonArray("probes");
            final List<Future<Void>> initProbes = new ArrayList<>();
            if (probesConf != null) {
                boolean readinessProbe = true;
                boolean livenessProbe = false;
                for (Object o : probesConf) {
                    final String probeClassName;
                    final JsonObject conf;
                    if (o instanceof String) {
                        probeClassName = (String) o;
                        conf = new JsonObject();
                    } else if (o instanceof JsonObject) {
                        final JsonObject jo = (JsonObject) o;
                        probeClassName = jo.getString("name");
                        conf = jo.getJsonObject("config");
                        readinessProbe = jo.getBoolean("readiness", true);
                        livenessProbe = jo.getBoolean("liveness", false);
                    } else {
                        log.error("We expect the probes to be a list of string with the name of the probes or an object");
                        continue;
                    }
                    conf.put("id", id);
                    try {
                        final Class<?> probeClass = Class.forName(probeClassName);
                        if (!HealthCheckProbe.class.isAssignableFrom(probeClass)) {
                            log.error("Specified class " + probeClassName + " is not a probe class");
                            continue;
                        }
                        final HealthCheckProbe probe = (HealthCheckProbe) probeClass.newInstance();
                        initProbes.add(probe.init(vertx, conf));
                        if (readinessProbe) {
                            this.readinessProbes.add(probe);
                        }
                        if (livenessProbe) {
                            this.livenessProbes.add(probe);
                        }
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        log.error("Cannot instantiate probe " + probeClassName, e);
                    }
                }
            }
            return all(initProbes)
                    .map(CompositeFuture::list)
                    .mapEmpty();
        });
    }

    protected abstract JsonObject getConfig();

    private List<? extends Future<HealthCheckProbe>> getDefaultLivenessProbes(final String id) {
        final List<Future<HealthCheckProbe>> defaultProbes = new ArrayList<>();
        if(vertx.isClustered() && ((VertxInternal) vertx).getClusterManager() instanceof ZookeeperClusterManager) {
            log.debug("Adding ZK cluster probe");
            final ZookeeperClusterProbe zkProbe = new ZookeeperClusterProbe();
            defaultProbes.add(zkProbe.init(vertx, config()).map(zkProbe));
        } else {
            log.debug("Skipping ZK cluster prob");
        }
        return defaultProbes;
    }

    private List<? extends Future<HealthCheckProbe>> getDefaultReadinessProbes(final String id) {
        final List<Future<HealthCheckProbe>> defaultProbes = new ArrayList<>();
        log.debug("Adding EventBus probe");
        final EventBusProbe eventBusProbe = new EventBusProbe();
        defaultProbes.add(eventBusProbe.init(vertx, new JsonObject().put("local", true).put("id", id)).map(eventBusProbe));
        if(vertx.isClustered()) {
            log.debug("Adding clustered EventBus probe");
            final EventBusProbe clusteredEventBusProbe = new EventBusProbe();
            defaultProbes.add(clusteredEventBusProbe.init(vertx, new JsonObject().put("local", false).put("id", id)).map(clusteredEventBusProbe));
        }
        if(vertx.isClustered() && ((VertxInternal) vertx).getClusterManager() instanceof ZookeeperClusterManager) {
            log.debug("Adding ZK probe");
            final ZookeeperProbe zkProbe = new ZookeeperProbe();
            defaultProbes.add(zkProbe.init(vertx, config()).map(zkProbe));
        } else {
            log.debug("Skipping ZK prob");
        }
        return defaultProbes;
    }

    protected Future<HealthCheckProbeResult> executeProbeWithTimeout(HealthCheckProbe healthCheckProbe) {
        try {
            return healthCheckProbe.probe(probeTimeout);
        } catch (RuntimeException e) {
            return succeededFuture(new HealthCheckProbeResult(healthCheckProbe.getName(), false, new JsonObject().put("exception", e.getMessage())));
        }
    }

    protected JsonObject mergeProbes(final  List<HealthCheckProbeResult> results) {
        final JsonObject merged = new JsonObject();
        for (HealthCheckProbeResult result : results) {
            merged.put(result.getName(), JsonObject.mapFrom(result));
        }
        return merged;
    }

    protected void addLivenessAndReadinessProbes(final RouteMatcher rm) {
        final String prefix = getPathPrefix(getConfig());
        rm.get(prefix + "/health/liveness", event -> {
            evaluateProbes(this.livenessProbes, event);
        });

        rm.get(prefix + "/health/readiness", event -> {
            evaluateProbes(this.readinessProbes, event);
        });
    }

    private void evaluateProbes(final List<HealthCheckProbe> probes, final io.vertx.core.http.HttpServerRequest event) {
        final List<Future<HealthCheckProbeResult>> futures = probes.stream()
                .map(this::executeProbeWithTimeout)
                .collect(Collectors.toList());
        Future.join(futures)
                .onSuccess(res -> {
                    final JsonObject result = mergeProbes(res.list());
                    boolean hasKO = res.<HealthCheckProbeResult>list().stream().anyMatch(p -> !p.isOk());
                    if(hasKO) {
                        Controller.renderError(event, result);
                    } else {
                        Controller.renderJson(event, result);
                    }
                })
                .onFailure(th -> {
                    log.error("An error occurred while getting readiness probe", th);
                    Controller.renderError(event);
                });
    }
}
