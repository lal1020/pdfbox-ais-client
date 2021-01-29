# Usage as a command-line tool

The AIS client can be used from the command line as a standalone tool.

## Usage
The following is a help listing with the available parameters: 
```text
--------------------------------------------------------------------------------
Swisscom AIS Client - Command line interface
--------------------------------------------------------------------------------
Usage: java -jar pdfbox-ais-X.X.X-full.jar [OPTIONS]

OPTIONS:
    -init                                               - Create sample configuration files in the current folder
    -input [FILE]                                       - Source PDF file to sign
    -output [FILE]                                      - Output PDF file, where the signed document should be written
    -type [static|ondemand|ondemand-stepup|timestamp]   - The type of signature to create
    -config [PROPERTIES FILE]                           - The properties file that provides the extra configuration parameters. Use -init to create a sample file. If you don't specify a file, by default config.properties is used
    -help                                               - This help text
    -v                                                  - Be verbose about what is going on (sets Logback config to info)
    -vv                                                 - Be EXTRA verbose about what is going on (sets Logback config to debug)

Use case:
    1. > java -jar pdfbox-ais-X.X.X-full.jar -init   => Have the config files generated for you in the current folder
    2. Edit the files accordingly
    3. > java -jar pdfbox-ais-X.X.X-full.jar -config config.properties -input fileIn.pdf -output fileOut.pdf -type timestamp
```

Use the _-init_ parameter to create a set of configuration files in the local folder. These files can then be customized for your own case.

Use the _-v_ and _-vv_ parameters to have more detailed logs in the console, including the exchanged HTTP request and response fragments. 

## Examples
Start with a fresh set of configuration files:
```shell
java -jar pdfbox-ais-1.0.0-full.jar -init
```

Timestamp a PDF document:
```shell
java -jar pdfbox-ais-1.0.0-full.jar -type timestamp -input local-sample-doc.pdf -output timestamp-output.pdf
```

Sign a PDF document with an On Demand signature with Step Up:
```shell
java -jar pdfbox-ais-1.0.0-full.jar -type ondemand-stepup -input local-sample-doc.pdf -output signature-output.pdf
```
