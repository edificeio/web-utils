package fr.wseduc.webutils;

import com.google.common.collect.ImmutableMap;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Map;

import static fr.wseduc.webutils.security.ActionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class StartupUtilsTest {

    private static SecuredAction CREATE_NO_OVERRIDE =  new SecuredAction("fr.weseduc.controlleur.MyController|create", null, WORKFLOW.name(), "fr.weseduc.controlleur.MyController|create");
    private static SecuredAction UPDATE_NO_OVERRIDE = new SecuredAction("fr.weseduc.controlleur.MyController|update", "info.update", RESOURCE.name(), "fr.weseduc.controlleur.MyController|update");
    private static SecuredAction READ_NO_OVERRIDE = new SecuredAction("fr.weseduc.controlleur.MyController|read", "info.read", AUTHENTICATED.name(), "fr.weseduc.controlleur.MyController|read");
    private static SecuredAction READ_OVERRIDE_NEW_RIGHT = new SecuredAction("fr.weseduc.controlleur.MyController|read", "info.read", AUTHENTICATED.name(), "mycontroller.read");
    private static SecuredAction UPDATE_OVERRIDE_TO_CREATE = new SecuredAction("fr.weseduc.controlleur.MyController|update", "info.update", WORKFLOW.name(), "fr.weseduc.controlleur.MyController|create");
    private static SecuredAction UPDATE_OVERRIDE_TO_CREATE_WRONG_TYPE = new SecuredAction("fr.weseduc.controlleur.MyController|update", "info.update", AUTHENTICATED.name(), "fr.weseduc.controlleur.MyController|create");

    private static JsonObject JO_CREATE_NO_OVERRIDE =  new JsonObject().put("type", WORKFLOW.name()).put("name","fr.weseduc.controlleur.MyController|create").put("right", "fr.weseduc.controlleur.MyController|create");
    private static JsonObject JO_UPDATE_NO_OVERRIDE = new JsonObject().put("type", RESOURCE.name()).put("name", "fr.weseduc.controlleur.MyController|update").put("right", "fr.weseduc.controlleur.MyController|update");
    private static JsonObject JO_READ_NO_OVERRIDE = new JsonObject().put("type", AUTHENTICATED.name()).put("name","fr.weseduc.controlleur.MyController|read").put("right", "fr.weseduc.controlleur.MyController|read");
    private static JsonObject JO_READ_OVERRIDE_NEW_RIGHT = new JsonObject().put("type", AUTHENTICATED.name()).put("name","fr.weseduc.controlleur.MyController|read").put("right", "mycontroller.read");
    private static JsonObject JO_UPDATE_OVERRIDE_TO_CREATE = new JsonObject().put("type", WORKFLOW.name()).put("name","fr.weseduc.controlleur.MyController|update").put("right", "fr.weseduc.controlleur.MyController|create");
    private static JsonObject JO_UPDATE_OVERRIDE_TO_CREATE_WRONG_TYPE = new JsonObject().put("type", RESOURCE.name()).put("name","fr.weseduc.controlleur.MyController|update").put("right", "fr.weseduc.controlleur.MyController|create");


    @Test
    public void applyOverrideRightForShare_noRightOverride() {
        //GIVEN
        Map<String, SecuredAction> actions = ImmutableMap.<String, SecuredAction>builder()
                .put("fr.weseduc.controlleur.MyController|create", CREATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|update", UPDATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|read", READ_NO_OVERRIDE)
                .build();

        //WHEN
        Map<String, SecuredAction> actionsResult = StartupUtils.applyOverrideRightForShare(actions);

        //THEN
        assertThat("All secured actions must be present in the overridden rights", actionsResult.size(), equalTo(3));

        assertThat("Create action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|create"), equalTo(true));
        assertThat("Update action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|update"), equalTo(true));
        assertThat("Read action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|read"), equalTo(true));

        assertThat("Create action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|create"), equalTo(CREATE_NO_OVERRIDE));
        assertThat("Update action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|update"), equalTo(UPDATE_NO_OVERRIDE));
        assertThat("Read action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|read"), equalTo(READ_NO_OVERRIDE));
    }


    @Test
    public void applyOverrideRightForShare_overrideRightWithNewOne() {
        //GIVEN
        Map<String, SecuredAction> actions = ImmutableMap.<String, SecuredAction>builder()
                .put("fr.weseduc.controlleur.MyController|create", CREATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|update", UPDATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|read", READ_OVERRIDE_NEW_RIGHT)
                .build();

        //WHEN
        Map<String, SecuredAction> actionsResult = StartupUtils.applyOverrideRightForShare(actions);

        //THEN
        assertThat("All secured actions must be present in the overridden rights", actionsResult.size(), equalTo(3));

        assertThat("Create action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|create"), equalTo(true));
        assertThat("Update action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|update"), equalTo(true));
        assertThat("Read action must be not present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|read"), equalTo(false));
        assertThat("New read right must present", actionsResult.containsKey("mycontroller.read"), equalTo(true));

        assertThat("Create action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|create"), equalTo(CREATE_NO_OVERRIDE));
        assertThat("Update action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|update"), equalTo(UPDATE_NO_OVERRIDE));
        assertThat("Read action must be present", actionsResult.get("mycontroller.read"), equalTo(READ_OVERRIDE_NEW_RIGHT));
    }

    @Test
    public void applyOverrideRightForShare_overrideRightDuplicate() {
        //GIVEN
        Map<String, SecuredAction> actions = ImmutableMap.<String, SecuredAction>builder()
                .put("fr.weseduc.controlleur.MyController|create", CREATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|update", UPDATE_OVERRIDE_TO_CREATE)
                .put("fr.weseduc.controlleur.MyController|read", READ_NO_OVERRIDE)
                .build();

        //WHEN
        Map<String, SecuredAction> actionsResult = StartupUtils.applyOverrideRightForShare(actions);

        //THEN
        assertThat("2 secured actions must be present in the overridden rights", actionsResult.size(), equalTo(2));

        assertThat("Create action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|create"), equalTo(true));
        assertThat("Update action must not be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|update"), equalTo(false));
        assertThat("Read action must present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|read"), equalTo(true));

        assertThat("Create action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|create"), equalTo(UPDATE_OVERRIDE_TO_CREATE));
        assertThat("Read action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|read"), equalTo(READ_NO_OVERRIDE));
    }

    @Test(expected =  IllegalArgumentException.class)
    public void applyOverrideRightForShare_overrideRightDuplicate_withDifferentType_shouldThrowException() {
        //GIVEN
        Map<String, SecuredAction> actions = ImmutableMap.<String, SecuredAction>builder()
                .put("fr.weseduc.controlleur.MyController|create", CREATE_NO_OVERRIDE)
                .put("fr.weseduc.controlleur.MyController|update", UPDATE_OVERRIDE_TO_CREATE_WRONG_TYPE)
                .put("fr.weseduc.controlleur.MyController|read", READ_NO_OVERRIDE)
                .build();

        //WHEN
        Map<String, SecuredAction> actionsResult = StartupUtils.applyOverrideRightForShare(actions);

        //THEN
        assertThat("2 secured actions must be present in the overridden rights", actionsResult.size(), equalTo(2));

        assertThat("Create action must be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|create"), equalTo(true));
        assertThat("Update action must not be present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|update"), equalTo(false));
        assertThat("Read action must present", actionsResult.containsKey("fr.weseduc.controlleur.MyController|read"), equalTo(true));

        assertThat("Create action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|create"), equalTo(CREATE_NO_OVERRIDE));
        assertThat("Read action must be present", actionsResult.get("fr.weseduc.controlleur.MyController|read"), equalTo(READ_NO_OVERRIDE));
    }


    @Test
    public void applyOverrideRightForRegistry_noRightOverride() {
        //GIVEN
        io.vertx.core.json.JsonArray actions = new io.vertx.core.json.JsonArray()
                    .add(JO_CREATE_NO_OVERRIDE)
                    .add(JO_UPDATE_NO_OVERRIDE)
                    .add(JO_READ_NO_OVERRIDE);

        //WHEN
        io.vertx.core.json.JsonArray actionsResult = StartupUtils.applyOverrideRightForRegistry(actions);

        //THEN
        assertThat("All secured actions must be present in the overridden rights", actionsResult.size(), equalTo(3));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult) != null, equalTo(true));
        assertThat("Update action must be present", extractAction("fr.weseduc.controlleur.MyController|update", actionsResult) != null, equalTo(true));
        assertThat("Read action must be present",extractAction("fr.weseduc.controlleur.MyController|read", actionsResult) != null, equalTo(true));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult), equalTo(JO_CREATE_NO_OVERRIDE));
        assertThat("Update action must be present", extractAction("fr.weseduc.controlleur.MyController|update", actionsResult), equalTo(JO_UPDATE_NO_OVERRIDE));
        assertThat("Read action must be present", extractAction("fr.weseduc.controlleur.MyController|read", actionsResult), equalTo(JO_READ_NO_OVERRIDE));
    }


    @Test
    public void applyOverrideRightForRegistry_overrideRightWithNewOne() {
        //GIVEN
        io.vertx.core.json.JsonArray actions = new io.vertx.core.json.JsonArray()
                .add(JO_CREATE_NO_OVERRIDE)
                .add(JO_UPDATE_NO_OVERRIDE)
                .add(JO_READ_OVERRIDE_NEW_RIGHT);

        //WHEN
        io.vertx.core.json.JsonArray actionsResult = StartupUtils.applyOverrideRightForRegistry(actions);

        //THEN
        assertThat("All secured actions must be present in the overridden rights", actionsResult.size(), equalTo(3));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult) != null, equalTo(true));
        assertThat("Update action must be present", extractAction("fr.weseduc.controlleur.MyController|update", actionsResult) != null, equalTo(true));
        assertThat("Read action must not be present",extractAction("fr.weseduc.controlleur.MyController|read", actionsResult) == null, equalTo(true));
        assertThat("Read override action must be present",extractAction("mycontroller.read", actionsResult) != null, equalTo(true));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult), equalTo(JO_CREATE_NO_OVERRIDE));
        assertThat("Update action must be present", extractAction("fr.weseduc.controlleur.MyController|update", actionsResult), equalTo(JO_UPDATE_NO_OVERRIDE));
        assertThat("Read action must be present", extractAction("mycontroller.read", actionsResult).getString("name"), equalTo(JO_READ_OVERRIDE_NEW_RIGHT.getString("right")));
    }


    @Test
    public void applyOverrideRightForRegistry_overrideRightDuplicate() {
        //GIVEN
        io.vertx.core.json.JsonArray actions = new io.vertx.core.json.JsonArray()
                .add(JO_CREATE_NO_OVERRIDE)
                .add(JO_UPDATE_OVERRIDE_TO_CREATE)
                .add(JO_READ_NO_OVERRIDE);

        //WHEN
        io.vertx.core.json.JsonArray actionsResult = StartupUtils.applyOverrideRightForRegistry(actions);

        //THEN
        assertThat("All secured actions must be present in the overridden rights", actionsResult.size(), equalTo(2));
        assertThat("Secured actions must NOT be mutate", actions.size(), equalTo(3));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult) != null, equalTo(true));
        assertThat("Update action must not be present", extractAction("fr.weseduc.controlleur.MyController|update", actionsResult) == null, equalTo(true));
        assertThat("Read action must be present",extractAction("fr.weseduc.controlleur.MyController|read", actionsResult) != null, equalTo(true));

        assertThat("Create action must be present", extractAction("fr.weseduc.controlleur.MyController|create", actionsResult).getString("name"), equalTo(JO_CREATE_NO_OVERRIDE.getString("name")));
        assertThat("Read action must be present", extractAction("fr.weseduc.controlleur.MyController|read", actionsResult), equalTo(JO_READ_NO_OVERRIDE));
    }


    @Test(expected =  IllegalArgumentException.class)
    public void applyOverrideRightForRegistry_overrideRightDuplicate_withDifferentType_shouldThrowException() {
        io.vertx.core.json.JsonArray actions = new io.vertx.core.json.JsonArray()
                .add(JO_CREATE_NO_OVERRIDE)
                .add(JO_UPDATE_OVERRIDE_TO_CREATE_WRONG_TYPE)
                .add(JO_READ_NO_OVERRIDE);

        //WHEN
        StartupUtils.applyOverrideRightForRegistry(actions);
    }


    private JsonObject extractAction(String actionName, JsonArray actions) {
        return actions.stream().filter(o -> ((JsonObject)o).getString("name").equals(actionName))
                .map(JsonObject.class::cast)
                .findFirst()
                .orElse(null);
    }

}

