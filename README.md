MAAP CAS Overlay Template & Server Configuration
=======================

This repository serves as both the source for the MAAP [CAS overlay template](https://github.com/apereo/cas-overlay-template), and the configuration instructions for deploying the MAAP Single Sign-On service, a CAS-powered service acting as the central authentication provider for MAAP.  

The MAAP CAS Overlay Template provides add-on functionality to the baseline [CAS framework](https://www.apereo.org/projects/cas), including [Earthdata Login](https://urs.earthdata.nasa.gov) support for NASA users, and integration with [Syncope](https://syncope.apache.org/), an Identity Management library for storing MAAP user and organization data.

# MAAP CAS Server Configuration

## Installation

Below are the installation steps used for setting up the MAAP SSO server. `Version` numbers are the latest available at the time of writing. Use the latest stable versions where possible.

1. Create a new EC2 instance running Ubuntu `18.04.2`
2. Install JDK `11`
3. Install Apache `2.4.29`
4. Install Tomcat `9.0.19.0`
    - Used for hosting CAS core, CAS admin, and several Syncope apps.
5. Install CAS `6.0.4`
    - CAS should be installed by cloning this repository, and following the compilation instructions listed below.
6. Install Maven `3.6.1`
    - Prequisite for Syncope.
7. Install Syncope `2.1.4`
    - Refer to the [Maven Project](https://syncope.apache.org/docs/getting-started.html#maven-project) method for installation. 
    - Once installed, install the MAAP [Gitlab Syncope](https://github.com/MAAP-Project/maap-auth-gitlab4syncope) extension for integration with the MAAP Gitlab server.
8. Install Postgresql `10`
    - During setup, create a new database and user named 'syncope' to enable communication with the default Syncope configuration.
9. Install the [Cas Management Overlay](https://github.com/apereo/cas-management-overlay)

## Compilation

You may invoke build commands using the `build.sh` script to work with your chosen overlay using:

```bash
./build.sh [command]
```

To see what commands are available to the build script, run:

```bash
./build.sh help
```

## Configuration

- The `etc` directory contains the configuration files and directories that need to be copied to `/etc/cas/config`.
- The specifics of the build are controlled using the `gradle.properties` file.

## Adding Modules

CAS modules may be specified under the `dependencies` block of the [Gradle build script](build.gradle):

```gradle
dependencies {
    compile "org.apereo.cas:cas-server-some-module:${project.casVersion}"
    ...
}
```

Study material:

- https://docs.gradle.org/current/userguide/artifact_dependencies_tutorial.html
- https://docs.gradle.org/current/userguide/dependency_management.html

## Clear Gradle Cache

If you need to, on Linux/Unix systems, you can delete all the existing artifacts (artifacts and metadata) Gradle has downloaded using:

```bash
# Only do this when absolutely necessary!
rm -rf $HOME/.gradle/caches/
```

Same strategy applies to Windows too, provided you switch `$HOME` to its equivalent in the above command.

# Deployment

- Create a keystore file `thekeystore` under `/etc/cas`. Use the password `changeit` for both the keystore and the key/certificate entries.
- Ensure the keystore is loaded up with keys and certificates of the server.

On a successful deployment via the following methods, CAS will be available at:

* `https://cas.server.name:8443/cas`

## Executable WAR

Run the CAS web application as an executable WAR.

```bash
./build.sh run
```

## External

Deploy the binary web application file `cas.war` after a successful build to a servlet container of choice.

# MAAP Developer Notes

The following bugs and breaking changes introduced in CAS v6 that necessitated overlay patches for MAAP:

1. *Incorrect casting of the Pac4j `generic` property*: this error, for which we submitted a PR to CAS (https://github.com/apereo/cas/pull/4046), broke the generic OIDC delegation process which, in turn, broke the ESA login function for our application. The overlay additions here may be removed once our merged PR is released in the next version of CAS: https://github.com/MAAP-Project/maap-auth-cas/commit/78e66607f397ddba7d34ac806a925562b9849853

2. *Breaking change to OAuth libraries*: prior to CAS v6, OAuth delegation honored the configured client name throughout the authentication to redirection steps during the login process, but this was evidently changed to substitute the client 'id' with the client name for the redirection step in v6, which led to an invalid redirect when logging in to URS. See: https://github.com/MAAP-Project/maap-auth-cas/commit/756b9c9e73edb0adaefd67d06b76b4fed7979215

3. *Change to configuration schema*: according to the official CAS documentation, the config structure with respect to `cas.authn.pac4j.oauth2` settings from v5.3 carried over to v6, though we discovered the schema had changed, which required the config changes noted here: https://github.com/apereo/cas/pull/3656#issuecomment-494702487
