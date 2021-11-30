# MAAP CAS Overlay Template & Server Configuration

This repository serves as both the source for the MAAP [CAS overlay template](https://github.com/apereo/cas-overlay-template), and the configuration instructions for deploying the MAAP Single Sign-On service, a CAS-powered service acting as the central authentication provider for MAAP.  

The MAAP CAS Overlay Template provides add-on functionality to the baseline [CAS framework](https://www.apereo.org/projects/cas), including [Earthdata Login](https://urs.earthdata.nasa.gov) support for NASA users, and integration with [Syncope](https://syncope.apache.org/), an Identity Management library for storing MAAP user and organization data.


## AWS Deployment 

Below are the installation steps for setting up the MAAP SSO server. Version numbers are the latest available at the time of writing. Use the latest stable versions where possible.

1. Start a new t3.xlarge instance with Ubuntu and configure it in the appropriate subnet. Use a 100GB primary volume. 
2. Install JDK `11`
3. Install Apache `2.4.29`
   - Enable mods `proxy_ajp` and `proxy_http`
4. Install Tomcat 
   - Make copies of the default tomcat instance at `/opt/tomcat/apache-tomcat-*` to the home directory for three sites we'll be creating: `tomcat-cas`, `tomcat-syncope-core` and `tomcat-syncope-ui`.
5. Install CAS 
```
git clone https://github.com/MAAP-Project/maap-auth-cas.git
cd maap-auth-cas
./gradlew clean build
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
cp ./console/target/syncope-console.war ~/tomcat-syncope-ui/webapps/

```
9. OPTIONAL: Install the [Cas Management Overlay](https://github.com/apereo/cas-management-overlay)
10. Add a new `auth.<env>.maap-project.org` site in Apache
11. Configure the new site three proxy sites using ProxyPass to point to the three sites described in step 4 (TBD: example Apache conf file).

## Local Development with Docker

Below are the installation steps used for setting up a local instance of the MAAP SSO server using Docker. _Currently, the Docker version does not include PostgreSQL or Syncope components._ 

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
2. Clone this repo to your development machine

#### Running the Application

Open a terminal window and run the following commands:

1. Use the "MAAP Server Configuration" information, see below, to set up CAS configuratino information. Without this, you will not be able to run the application locally.
2. Build the Docker images
```
make build
```
3. Start the application

```
make start
```

If you want to run the services in the background, run `make start-detached` instead.

4. View the application

```
make open
```

Or open your preferred browser and visit http://localhost/cas/login

#### Making Changes

As mentioned above, _currently the Docker version does not include PostgreSQL or Syncope components._ So any changes made to those components will not be viewable with this Docker configuration.

1. Make the necessary changes to the codebase.

#### CAS changes

If the changes were made to the CAS service, rebuild and redploy the WAR. To do this, the application must be running (see above). Open a terminal window and run the command:

```
make rebuild-war
```

After rebuilding is complete and the CAS container restarts Tomcat, refresh your browser and you will see your changes. _Note: It can take about 10-15 seconds to refresh the browser after restarting Tomcat, this is normal_.

#### Apache changes

If the changes were made to the Apache service:

```
make stop # stop the Docker containers
make remove-containers # remove the old containers
make build # rebuild the images
make start # start the app
````

---

## MAAP Server Configuration

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



