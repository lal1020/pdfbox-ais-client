
package com.swisscom.ais.client.rest.model.signresp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "sc.CRL"
})
public class ScCRLs {

    @JsonProperty("sc.CRL")
    private String scCRL;

    @JsonProperty("sc.CRL")
    public String getScCRL() {
        return scCRL;
    }

    @JsonProperty("sc.CRL")
    public void setScCRL(String scCRL) {
        this.scCRL = scCRL;
    }

    public ScCRLs withScCRL(String scCRL) {
        this.scCRL = scCRL;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScCRLs.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("scCRL");
        sb.append('=');
        sb.append(((this.scCRL == null)?"<null>":this.scCRL));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
