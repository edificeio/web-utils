package fr.wseduc.transformer;

import fr.wseduc.transformer.to.ContentTransformerResponse;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link ContentTransformerResponse}
 */
public class ContentTransformerResponseTest {

    /**
     * Testing partial json deserialization and nullable fields
     */
    @Test
    public void mapToTest() {
        JsonObject jsonBody = new JsonObject("{\"contentVersion\":0,\"jsonContent\":{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Le lorem ipsum est, en imprimerie, une suite de mots sans signification utilisée à titre provisoire pour calibrer une mise en pag\"}]}]},\"plainTextContent\":\"Le lorem ipsum est, en imprimerie, une suite de mots sans signification utilisée à titre provisoire pour calibrer une mise en pag\"}");
        JsonObject jsonContent = new JsonObject("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Le lorem ipsum est, en imprimerie, une suite de mots sans signification utilisée à titre provisoire pour calibrer une mise en pag\"}]}]}");

        Assert.assertEquals(new ContentTransformerResponse(0, null, jsonContent.getMap(), "Le lorem ipsum est, en imprimerie, une suite de mots sans signification utilisée à titre provisoire pour calibrer une mise en pag", null, null), jsonBody.mapTo(ContentTransformerResponse.class));
    }

}
