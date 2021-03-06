#Based on http://paulbakker.io/java/docker-gradle-multistage/
FROM java:8 as builder
COPY . /appsrc
WORKDIR /appsrc
RUN ./gradlew --no-daemon clean build

FROM openjdk:8-jre-slim
EXPOSE 8080
COPY --from=builder /appsrc/build/distributions/rest4sftp-0.8.0-SNAPSHOT.tar /app/
WORKDIR /app
RUN tar -xvf /app/rest4sftp-0.8.0-SNAPSHOT.tar
ENTRYPOINT ["/app/rest4sftp-0.8.0-SNAPSHOT/bin/rest4sftp","--port","8080"]
CMD ["--sftp"]