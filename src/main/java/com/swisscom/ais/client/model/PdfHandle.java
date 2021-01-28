package com.swisscom.ais.client.model;

import com.swisscom.ais.client.rest.model.DigestAlgorithm;
import com.swisscom.ais.client.utils.Trace;

import static com.swisscom.ais.client.utils.Utils.valueNotEmpty;
import static com.swisscom.ais.client.utils.Utils.valueNotNull;

public class PdfHandle {

    private String inputFromFile;

    private String outputToFile;

    private DigestAlgorithm digestAlgorithm = DigestAlgorithm.SHA512;

    public String getInputFromFile() {
        return inputFromFile;
    }

    public void setInputFromFile(String inputFromFile) {
        this.inputFromFile = inputFromFile;
    }

    public String getOutputToFile() {
        return outputToFile;
    }

    public void setOutputToFile(String outputToFile) {
        this.outputToFile = outputToFile;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public void validateYourself(Trace trace) {
        valueNotEmpty(inputFromFile, "The inputFromFile cannot be null or empty", trace);
        valueNotEmpty(outputToFile, "The outputToFile cannot be null or empty", trace);
        valueNotNull(digestAlgorithm, "The digest algorithm for a PDF handle cannot be NULL", trace);
    }

}
