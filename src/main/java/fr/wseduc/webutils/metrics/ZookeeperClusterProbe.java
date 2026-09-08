package fr.wseduc.webutils.metrics;

import fr.wseduc.webutils.Controller;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.curator.framework.CuratorFramework;

import static io.vertx.core.Future.succeededFuture;

/**
 * Checks that Zookeeper is reachable and that a simple operation can be executed.
 */
public class ZookeeperClusterProbe implements HealthCheckProbe {
  private static final Logger log = LoggerFactory.getLogger(ZookeeperClusterProbe.class);
  private Vertx vertx;
  private ZookeeperClusterManager zookeeperClusterManager;

  @Override
  public Future<Void> init(final Vertx vertx, final JsonObject config) {
    this.vertx = vertx;
    this.zookeeperClusterManager = (ZookeeperClusterManager) ((VertxInternal) vertx).getClusterManager();
    return succeededFuture();
  }

  @Override
  public String getName() {
    return "zookeeper";
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public Future<HealthCheckProbeResult> probe() {
    if (zookeeperClusterManager == null) {
      return succeededFuture(new HealthCheckProbeResult(getName(), false, 
        new JsonObject().put("error", "Zookeeper cluster manager not available")));
    }
    final String nodeId = zookeeperClusterManager.getNodeId();
    if (zookeeperClusterManager.getNodes().contains(nodeId)) {
      return succeededFuture(new HealthCheckProbeResult(getName(), true, null));
    } else {
      log.warn("Liveness check failed: node " + nodeId + " not found in cluster nodes");
      return succeededFuture(new HealthCheckProbeResult(getName(), false, new JsonObject()
              .put("test", "ko")
              .put("error", "Node not registered in cluster")
              .put("nodeId", nodeId)));
    }
  }
}
