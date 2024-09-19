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
     * ID of the resource having this content.
     */
    private final String resourceId;
    /**
     * Type of the resource having this content, for example "post". 
     */
    private final String resourceType;

    /**
     * Constructor
     * @param requestedFormats requested transformation formats
     * @param contentVersion version of content to transform
     * @param htmlContent html content to transform into json
     * @param jsonContent json content to transform into html
     * @param resourceId ID of the resource having this content
     * @param resourceType Type of the resource having this content
     */
    @JsonCreator
    public ContentTransformerRequest(@JsonProperty("requestedFormats") Set<ContentTransformerFormat> requestedFormats,
                                     @JsonProperty("contentVersion") int contentVersion,
                                     @JsonProperty("htmlContent") String htmlContent,
                                     @JsonProperty("jsonContent") JsonObject jsonContent,
                                     @JsonProperty("resourceType") String resourceType,
                                     @JsonProperty("resourceId") String resourceId) {
        this.requestedFormats = requestedFormats;
        this.contentVersion = contentVersion;
        this.htmlContent = htmlContent;
        this.jsonContent =jsonContent;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
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

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }
}

