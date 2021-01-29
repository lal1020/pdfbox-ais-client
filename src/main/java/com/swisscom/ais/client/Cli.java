package com.swisscom.ais.client;

import com.swisscom.ais.client.impl.AisClientImpl;
import com.swisscom.ais.client.model.PdfHandle;
import com.swisscom.ais.client.model.RevocationInformation;
import com.swisscom.ais.client.model.SignatureStandard;
import com.swisscom.ais.client.model.UserData;
import com.swisscom.ais.client.rest.RestClientConfiguration;
import com.swisscom.ais.client.rest.RestClientImpl;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

            switch (type) {
                case "static": {
                    aisClient.signWithStaticCertificate(Collections.singletonList(document), userData);
                    break;
                }
                case "ondemand": {
                    aisClient.signWithOnDemandCertificate(Collections.singletonList(document), userData);
                    break;
                }
                case "ondemand-stepup": {
                    aisClient.signWithOnDemandCertificateAndStepUp(Collections.singletonList(document), userData);
                    break;
                }
                case "timestamp": {
                    aisClient.timestamp(Collections.singletonList(document), userData);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid type: " + type);
                }
            }
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
        System.out.println(SEPARATOR);
        System.out.println("Swisscom AIS Client - Command line interface");
        System.out.println(SEPARATOR);
        System.out.println("Usage: java -jar pdfbox-ais-X.X.X-full.jar [OPTIONS]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("    -init                                               - Create sample configuration files in the current folder");
        System.out.println("    -input [FILE]                                       - Source PDF file to sign");
        System.out.println("    -output [FILE]                                      - Output PDF file, where the signed document should be written");
        System.out.println("    -type [static|ondemand|ondemand-stepup|timestamp]   - The type of signature to create");
        System.out.println(
            "    -config [PROPERTIES FILE]                           - The properties file that provides the extra configuration parameters. "
            + "Use -init to create a sample file. If you don't specify a file, by default config.properties is used");
        System.out.println("    -help                                               - This help text");
        System.out
            .println("    -v                                                  - Be verbose about what is going on (sets Logback config to info)");
        System.out.println(
            "    -vv                                                 - Be EXTRA verbose about what is going on (sets Logback config to debug)");
        System.out.println();
        System.out.println("Use case:");
        System.out.println("    1. > java -jar pdfbox-ais-X.X.X-full.jar -init   => Have the config files generated for you in the current folder");
        System.out.println("    2. Edit the files accordingly");
        System.out.println("    3. > java -jar pdfbox-ais-X.X.X-full.jar -config config.properties -input fileIn.pdf -output fileOut.pdf -type timestamp");
    }

    private static void runInit() {
        String[][] configPairs = new String[][]{
            new String[]{"/sample-files/config-sample.properties", "config.properties"},
            new String[]{"/sample-files/logback-sample.xml", "logback.xml"}
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
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                InputStream is = Cli.class.getResourceAsStream(inputFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesRead);
                }
                is.close();
                fos.close();
            } catch (IOException e) {
                throw new AisClientException("Failed to create the config file: [" + outputFile + "]");
            }
        }
    }

    private static void configureLogback() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        switch (verboseLevel) {
            case 0: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "warn", loggerContext);
                setLoggerToLevel("org.apache.hc", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.config", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.protocol", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.requestResponse", "warn", loggerContext);
                setLoggerToLevel("swisscom.ais.client.fullRequestResponse", "warn", loggerContext);
                break;
            }
            case 1: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "info", loggerContext);
                setLoggerToLevel("org.apache.hc", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.config", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.protocol", "info", loggerContext);
                setLoggerToLevel("swisscom.ais.client.requestResponse", "debug", loggerContext);
                setLoggerToLevel("swisscom.ais.client.fullRequestResponse", "warn", loggerContext);
                break;
            }
            case 2: {
                setLoggerToLevel(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME, "debug", loggerContext);
                setLoggerToLevel("org.apache.hc", "debug", loggerContext);
                setLoggerToLevel("swisscom.ais.client", "debug", loggerContext);
                setLoggerToLevel("swisscom.ais.client.config", "debug", loggerContext);
                setLoggerToLevel("swisscom.ais.client.protocol", "debug", loggerContext);
                setLoggerToLevel("swisscom.ais.client.requestResponse", "warn", loggerContext);
                setLoggerToLevel("swisscom.ais.client.fullRequestResponse", "debug", loggerContext);
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
