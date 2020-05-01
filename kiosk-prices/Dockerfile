FROM fnproject/fn-java-fdk-build:jdk11-1.0.108 as build-stage
WORKDIR /function

ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository

# Copy in local jars from either another image or local filesystem
COPY ice-cream-1.0-SNAPSHOT.jar local-jars/

# Install each local jar into maven repository
RUN mvn install:install-file -Dfile=local-jars/ice-cream-1.0-SNAPSHOT.jar \
  -DgroupId=com.oracle.emeatechnology -DartifactId=icecream \
  -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

# Copy in pom.xml *Updated with the dependencies you just installed*
ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]

# Copy in source code
ADD src /function/src
RUN ["mvn", "package"]

FROM fnproject/fn-java-fdk:jre11-1.0.108
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

# New command:
CMD ["com.example.fn.PriceList::handleRequest"]
