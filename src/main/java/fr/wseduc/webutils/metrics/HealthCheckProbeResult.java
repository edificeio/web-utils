package fr.wseduc.webutils.metrics;

import io.vertx.core.json.JsonObject;

public class HealthCheckProbeResult {
  private final String name;
  private final boolean ok;
  private final JsonObject metadata;

  public HealthCheckProbeResult(String name, boolean ok, JsonObject metadata) {
    this.name = name;
    this.ok = ok;
    this.metadata = metadata;
  }

  public String getName() {
    return name;
  }

  public boolean isOk() {
    return ok;
  }

  public JsonObject getMetadata() {
    return metadata;
  }
}
