
package com.swisscom.ais.client.rest.model.signresp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "@WhichDocument",
    "Base64Signature"
})
public class ScExtendedSignatureObject {

    @JsonProperty("@WhichDocument")
    private String whichDocument;
    @JsonProperty("Base64Signature")
    private Base64Signature base64Signature;

    @JsonProperty("@WhichDocument")
    public String getWhichDocument() {
        return whichDocument;
    }

    @JsonProperty("@WhichDocument")
    public void setWhichDocument(String whichDocument) {
        this.whichDocument = whichDocument;
    }

    public ScExtendedSignatureObject withWhichDocument(String whichDocument) {
        this.whichDocument = whichDocument;
        return this;
    }

    @JsonProperty("Base64Signature")
    public Base64Signature getBase64Signature() {
        return base64Signature;
    }

    @JsonProperty("Base64Signature")
    public void setBase64Signature(Base64Signature base64Signature) {
        this.base64Signature = base64Signature;
    }

    public ScExtendedSignatureObject withBase64Signature(Base64Signature base64Signature) {
        this.base64Signature = base64Signature;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScExtendedSignatureObject.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("whichDocument");
        sb.append('=');
        sb.append(((this.whichDocument == null)?"<null>":this.whichDocument));
        sb.append(',');
        sb.append("base64Signature");
        sb.append('=');
        sb.append(((this.base64Signature == null)?"<null>":this.base64Signature));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
