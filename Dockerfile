FROM folioci/alpine-jre-openjdk17:latest

# Install latest patch versions of packages: https://pythonspeed.com/articles/security-updates-in-docker/
USER root
RUN apk upgrade --no-cache
USER folio

ENV VERTICLE_FILE mod-finc-config-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
ENV DB_USERNAME folio_admin
ENV DB_PASSWORD folio_admin
ENV DB_HOST 172.17.0.1
ENV DB_PORT 5432
ENV DB_DATABASE okapi_modules

# Copy your fat jar to the container
COPY target/${VERTICLE_FILE} ${VERTICLE_HOME}/${VERTICLE_FILE}

# Expose this port locally in the container.
EXPOSE 8081
