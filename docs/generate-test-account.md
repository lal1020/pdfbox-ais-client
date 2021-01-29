# Generate a Swisscom AIS test account
To start using the Swisscom AIS service and this client library, you need to get a test
account from Swisscom. This section walks you through the process. For reference, you can have a look
at the documentation available for [Swisscom Trust Services (AIS included)](https://trustservices.swisscom.com/en/downloads/) and, more specifically, to the
[Reference Guide for AIS](http://documents.swisscom.com/product/1000255-Digital_Signing_Service/Documents/Reference_Guide/Reference_Guide-All-in-Signing-Service-en.pdf).

The authentication between an AIS client and the AIS service relies on TLS client authentication. Therefore, you need a certificate
that is enrolled on the Swisscom AIS side. For these steps, a local installation of [OpenSSL](https://www.openssl.org/) is needed
(for Windows, the best option is to use the one that comes with GIT for Windows (see _<git>/usr/bin/openssl.exe_)).

Generate first a private key:
```shell
openssl genrsa -des3 -out my-ais.key 2048
```
Then generate a Certificate Signing Request (CSR):
```shell
openssl req -new -key my-ais.key -out my-ais.csr
```
You will be asked for the following:
```text
Country Name (2 letter code) [AU]: US
State or Province Name (full name) [Some-State]: YourCity
Locality Name (eg, city) []: YourCity
Organization Name (eg, company) [Internet Widgits Pty Ltd]: TEST Your Company
Organizational Unit Name (eg, section) []: For test purposes only
Common Name (e.g. server FQDN or YOUR name) []: TEST Your Name
Email Address []: your.name@yourmail.com
```

Then generate a self-signed certificate (the duration must be 90 days):
```shell
openssl x509 -req -days 90 -in my-ais.csr -signkey my-ais.key -out my-ais.crt
```
The resulting certificate needs to be sent to the Swisscom AIS team for creating a
test account linked to this certificate. This might vary from case to case, so please get in touch with Swisscom and discuss the final
step for authorizing the certificate.
