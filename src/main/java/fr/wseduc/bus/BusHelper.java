package fr.wseduc.bus;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Helper class designed to help the use of VertX bus.
 */
public class BusHelper {
    /**
     * <p>Transforms the payload as a JsonObject (iff it is a complexe object) and send it on the bus.</p>
     *
     * <p>NB : The underlying serialization should not be taken for granted (it is now a {@code JsonObject} but could
     * as well be a {@code String} or something else)</p>
     * @param message Message for which we want to send a reply
     * @param payload Payload of the reply
     */
    public static void reply(final Message<?> message, final Object payload) {
        if(payload == null) {
            message.reply(null);
        } else if(payload instanceof JsonObject || payload instanceof String) {
            message.reply(payload);
        } else {
            message.reply(JsonObject.mapFrom(payload));
        }
    }
}
