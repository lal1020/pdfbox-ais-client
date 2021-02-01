# Build or download the AIS client
To get the binary package of the AIS client library, you can either build it yourself or download it from the Release section of this repository.

## Build the client
The AIS client library can be built using Maven. Clone the repository in a local folder of yours and run this command:

```shell
mvn install
```

Maven will build the final packages and install them in your repository. You can also find them in the _target_ folder in the location
where you run the above command. There are 2 packages that are built:

- the _target/pdfbox-ais-X.X.X.jar_, which is the smaller file (~140 KB) and contains the binary files of the AIS client, 
  without any of the dependencies
- the _target/pdfbox-ais-X.X.X-full.jar_, which is the larger file (~15 MB) and contains the binary files of the AIS client 
  plus all the dependencies, in one single JAR file 

You might also be interested in the _target/dependecies_ folder that contains all the dependencies that the AIS client has (including the
transitive ones). If you use the library in a project that does not resolve dependencies automatically (e.g. Maven like or Gradle like), then
you will probably need to embed these files as well.

## Download the client
The AIS client library can also be downloaded directly, without having to build it yourself.

There are 2 packages available:

- the client JAR file, containing only the binary files of the client. The dependencies are bundled in the same package 
  in the _dependencies_ folder
- the full JAR file, containing all the needed binary files (dependencies included). This one is the easiest one to use 
  for the command line use case
