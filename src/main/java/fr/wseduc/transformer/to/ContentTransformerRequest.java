package fr.wseduc.transformer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.Set;

/**
 * Content transformer service request
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTransformerRequest {

    /**
     * Transformation formats requested
     */
    private final Set<ContentTransformerFormat> requestedFormats;
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
     * @param requestedFormats requested transformation formats
     * @param contentVersion version of content to transform
     * @param htmlContent html content to transform into json
     * @param jsonContent json content to transform into html
     */
    @JsonCreator
    public ContentTransformerRequest(@JsonProperty("action") Set<ContentTransformerFormat> requestedFormats,
                                     @JsonProperty("contentVersion") int contentVersion,
                                     @JsonProperty("htmlContent") String htmlContent,
                                     @JsonProperty("jsonContent") JsonObject jsonContent) {
        this.requestedFormats = requestedFormats;
        this.contentVersion = contentVersion;
        this.htmlContent = htmlContent;
        this.jsonContent =jsonContent;
    }

    public Set<ContentTransformerFormat> getRequestedFormats() {
        return requestedFormats;
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
