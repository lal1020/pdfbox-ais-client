
package com.swisscom.ais.client.rest.model.signresp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "sc.OCSP"
})
public class ScOCSPs {

    @JsonProperty("sc.OCSP")
    private String scOCSP;

    @JsonProperty("sc.OCSP")
    public String getScOCSP() {
        return scOCSP;
    }

    @JsonProperty("sc.OCSP")
    public void setScOCSP(String scOCSP) {
        this.scOCSP = scOCSP;
    }

    public ScOCSPs withScOCSP(String scOCSP) {
        this.scOCSP = scOCSP;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScOCSPs.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("scOCSP");
        sb.append('=');
        sb.append(((this.scOCSP == null)?"<null>":this.scOCSP));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
