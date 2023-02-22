package fr.wseduc.sms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Execution report of a SMS sending job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsSendingReport {
    /** Ids of the SMS sent by the provider (can be used for tracking). */
    private final String[] ids;
    /** Recipients whose numbers are not valid and to whom the SMS could not be sent.*/
    private final String[] invalidReceivers;
    /** Recipients whose numbers are valid and to whom the SMS could be sent.*/
    private final String[] validReceivers;

    @JsonCreator
    public SmsSendingReport(@JsonProperty("ids") final String[] ids,
                           @JsonProperty("invalidReceivers") final String[] invalidReceivers,
                           @JsonProperty("validReceivers") final String[] validReceivers) {
        this.ids = ids;
        this.invalidReceivers = invalidReceivers;
        this.validReceivers = validReceivers;
    }

    public String[] getIds() {
        return ids;
    }

    public String[] getInvalidReceivers() {
        return invalidReceivers;
    }

    public String[] getValidReceivers() {
        return validReceivers;
    }
}
