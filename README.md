# ice-cream

This is an example to show how to add a local jar (i.e. not in a central Maven repository to a Java Fn function).

To do this you need to do a Docker Multi - Stage build.

At the build stage, you will:
- add the local jar(s),
- install them into the Maven repository within the build container
- update the pom.xml
- run the Maven build

For the production stage, you copy the output of the build stage into the new image and update the `CMD`

This method _assumes that your jars will are compatible with the JDK included in the standard Fn images (more on this below)_

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

Note the version of the JDK indicated in the image tags (in this case 11-1.0.108).

If your jar is compatible with this then carry on.

Otherwise switch to [this method](../hand-made/README.md) for building an image
that isn't based on `fn-java-fdk-build`.

To generate a `Dockerfile` from this, run the following:
`fn build --verbose | grep Step |> Dockerfile.new cut -d ' ' -f4-`

Note: this won't work if you just put `Dockerfile` in the above command!

Rename `Dockerfile.new` to `Dockerfile`

## Change runtime to `docker`
You want Fn to build the function using your custom Dockerfile.
To do this delete the file `func.yaml`.
Then run `fn init`:
```
$ fn init                                             [15:12:18]
Dockerfile found. Using runtime 'docker'.
func.yaml created.
$ cat func.yaml                                       [15:13:12]
schema_version: 20180708
name: dummy
version: 0.0.1
runtime: docker
```

Check that you can build from the Dockerfile:
`fn build --verbose`

## Customise the Dockerfile
Now you need to customise the Dockerfile to include your local jar(s),
and use them at build time and run time.

In this example, we're going to add a jar `ice-cream-1.0-SNAPSHOT.jar`,
which gives you prices for ice cream and allows you to order
either by the cone from a Kiosk, or by the litre online.

### Copy in your local jars

Add the following the the Dockerfile in the build stage as step 4 :

`COPY ice-cream-1.0-SNAPSHOT.jar local-jars/`

### Install the local jars into the local Maven repository

As step 5, add the following:
```
RUN mvn install:install-file -Dfile=local-jars/ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
```

### Change your `CMD`
The Fn JDK image helpfully sets the `ENTRYPOINT`, so all you have to do is
set the `CMD` as the last line of the `Dockerfile`, for example:

`CMD ["com.example.fn.KioskOrder::handleRequest"]`

### Review
Your Dockerfile should now look like:
```
FROM fnproject/fn-java-fdk-build:jdk11-1.0.108 as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository
COPY ice-cream-1.0-SNAPSHOT.jar local-jars/
RUN mvn install:install-file -Dfile=local-jars/ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true",
"-DoutputDirectory=target", "--fail-never"]
ADD src /function/src
RUN ["mvn", "package"]
FROM fnproject/fn-java-fdk:jre11-1.0.108
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/
CMD ["com.example.fn.KioskOrder::handleRequest"]
```

## Customise the `pom.xml`
For each of your local jars add a dependency to the `pom.xml` that matches
what you specified in the Maven install command above.
```
<dependencies>
    <!-- begin local-jars -->
    <dependency>
        <groupId>com.oracle.emeatechnology</groupId>
        <artifactId>icecream</artifactId>
        <version>1.0</version>
    </dependency>
    <!-- end local-jars -->
```

## Deploy
Check `fn build --verbose` works.

If it does, then run `fn deploy`, e.g.
```
fn deploy --app ice-cream
$ fn invoke ice-cream kiosk-order < bubblegum.json
6.25
```
Sadly, you don't get an ice cream :-(

## Example
For a working example, see the code in this repository (the `Dockerfile`
has been edited to improve readability).
