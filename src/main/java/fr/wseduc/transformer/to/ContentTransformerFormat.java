package fr.wseduc.transformer.to;


import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentTransformerFormat {

    HTML("html"),
    JSON("json"),
    PLAINTEXT("plainText");

    private final String value;

    ContentTransformerFormat(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
