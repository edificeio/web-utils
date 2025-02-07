package fr.wseduc.transformer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
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
     * Ids of additional extensions to be applied by the content transformer
     */
    private final Set<String> additionalExtensionIds;

    /**
     * Constructor
     * @param requestedFormats requested transformation formats
     * @param contentVersion version of content to transform
     * @param htmlContent html content to transform into json
     * @param jsonContent json content to transform into html
     * @param additionalExtensionIds ids of additional extensions
     */
    @JsonCreator
    public ContentTransformerRequest(@JsonProperty("requestedFormats") Set<ContentTransformerFormat> requestedFormats,
                                     @JsonProperty("contentVersion") int contentVersion,
                                     @JsonProperty("htmlContent") String htmlContent,
                                     @JsonProperty("jsonContent") JsonObject jsonContent,
                                     @JsonProperty("additionalExtensionIds") Set<String> additionalExtensionIds) {
        this.requestedFormats = requestedFormats;
        this.contentVersion = contentVersion;
        this.htmlContent = htmlContent;
        this.jsonContent = jsonContent;
        this.additionalExtensionIds = additionalExtensionIds;
    }

    /**
     * Constructor with empty additionalExtensionIds
     * @param requestedFormats requested transformation formats
     * @param contentVersion version of content to transform
     * @param htmlContent html content to transform into json
     * @param jsonContent json content to transform into html
     */
    public ContentTransformerRequest(Set<ContentTransformerFormat> requestedFormats, int contentVersion, String htmlContent, JsonObject jsonContent) {
        this(requestedFormats, contentVersion, htmlContent, jsonContent, Collections.emptySet());
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

    public Set<String> getAdditionalExtensionIds() {
        return additionalExtensionIds;
    }
}

