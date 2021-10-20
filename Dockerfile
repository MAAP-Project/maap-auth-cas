FROM ubuntu:18.04
LABEL maintainer="anil.natha@jpl.nasa.gov"
LABEL version="0.0.1"

RUN apt-get update && apt-get install -y default-jdk apache2 curl && apt-get clean

# Enable proxy_ajp and proxy_http
RUN a2enmod proxy_ajp
RUN a2enmod proxy_http

# Install tomcat
RUN groupadd tomcat
RUN useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat

WORKDIR /tmp
RUN curl -O https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.54/bin/apache-tomcat-9.0.54.tar.gz
ENV SHA=83430f24d42186ce2ff51eeef2f7a5517048f37d9050c45cac1e3dba8926d61a1f7f5aba122a34a11ac1dbdd3c1f6d98671841047df139394d43751263de57c3
RUN echo "${SHA} /tmp/apache-tomcat-9.0.54.tar.gz" | sha512sum -c -
RUN mkdir /opt/tomcat
RUN tar -xzvf apache-tomcat-9.0.54.tar.gz -C /opt/tomcat --strip-components=1

WORKDIR /opt/tomcat
RUN chgrp -R tomcat /opt/tomcat
RUN chmod -R g+r conf
RUN chmod g+x conf
RUN chown -R tomcat webapps/ work temp/ logs

EXPOSE 8080

CMD ["/opt/tomcat/bin/catalina.sh", "run"]