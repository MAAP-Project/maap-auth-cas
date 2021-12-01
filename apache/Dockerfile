FROM ubuntu:18.04
LABEL maintainer="anil.natha@jpl.nasa.gov"
LABEL version="0.0.1"

RUN apt-get update && apt-get install -y apache2 && rm -rf /var/lib/apt/lists/* && apt-get clean

###########################################################################
# Copy certificates

COPY ./etc/ssl/certs/* /etc/ssl/certs
COPY ./etc/ssl/private/* /etc/ssl/private

###########################################################################
# Apache configuration

RUN a2enmod proxy_ajp
RUN a2enmod proxy_http
RUN a2enmod ssl

COPY ./etc/apache2/sites-available/001-tomcat-cas.conf /etc/apache2/sites-available/
RUN a2ensite 001-tomcat-cas

###########################################################################
# Default command to run container

CMD ["/usr/sbin/apachectl", "-D", "FOREGROUND", "-k", "start"]