package fr.wseduc.transformer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

/**
 * Content transformer service request
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTransformerRequest {

    /**
     * Transformation action to perform
     */
    private final ContentTransformerAction action;
    /**
     * Version of content to transform
     */
    private final int contentVersion;
    /**
     * Html content to transform into json
     */
    private final String htmlContent;
    /**
     * Json content to transform into html
     */
    private final JsonObject jsonContent;

    /**
     * Constructor
     * @param action transformation action to perform
     * @param contentVersion version of content to transform
     * @param htmlContent html content to transform into json
     * @param jsonContent json content to transform into html
     */
    @JsonCreator
    public ContentTransformerRequest(@JsonProperty("action") ContentTransformerAction action,
                                     @JsonProperty("contentVersion") int contentVersion,
                                     @JsonProperty("htmlContent") String htmlContent,
                                     @JsonProperty("jsonContent") JsonObject jsonContent) {
        this.action = action;
        this.contentVersion = contentVersion;
        this.htmlContent = htmlContent;
        this.jsonContent =jsonContent;
    }
    public ContentTransformerRequest(int contentVersion, String htmlContent) {
        this(ContentTransformerAction.HTML2JSON, contentVersion, htmlContent, null);
    }
    public ContentTransformerRequest(int contentVersion, JsonObject jsonContent) {
        this(ContentTransformerAction.JSON2HTML, contentVersion, null, jsonContent);
    }

    public ContentTransformerAction getAction() {
        return action;
    }

    public int getContentVersion() {
        return contentVersion;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public JsonObject getJsonContent() {
        return jsonContent;
    }
}