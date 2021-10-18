FROM ubuntu:18.04
LABEL maintainer="anil.natha@jpl.nasa.gov"
LABEL version="0.0.1"

RUN apt-get update && apt-get install -y default-jre apache2 && rm -rf /var/lib/apt/lists/*

# Enable proxy_ajp and proxy_http
RUN a2enmod proxy_ajp
RUN a2enmod proxy_http

