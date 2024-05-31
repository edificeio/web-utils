package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClientMetricsRecorder;
import fr.wseduc.transformer.to.ContentTransformerFormat;
import fr.wseduc.transformer.to.ContentTransformerRequest;
import io.micrometer.core.instrument.*;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.backends.BackendRegistries;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.wseduc.webutils.metrics.MetricsUtils.getSla;
import static fr.wseduc.webutils.metrics.MetricsUtils.setTimerSla;
import static java.util.Collections.emptyList;


public class ContentTransformerClientMetricsRecorder implements IContentTransformerClientMetricsRecorder {

    private final Timer sendingTimes;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter failureCounterTooLarge;
    private final DistributionSummary durationPerByte;

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
                configuration.sla, 1000
        ).register(registry);
        successCounter = Counter.builder("content.transform.ok")
                .description("number of times a content was successfully transformed")
                .tags(tags)
                .register(registry);
        failureCounter = Counter.builder("content.transform.ko")
                .description("number of times an error occurred while trying to transform a content")
                .tags(tags)
                .register(registry);
        failureCounterTooLarge = Counter.builder("content.transform.too.large")
          .description("number of times an error occurred because the payload was too large")
                .tags(tags)
                .register(registry);
        durationPerByte = DistributionSummary.builder("content.transform.rate")
                .description("time to transform content per content size")
                .baseUnit("ms/byte")
                .tags(tags)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Override
    public void onTransformSuccess(final ContentTransformerRequest request, final long durationInMs) {
        onTransform(true, request, durationInMs);
    }

    @Override
    public void onTransformFailure(final ContentTransformerRequest request, final long durationInMs, Throwable th) {
        final String exMessage = th.getMessage();
        if(exMessage.endsWith("413")) { // If the error comes from a payload too large then we use a special counter
            failureCounterTooLarge.increment();
        } else {
            onTransform(false, request, durationInMs);
        }
    }

    private void onTransform(final boolean success, final ContentTransformerRequest request, final long durationInMs) {
        sendingTimes.record(durationInMs, TimeUnit.MILLISECONDS);
        if (request.getHtmlContent() != null) {
            durationPerByte.record((double) durationInMs/request.getHtmlContent().length());
        }
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
