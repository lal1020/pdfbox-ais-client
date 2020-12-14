#!/usr/bin/env bash

generate_pojo() {
    the_file=$1
    the_package=$2
    echo "Generating POJOs from: '$the_file' in package: '$the_package' ..."
    jsonschema2pojo.bat \
      --source $the_file \
      --source-type JSON \
      --target gen/src/main/java \
      --package $the_package \
      --omit-hashcode-and-equals \
      --output-encoding UTF-8 \
      --target-language JAVA \
      --generate-builders
}

generate_pojo AIS_SignRequest.json com.swisscom.ais.client.rest.model.signreq
generate_pojo AIS_SignResponse.json com.swisscom.ais.client.rest.model.signresp

# jsonschema2pojo --source AIS_SignRequest.json --source-type JSON --target gen/src/main/java --package com.swisscom.ais.client.rest.model.signreq --omit-hashcode-and-equals --output-encoding UTF-8 --target-language JAVA --generate-builders
# jsonschema2pojo --source AIS_SignResponse.json --source-type JSON --target gen/src/main/java --package com.swisscom.ais.client.rest.model.signresp --omit-hashcode-and-equals --output-encoding UTF-8 --target-language JAVA --generate-builders
