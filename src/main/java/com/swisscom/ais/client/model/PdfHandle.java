package com.swisscom.ais.client.model;

import com.swisscom.ais.client.utils.Trace;

import static com.swisscom.ais.client.utils.Utils.valueNotEmpty;

public class PdfHandle {

    private String inputFromFile;

    private String outputToFile;

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

    public void validateYourself(Trace trace) {
        valueNotEmpty(inputFromFile, "The inputFromFile cannot be null or empty", trace);
        valueNotEmpty(outputToFile, "The outputToFile cannot be null or empty", trace);
    }

}
