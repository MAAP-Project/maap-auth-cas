export NAME_PREFIX = maap-auth
export APACHE_CONTAINER_NAME = $(NAME_PREFIX)-apache
export CAS_CONTAINER_NAME = $(NAME_PREFIX)-cas
export RUN_OPTIONS = 


build-images:	## Builds application for Docker Compose
	docker-compose build

build-images-nocache:	## Builds application for Docker Compose without the cache
	docker-compose build --no-cache

destroy:	## Stops running app locally and removes Docker container images requiring a rebuild
	docker-compose down --rmi all

list-containers: ## List containers related to this project
	docker container ls --all --filter "ancestor=$(APACHE_CONTAINER_NAME)" --filter "ancestor=$(CAS_CONTAINER_NAME)"

list-images: ## List images related to this project
	docker images --filter=reference='$(NAME_PREFIX)*'

login-apache: ## Open terminal window using apache container
	docker exec -it $(APACHE_CONTAINER_NAME) /bin/bash

login-cas: ## Open terminal window using cas container
	docker exec -it $(CAS_CONTAINER_NAME) /bin/bash

open: ## open default browser to login selection interface
	open https://localhost/cas/login

rebuild-war: ## Builds war file and deploys it to Tomcat webapps folder
	docker exec $(CAS_CONTAINER_NAME) /tmp/maap-auth-cas/gradlew clean build
	docker exec $(CAS_CONTAINER_NAME) cp /tmp/maap-auth-cas/build/libs/cas.war /opt/tomcat/webapps/
	docker exec $(CAS_CONTAINER_NAME) supervisorctl restart cas

save-war-cas:	##Builds war file and saves it to local filesystem
	docker exec $(CAS_CONTAINER_NAME) /tmp/maap-auth-cas/gradlew clean build
	mkdir -p build
	docker cp $(CAS_CONTAINER_NAME):/tmp/maap-auth-cas/build/libs/cas.war ./build/

remove-containers:  ## Remove all containers related to this project.
	docker container ls --all | awk '{print $$2}' | grep "$(NAME_PREFIX)" | xargs -I {} docker rm -f {}

remove-images: remove-containers	## Remove all images related to this project. This depends on also removing project's containers; otherwise this target will fail if containers reference any images.
	docker images --all | awk '{print $$1}' | grep "${NAME_PREFIX}" | xargs -I {} docker rmi -f {}

restart:	## Restarts the application locally, does not reload environment variables
	docker-compose restart

restart-apache:	## Restarts the Apache service
	docker-compose restart apache

restart-cas:	## Restarts the CAS service
	docker-compose restart cas

restart-cas-tomcat: ## Restarts the Tomcat instance running in the CAS container
	docker exec $(CAS_CONTAINER_NAME) supervisorctl restart cas

start:	## Starts up the application using Docker Compose
	docker-compose up $(RUN_OPTIONS)

start-detached:	RUN_OPTIONS = "-d" ## Starts up the application with Docker Compose in detached mode
start-detached: start

stop:	## Stops all running services
	docker-compose stop

stop-apache:	## Stops running Apache service
	docker-compose stop apache

stop-cas:	## Stops running CAS service
	docker-compose stop cas

watch-containers: ## Display a list of running containers that refreshes periodically
	watch docker container ls


# ----------------------------------------------------------------------------
# Self-Documented Makefile
# ref: http://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
# ----------------------------------------------------------------------------
help:						## (DEFAULT) This help information
	@echo ====================================================================
	@grep -E '^## .*$$'  \
		$(MAKEFILE_LIST)  \
		| awk 'BEGIN { FS="## " }; {printf "\033[33m%-20s\033[0m \n", $$2}'
	@echo
	@grep -E '^[0-9a-zA-Z_-]+:.*?## .*$$'  \
		$(MAKEFILE_LIST)  \
		| awk 'BEGIN { FS=":.*?## " }; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'  \
#		 | sort
.PHONY: help
.DEFAULT_GOAL := help
