# Docker file build

FROM adoptopenjdk/openjdk14:ubi

# Set environment variables
ENV VERTICLE_HOME /work/application
ENV VERTICLE_FILE /target/core-1.9.1-fat.jar

# Install ca-certificates for SSL/TLS connections
RUN yum update -y && yum install -y ca-certificates && update-ca-trust force-enable

# Set environment variables
ENV FILE core-1.9.1-fat.jar

# Expose required ports
EXPOSE 8080
EXPOSE 8447
EXPOSE 7000

# Set working directory and copy the jar file
WORKDIR ${VERTICLE_HOME}
COPY ${VERTICLE_FILE} ${VERTICLE_HOME}

ADD CERT ${VERTICLE_HOME}/CERT


# Set entry point and default command
ENTRYPOINT [ "sh", "-c" ]
CMD ["exec java -Dhttps.protocols=TLSv1.2 -Djavax.net.debug=ssl -Djavax.net.ssl.trustStore=/path/to/keystore.jks -Djavax.net.ssl.trustStorePassword=changeit  -Xms512m -Xmx2g  -jar $FILE"]
