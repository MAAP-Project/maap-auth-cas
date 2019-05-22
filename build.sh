#!/bin/bash

function copy() {
	./gradlew copyCasConfiguration "$@"
}

function help() {
	casVersion=$(./gradlew casVersion --quiet)
	clear
	echo "******************************************************************"
	tput setaf 2
	echo "Apereo CAS $casVersion"
	echo "Enterprise Single SignOn for all earthlings and beyond"
	tput sgr 0
	echo "- https://github.com/apereo/cas"
	echo "- https://apereo.github.io/cas"
	echo "******************************************************************"

	echo -e "Usage: build.sh [command]\n"
	echo -e "\tThe following commands are available:\n"
	echo -e "\tclean: \t\tClean Maven build directory"
	echo -e "\tcli: \t\tRun the CAS command line shell and pass commands"
	echo -e "\tcopy: \t\tCopy config from the project's local etc/cas/config directory to the root /etc/cas/config"
	echo -e "\tdebug: \t\tRun cas.war and listen for Java debugger on port 5000"
	echo -e "\tdependencies: \tGet a report of all dependencies configured in the build"
	echo -e "\tdocker: \tBuild a Docker image based on the current build and configuration"
	echo -e "\tgencert: \tCreate keystore with SSL certificate in location where CAS looks by default"
	echo -e "\tgetview: \tAsk for a view name to be included in the overlay for customizations"
	echo -e "\tgetresource: \tAsk for a resource name (properties/json/etc file) to be included in the overlay for customizations"
	echo -e "\tlistviews: \tList all CAS views that ship with the web application and can be customized in the overlay"
	echo -e "\tpackage: \tClean and build CAS war"
	echo -e "\texplode: \tExplode and unzip and packaged CAS war"
	echo -e "\trun: \t\tBuild and run cas.war via Java as an executable war"
	echo -e "\trunalone: \tBuild and run cas.war on its own as a standalone executable"
	echo -e "\ttomcat: \tDeploy the CAS web application to an external Apache Tomcat server"
	echo -e "\tupdate: \tPackage the CAS overlay by force-updating dependencies and SNAPSHOT versions"
}

function clean() {
	./gradlew clean "$@"
}

function package() {
	./gradlew clean build "$@"
}

function update() {
	./gradlew clean build --refresh-dependencies "$@"
}

function dependencies() {
	./gradlew allDependencies
}

function tomcat() {
	./gradlew tomcatDeploy "$@"
}

function debug() {
	./gradlew debug "$@"
}

function run() {
	./gradlew run "$@"
}

function runalone() {
	./gradlew clean executable
}

function jibdocker() {
   ./gradlew clean build jibDockerBuild "$@"
}

function listviews() {
	./gradlew listTemplateViews "$@"
}

function explodeApp() {
	./gradlew explodeWar
}

function getresource() {
	./gradlew getResource -PresourceName="$@"
}

function getview() {
	./gradlew getResource -PresourceName="$@"
}

function gencert() {
	./gradlew createKeystore "$@"
}

function cli() {
	./gradlew downloadShell runShell "$@"
}

command=$1

if [ -z "$command" ]; then
    echo "No commands provided. Defaulting to [run]"
	command="run"
fi

shift 1

case "$command" in
"copy")
    copy
    ;;
"help")
    help
    ;;
"clean")
	clean "$@"
	;;
"package"|"build")
	package "$@"
	;;
"debug")
	debug "$@"
	;;
"run")
	run "$@"
	;;
"explode")
	explodeApp "$@"
	;;
"docker")
	jibdocker "$@"
	;;
"gencert")
	gencert "$@"
	;;
"cli")
	cli "$@"
	;;
"update")
	update "$@"
	;;
"dependencies")
	update "$@"
	;;
"runalone")
	runalone "$@"
	;;
"listviews")
	listviews "$@"
	;;
"getview")
	getview "$@"
	;;
"getresource")
	getresource "$@"
	;;
"tomcat")
	tomcat
	;;
esac
