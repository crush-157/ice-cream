FROM fnproject/fn-java-fdk-build:jdk11-1.0.104 as build-stage
WORKDIR /function

# Copy in local jars from either another image or local filesystem
COPY ice-cream-1.0-SNAPSHOT.jar local-jars/

# Install each local jar into maven repository
RUN mvn install:install-file -Dfile=local-jars/ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

# Copy in pom.xml *Updated with the dependencies you just installed*
COPY pom.xml .

# Copy in source code
COPY src/ src/

RUN ["mvn", "package", "dependency:copy-dependencies", \
     "-DincludeScope=runtime", "-Dmdep.prependGroupId=true",\
     "-DoutputDirectory=target", "--fail-never"]

FROM fnproject/fn-java-fdk:jre11-1.0.104
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

CMD ["com.example.fn.PriceList::handleRequest"]
