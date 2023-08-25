package fr.wseduc.transformer.to;


import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentTransformerAction {

    HTML2JSON("html2json"),
    JSON2HTML("json2html");

    private final String value;

    ContentTransformerAction(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
