
package com.swisscom.ais.client.rest.model.signresp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Other",
    "Timestamp"
})
public class SignatureObject {

    @JsonProperty("Other")
    private Other other;
    @JsonProperty("Timestamp")
    private Timestamp timestamp;

    @JsonProperty("Other")
    public Other getOther() {
        return other;
    }

    @JsonProperty("Other")
    public void setOther(Other other) {
        this.other = other;
    }

    public SignatureObject withOther(Other other) {
        this.other = other;
        return this;
    }

    @JsonProperty("Timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @JsonProperty("Timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public SignatureObject withTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SignatureObject.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("other");
        sb.append('=');
        sb.append(((this.other == null)?"<null>":this.other));
        sb.append(',');
        sb.append("timestamp");
        sb.append('=');
        sb.append(((this.timestamp == null)?"<null>":this.timestamp));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
