FROM maven:3.8.5-openjdk-17 as build
RUN mkdir /usr/src/project
COPY . /usr/src/project
WORKDIR /usr/src/project
RUN mvn package -DskipTests
RUN jar xf target/teste.jar
RUN jdeps --ignore-missing-deps -q --recursive --multi-release 17 --print-module-deps  --class-path './BOOT-INF/lib/*'  ./target/teste.jar > ./deps.info
RUN jlink \
    --add-modules $(cat ./deps.info) \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /myjre
FROM debian:sid-slim
ENV JAVA_HOME /user/java/jdk17
ENV PATH $JAVA_HOME/bin:$PATH
COPY --from=build /myjre $JAVA_HOME
RUN mkdir /project
COPY --from=build /usr/src/project/deps.info /project/deps.info
COPY --from=build /usr/src/project/target/teste.jar /project/teste.jar
WORKDIR /project
ENTRYPOINT $JAVA_HOME/bin/java -Dspring.profiles.active=local -jar teste.jar