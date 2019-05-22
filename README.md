MAAP CAS Overlay Template & Server Configuration
=======================

This repository serves as both the source for the MAAP [CAS overlay template](https://github.com/apereo/cas-overlay-template), and the configuration instructions for deploying the MAAP Single Sign-On service, a CAS-powered service acting as the central authentication provider for MAAP.  

The MAAP CAS Overlay Template provides add-on functionality to the baseline [CAS framework](https://www.apereo.org/projects/cas), including [Earthdata Login](https://urs.earthdata.nasa.gov) support for NASA users, and integration with [Syncope](https://syncope.apache.org/), an Identity Management library for storing MAAP user and organization data.

# MAAP CAS Server Configuration

## Versions

- Ubuntu `18.04.2`
- JDK `11`
- Apache `2.4.29`
- Tomcat `9.0.19.0`
- CAS `6.0.4`
- Syncope `2.1.4`

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