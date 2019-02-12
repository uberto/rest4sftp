# rest4sftp

Bridge from RESTful api to SFTP and FTP.


Beautifully written in Kotlin by Asad, Tom and Uberto

# How it works

rest4sftp translate RESTful HTTP calls into FTP or SFTP calls.

You can decide the protocol and other options with cmd line args or env variables.

List of options **TODO**

Then each request need a header with ftpserver data.

Headers example **TODO**


Example of a session **TODO**


Full Api are **TODO**

# Instructions using Docker Image

the easiest way is to use the docker image which is configured in `Dockerfile` to start a server on local port 8080. You
can start the image on 127.0.0.1:9990 (or another port of your choice) by one of the following methods:

## Docker run from source:
```$bash
docker build -t uberto/rest4sftp .
docker run -p 127.0.0.1:9990:8080/tcp uberto/rest4sftp --sftp
```

## Docker run from latest docker hub image:
```$bash
docker pull uberto/rest4sftp
docker run -p 127.0.0.1:9990:8080/tcp uberto/rest4sftp --sftp
```

## Docker-compose run instruction:
1. create a docker-compose.yml as follows:
```$yml
version: 3

services:
  rest4sftp:
    image: "uberto/rest4sftp"
    port: 9990:8080
    command: --sftp
```
2. run the following to start the docker image:
```$bash
cd <to directory where docker-compose.yml was created>
docker compose up
```

# Using from source

You can import it from maven in your project like this:

maven import **TODO**

There is a main fun in the class **TODO**


# Code design

The main classes are: **TODO**

