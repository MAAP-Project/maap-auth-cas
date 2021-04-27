# Auth (CAS & Syncope) Installation

This repository serves as both the source for the MAAP [CAS overlay template](https://github.com/apereo/cas-overlay-template), and the configuration instructions for deploying the MAAP Single Sign-On service, a CAS-powered service acting as the central authentication provider for MAAP.  

The MAAP CAS Overlay Template provides add-on functionality to the baseline [CAS framework](https://www.apereo.org/projects/cas), including [Earthdata Login](https://urs.earthdata.nasa.gov) support for NASA users, and integration with [Syncope](https://syncope.apache.org/), an Identity Management library for storing MAAP user and organization data.

## Prerequisite
Start a new t3.xlarge instance with Ubuntu and configure it in the appropriate subnet. Use a 100GB primary volume.

## Installation

1. Update OS
```shell
sudo apt-get update
sudo apt-get dist-upgrade
```
2. Install JDK 
3. Install Apache 
   - Enable mods `proxy_ajp` and `proxy_http`
4. Install Tomcat 
   - Make copies of the default tomcat instance at `/opt/tomcat/apache-tomcat-*` to the home directory for three sites we'll be creating: `tomcat-cas`, `tomcat-syncope-core` and `tomcat-syncope-ui`.
5. Install CAS 
```
git clone https://github.com/MAAP-Project/maap-auth-cas.git
cd maap-auth-cas
gradle clean build
cp ./build/libs/cas.war ~/tomcat-cas/webapps/
```
6. Install Maven (required for Syncope)
7. Install PostgreSQL
8. Install Syncope 
```shell
cd ~
mkdir maap-syncope
cd maap-syncope
mvn archetype:generate \
    -DarchetypeGroupId=org.apache.syncope \
    -DarchetypeArtifactId=syncope-archetype \
    -DarchetypeRepository=https://central.maven.org/maven2 \
    -DarchetypeVersion=2.1.9
```
  - Follow the prompts to complete the installation
  - Create a new PostgreSQL database and user named 'syncope' to enable communication with the default Syncope configuration.
  - Find the postgresl jar (`find . -name postgresql-*.jar`) and copy it to the `tomcat-syncope-core\lib` directory. 
  - Update the SQL connection string in the syncope-core project, found in `~/maap-syncope/core/src/main/resources/domains/Master.properties`
  - Build and deploy Syncope: 
```
mvn clean install
cp ./core/target/syncope.war ~/tomcat-syncope-core/webapps/
cp ./console/target/syncope-console.war ~/tomcat-syncope-core/webapps/

```
9. OPTIONAL: Install the [Cas Management Overlay](https://github.com/apereo/cas-management-overlay)
10. Add a new `auth.<env>.maap-project.org` site in Apache
11. Configure the new site three proxy sites using ProxyPass to point to the three sites described in step 4 (TBD: example Apache conf file).

## Configuration

1. Fill in the `<add>` and `<env>` fields in the [CAS config file](etc/cas/config/cas.properties), and copy it to `/etc/cas/config/` directory.
2. Create a new folder, `/etc/cas/services-repo` to store the service definitions for each site that will use the MAAP Auth service. These include:
   - ESA ADE
   - ESA GitLab
   - ESA Liferay
   - NASA API
   - NASA ADE - [example config file](/etc/cas/services-repo/NasaAde-123.json) 
   - NASA GitLab
   - NASA Portal
3. Log into the Syncope console, and add the necessary settings to store MAAP user accounts 
  - Add the following schemas to the 'PLAIN' section:
    - display_name
    - email
    - family_name (mandatory)
    - gitlab_access_token (unique)
    - gitlab_uid (unique)
    - given_name (mandatory)
    - user_type
  - Add new user email notifications:
    - MAAP User Status Update
    - MAAP New User Access Request
    - Welcome to MAAP
4. Configure the Syncope email sender properties in the file `/opt/syncope/conf/mail.properties`



