
package com.swisscom.ais.client.rest.model.signreq;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "DocumentHash"
})
public class InputDocuments {

    @JsonProperty("DocumentHash")
    private List<DocumentHash> documentHash = new ArrayList<DocumentHash>();

    @JsonProperty("DocumentHash")
    public List<DocumentHash> getDocumentHash() {
        return documentHash;
    }

    @JsonProperty("DocumentHash")
    public void setDocumentHash(List<DocumentHash> documentHash) {
        this.documentHash = documentHash;
    }

    public InputDocuments withDocumentHash(List<DocumentHash> documentHash) {
        this.documentHash = documentHash;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(InputDocuments.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("documentHash");
        sb.append('=');
        sb.append(((this.documentHash == null)?"<null>":this.documentHash));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
