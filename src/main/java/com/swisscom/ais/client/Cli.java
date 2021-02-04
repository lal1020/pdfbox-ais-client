package com.swisscom.ais.client;

import com.swisscom.ais.client.impl.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.RevocationInformation;
import com.swisscom.ais.client.model.SignatureResult;
import com.swisscom.ais.client.model.SignatureStandard;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;
import com.swisscom.ais.client.utils.Loggers;
import com.swisscom.ais.client.utils.Utils;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class Cli {

    private static final String PARAM_INPUT = "input";
    private static final String PARAM_OUTPUT = "output";
    private static final String PARAM_CONFIG = "config";
    private static final String PARAM_INIT = "init";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_HELP = "help";
    private static final String PARAM_VERBOSE1 = "v";
    private static final String PARAM_VERBOSE2 = "vv";
    private static final String SEPARATOR = "--------------------------------------------------------------------------------";

    private static final String TYPE_STATIC = "static";
    private static final String TYPE_ON_DEMAND = "ondemand";
    private static final String TYPE_ON_DEMAND_STEP_UP = "ondemand-stepup";
    private static final String TYPE_TIMESTAMP = "timestamp";

    // ----------------------------------------------------------------------------------------------------

    private static boolean continueExecution;
    private static String startFolder;

    private static String inputFile;
    private static String outputFile;
    private static String configFile;
    private static String type;
    private static int verboseLevel;

    public static void main(String[] args) throws IOException {
        startFolder = new File("").getAbsolutePath();

        parseArguments(args);
        if (!continueExecution) {
            return;
        }

        configureLogback();

        System.out.println(SEPARATOR);
        System.out.println("Swisscom AIS Client - Command line interface");
        System.out.println(SEPARATOR);
        System.out.println("Starting with following parameters:");
        System.out.println("Config            : " + configFile);
        System.out.println("Input file        : " + inputFile);
        System.out.println("Output file       : " + outputFile);
        System.out.println("Type of signature : " + type);
        System.out.println("Verbose level     : " + verboseLevel);
        System.out.println(SEPARATOR);

        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));

        RestClientConfiguration restConfig = new RestClientConfiguration();
        restConfig.setFromProperties(properties);

        RestClientImpl restClient = new RestClientImpl();
        restClient.setConfiguration(restConfig);

        AisClientConfiguration aisConfig = new AisClientConfiguration();
        aisConfig.setFromProperties(properties);

        try (AisClientImpl aisClient = new AisClientImpl(aisConfig, restClient)) {
            UserData userData = new UserData();
            userData.setFromProperties(properties);
            userData.setConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl));
            userData.setAddRevocationInformation(RevocationInformation.PADES);
            userData.setAddTimestamp(true);
            userData.setSignatureStandard(SignatureStandard.PADES);

            PdfHandle document = new PdfHandle();
            document.setInputFromFile(inputFile);
            document.setOutputToFile(outputFile);

            SignatureResult result;

            switch (type) {
                case TYPE_STATIC: {
                    result = aisClient.signWithStaticCertificate(Collections.singletonList(document), userData);
                    break;
                }
                case TYPE_ON_DEMAND: {
                    result = aisClient.signWithOnDemandCertificate(Collections.singletonList(document), userData);
                    break;
                }
                case TYPE_ON_DEMAND_STEP_UP: {
                    result = aisClient.signWithOnDemandCertificateAndStepUp(Collections.singletonList(document), userData);
                    break;
                }
                case TYPE_TIMESTAMP: {
                    result = aisClient.timestamp(Collections.singletonList(document), userData);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid type: " + type);
                }
            }

            System.out.println(SEPARATOR);
            System.out.println("Final result: " + result);
            System.out.println(SEPARATOR);
        }
    }

    // ----------------------------------------------------------------------------------------------------

    private static void parseArguments(String[] args) {
        if (args.length == 0) {
            showHelp(null);
            return;
        }
        int argIndex = 0;
        while (argIndex < args.length) {
            String currentArg = args[argIndex];
            if (currentArg.startsWith("--")) {
                currentArg = currentArg.substring(2);
            }
            if (currentArg.startsWith("-")) {
                currentArg = currentArg.substring(1);
            }
            switch (currentArg) {
                case PARAM_INIT: {
                    runInit();
                    return;
                }
                case PARAM_HELP: {
                    showHelp(null);
                    return;
                }
                case PARAM_VERBOSE1: {
                    if (verboseLevel == 0) {
                        verboseLevel = 1;
                    }
                    break;
                }
                case PARAM_VERBOSE2: {
                    verboseLevel = 2;
                    break;
                }
                case PARAM_INPUT: {
                    if (argIndex + 1 < args.length) {
                        inputFile = args[argIndex + 1];
                        argIndex++;
                    } else {
                        showHelp("Input file name is missing");
                    }
                    break;
                }
                case PARAM_OUTPUT: {
                    if (argIndex + 1 < args.length) {
                        outputFile = args[argIndex + 1];
                        argIndex++;
                    } else {
                        showHelp("Output file name is missing");
                    }
                    break;
                }
                case PARAM_CONFIG: {
                    if (argIndex + 1 < args.length) {
                        configFile = args[argIndex + 1];
                        argIndex++;
                    } else {
                        showHelp("Config file name is missing. "
                                 + "If you need a sample config file, run this program with the -init argument");
                    }
                    break;
                }
                case PARAM_TYPE: {
                    if (argIndex + 1 < args.length) {
                        type = args[argIndex + 1];
                        argIndex++;
                    } else {
                        showHelp("Type of signature is missing");
                    }
                    break;
                }
            }
            argIndex++;
        }
        if (inputFile == null) {
            showHelp("Input file name is missing");
            return;
        }
        if (outputFile == null) {
            showHelp("Output file name is missing");
            return;
        }
        if (configFile == null) {
            configFile = "config.properties";
        }
        if (type == null) {
            showHelp("Type of signature is missing");
            return;
        }
        continueExecution = true;
    }

    private static void showHelp(String argsValidationError) {
        if (argsValidationError != null) {
            System.out.println(argsValidationError);
        }
        Utils.copyFileFromClasspathToStdout("/cli-files/usage.txt");
    }

    private static void runInit() {
        String[][] configPairs = new String[][]{
            new String[]{"/cli-files/config-sample.properties", "config.properties"},
            new String[]{"/cli-files/logback-sample.xml", "logback.xml"}
        };
        for (String[] configPair : configPairs) {
            String inputFile = configPair[0];
            String baseOutputFile = configPair[1];
            String outputFile = startFolder + "/" + baseOutputFile;
            if (new File(outputFile).exists()) {
                System.out.println("File " + baseOutputFile + " already exists! Will not be overridden!");
                return;
            }
            System.out.println("Writing " + baseOutputFile + " to " + outputFile);
            Utils.copyFileFromClasspathToDisk(inputFile, outputFile);
        }
    }

    private static void configureLogback() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        switch (verboseLevel) {
            case 0: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "warn", loggerContext);
                setLoggerToLevel("org.apache.hc", "info", loggerContext);
                setLoggerToLevel(Loggers.CLIENT, "info", loggerContext);
                setLoggerToLevel(Loggers.CONFIG, "info", loggerContext);
                setLoggerToLevel(Loggers.CLIENT_PROTOCOL, "info", loggerContext);
                setLoggerToLevel(Loggers.REQUEST_RESPONSE, "warn", loggerContext);
                setLoggerToLevel(Loggers.FULL_REQUEST_RESPONSE, "warn", loggerContext);
                setLoggerToLevel(Loggers.PDF_PROCESSING, "warn", loggerContext);
                break;
            }
            case 1: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "info", loggerContext);
                setLoggerToLevel("org.apache.hc", "info", loggerContext);
                setLoggerToLevel(Loggers.CLIENT, "info", loggerContext);
                setLoggerToLevel(Loggers.CONFIG, "info", loggerContext);
                setLoggerToLevel(Loggers.CLIENT_PROTOCOL, "info", loggerContext);
                setLoggerToLevel(Loggers.REQUEST_RESPONSE, "debug", loggerContext);
                setLoggerToLevel(Loggers.FULL_REQUEST_RESPONSE, "warn", loggerContext);
                setLoggerToLevel(Loggers.PDF_PROCESSING, "debug", loggerContext);
                break;
            }
            case 2: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "debug", loggerContext);
                setLoggerToLevel("org.apache.hc", "debug", loggerContext);
                setLoggerToLevel(Loggers.CLIENT, "debug", loggerContext);
                setLoggerToLevel(Loggers.CONFIG, "debug", loggerContext);
                setLoggerToLevel(Loggers.CLIENT_PROTOCOL, "debug", loggerContext);
                setLoggerToLevel(Loggers.REQUEST_RESPONSE, "warn", loggerContext);
                setLoggerToLevel(Loggers.FULL_REQUEST_RESPONSE, "debug", loggerContext);
                setLoggerToLevel(Loggers.PDF_PROCESSING, "debug", loggerContext);
                break;
            }
            default: {
                throw new IllegalStateException("Invalid verboseLevel: " + verboseLevel);
            }
        }
    }

    private static void setLoggerToLevel(String loggerName, String level, LoggerContext loggerContext) {
        Logger logger = loggerContext.getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(Level.toLevel(level));
        }
    }

}
