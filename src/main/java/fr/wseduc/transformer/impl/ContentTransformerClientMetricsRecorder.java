package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.to.ContentTransformerAction;
import static fr.wseduc.webutils.metrics.MetricsUtils.getSla;
import static fr.wseduc.webutils.metrics.MetricsUtils.setTimerSla;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.backends.BackendRegistries;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import static java.util.Collections.emptyList;
import io.micrometer.core.instrument.Counter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ContentTransformerClientMetricsRecorder implements IContentTransformerClientMetricsRecorder {

    private final Timer sendingTimes;
    private final Counter successCounter;
    private final Counter failureCounter;

    public ContentTransformerClientMetricsRecorder(final Configuration configuration) {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry == null) {
            throw new IllegalStateException("micrometer.registries.empty");
        }
        final String[] tags = new String[]{"app", configuration.appName};
        sendingTimes = setTimerSla(
                Timer.builder("content.transform.time")
                .tags(tags)
                .description("time to transform content"),
                configuration.sla, 100
        ).register(registry);
        successCounter = Counter.builder("content.transform.ok")
                .description("number of times a content was successfully transformed")
                .tags(tags)
                .register(registry);
        failureCounter = Counter.builder("content.transform.ko")
                .description("number of times an error occurred while trying to transform a content")
                .tags(tags)
                .register(registry);

    }

    @Override
    public void onTransformSuccess(final ContentTransformerAction action, final long durationInMs) {
        onTransform(true, action, durationInMs);
    }

    @Override
    public void onTransformFailure(final ContentTransformerAction action, final long durationInMs) {
        onTransform(false, action, durationInMs);
    }

    public void onTransform(final boolean success, final ContentTransformerAction action, final long durationInMs) {
        sendingTimes.record(durationInMs, TimeUnit.MILLISECONDS);
        if(success) {
            successCounter.increment();
        } else {
            failureCounter.increment();
        }
    }

    public static class Configuration {
        final String appName;
        final List<Duration> sla;

        private Configuration(final String appName, final List<Duration> sla) {
            this.appName = appName;
            this.sla = sla;
        }

        public static Configuration fromJson(final String appName, final JsonObject conf) {
            final List<Duration> sla;
            if(conf == null) {
                sla = emptyList();
            } else {
                sla = getSla("sla", conf);
            }
            return new Configuration(appName, sla);
        }
    }
}
