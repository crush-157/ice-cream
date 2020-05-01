# ice-cream

This is an example to show how to add a local jar (i.e. not in a central Maven repository to a Java Fn function).

To do this you need to do a Docker Multi - Stage build.

At the build stage, you will:
- add the local jar(s),
- install them into the Maven repository within the build container
- update the pom.xml
- run the Maven build

For the production stage, you copy the output of the build stage into the new image and update the `CMD`

## Dockerfile
The first thing we need is a Dockerfile.

The easiest way to do this is to create a boilerplate function:

`fn init --runtime java dummy`

`cd` into the `dummy` directory and run an fn build with the `--verbose` flag to see the steps of the Docker build:

```
$ fn build --verbose                                  [14:57:34]
Building image fra.ocir.io/oraseemeatechse/crush157/dummy:0.0.1 
FN_REGISTRY:  fra.ocir.io/oraseemeatechse/crush157
Current Context:  default
Sending build context to Docker daemon  14.34kB
Step 1/11 : FROM fnproject/fn-java-fdk-build:jdk11-1.0.108 as build-stage
 ---> e3c33b854f29
Step 2/11 : WORKDIR /function
 ---> Using cache
 ---> c0bcff391281
Step 3/11 : ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository
 ---> Using cache
 ---> aae7552e4aed
Step 4/11 : ADD pom.xml /function/pom.xml
 ---> Using cache
 ---> b199c9d67213
Step 5/11 : RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]
 ---> Using cache
 ---> da9254c64c68
Step 6/11 : ADD src /function/src
 ---> Using cache
 ---> 4851da59cf62
Step 7/11 : RUN ["mvn", "package"]
 ---> Using cache
 ---> 02bd3419bff8
Step 8/11 : FROM fnproject/fn-java-fdk:jre11-1.0.108
 ---> ea31ad6990fe
Step 9/11 : WORKDIR /function
 ---> Using cache
 ---> 0eb543f603e0
Step 10/11 : COPY --from=build-stage /function/target/*.jar /function/app/
 ---> Using cache
 ---> cc501a8df196
Step 11/11 : CMD ["com.example.fn.HelloFunction::handleRequest"]
 ---> Using cache
 ---> 70c359b81d22
Successfully built 70c359b81d22
Successfully tagged fra.ocir.io/oraseemeatechse/crush157/dummy:0.0.1

Function fra.ocir.io/oraseemeatechse/crush157/dummy:0.0.1 built successfully.
```

Copy and paste the steps into an empty `Dockerfile`

## func.yaml
