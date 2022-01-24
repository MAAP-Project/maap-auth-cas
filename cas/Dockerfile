FROM ubuntu:18.04
LABEL maintainer="anil.natha@jpl.nasa.gov"
LABEL version="0.0.1"

RUN apt-get update && apt-get install -y default-jdk supervisor curl vim && rm -rf /var/lib/apt/lists/* && apt-get clean

###########################################################################
###########################################################################
# Apache Tomcat installation and configuration

RUN groupadd tomcat
RUN useradd -s /bin/false -g tomcat -d /home/tomcat tomcat

WORKDIR /tmp
RUN curl -O https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.54/bin/apache-tomcat-9.0.54.tar.gz
ENV SHA=83430f24d42186ce2ff51eeef2f7a5517048f37d9050c45cac1e3dba8926d61a1f7f5aba122a34a11ac1dbdd3c1f6d98671841047df139394d43751263de57c3
RUN echo "${SHA} /tmp/apache-tomcat-9.0.54.tar.gz" | sha512sum -c -

RUN mkdir /opt/tomcat
RUN tar -xzvf apache-tomcat-9.0.54.tar.gz -C /opt/tomcat --strip-components=1

WORKDIR /opt/tomcat
COPY ./opt/tomcat/conf/tomcat-cas-server.xml /opt/tomcat/conf/server.xml
RUN chgrp -R tomcat /opt/tomcat
RUN chmod -R g+r conf
RUN chmod -R o-r conf
RUN chmod g+x conf
RUN chown -R tomcat webapps/ work temp/ logs

###########################################################################
###########################################################################
# Set up cas instance

WORKDIR /tmp/maap-auth-cas
COPY . ./

RUN mkdir -p ~/.gradle \
    && echo "org.gradle.daemon=false" >> ~/.gradle/gradle.properties \
    && echo "org.gradle.configureondemand=true" >> ~/.gradle/gradle.properties \
    && chmod 750 ./gradlew \
    && ./gradlew --version;

RUN ./gradlew clean build --parallel --no-daemon
RUN ./gradlew createKeystore
RUN cp ./build/libs/cas.war /opt/tomcat/webapps/
RUN chown tomcat:tomcat /opt/tomcat/webapps/cas.war
RUN chmod 750 /opt/tomcat/webapps/cas.war

###########################################################################
###########################################################################
# Set up CAS configuration settings

WORKDIR /etc/cas/config
COPY ./etc/cas/config/* /etc/cas/config/
RUN mkdir /etc/cas/services-repo
WORKDIR /etc/cas/services-repo
COPY ./etc/cas/services-repo/* /etc/cas/services-repo/
WORKDIR /etc/cas
COPY ./etc/cas/*.jwks /etc/cas/
COPY ./etc/cas/*.key /etc/cas/
COPY ./etc/cas/thekeystore /etc/cas/

###########################################################################
###########################################################################
# Set up supervisord

COPY ./etc/supervisor/supervisord.conf /etc/supervisor/
COPY ./etc/supervisor/conf.d/supervisor_programs.conf /etc/supervisor/conf.d/

###########################################################################
###########################################################################
# Default command to run container

### Setting the working directory is needed to be able to rebuild war files using docker exec
WORKDIR /tmp/maap-auth-cas

#CMD ["/opt/tomcat/bin/catalina.sh", "run"]
CMD ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]