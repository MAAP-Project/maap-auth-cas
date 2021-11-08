export CONTAINER_NAME = maap-auth-cas
export IMAGE_NAME = maap-auth-cas
export RUN_OPTIONS = 

build-image:	## Build image
	docker build --force-rm -t $(IMAGE_NAME) .

build-image-verbose: ## Build Image and show verbose output
	docker build --force-rm -t $(IMAGE_NAME) --progress=plain .

rebuild-war: ## Builds war file and deploys it to Tomcat webapps folder
	docker exec $(CONTAINER_NAME) /tmp/maap-auth-cas/gradlew clean build
	docker exec $(CONTAINER_NAME) cp /tmp/maap-auth-cas/build/libs/cas.war /tomcat/tomcat-cas/webapps/
	docker exec $(CONTAINER_NAME) supervisorctl restart tomcat

list-containers: ## List containers related to this project
	docker container ls --all --filter "ancestor=$(IMAGE_NAME)"

list-images: ## List images related to this project
	docker images maap-auth-cas

login-container: ## Open terminal window using running container
	docker exec -it $(CONTAINER_NAME) /bin/bash

remove-images: remove-containers	## Remove all images related to this project. Also removes project's containers.
	docker image ls | awk '{print $$1}' | grep "${IMAGE_NAME}" | awk '{print $$1}' | xargs -I {} docker rmi -f {}

remove-containers:  ## Remove all containers related to this project.
	docker ps -a | awk '{ print $$1,$$2 }' | grep "${CONTAINER_NAME}" | awk '{print $$1}' | xargs -I {} docker rm -f {}

run-container: ## Run Container
	docker run --name $(CONTAINER_NAME) $(RUN_OPTIONS) -v "$(PWD)/src:/tmp/maap-auth-cas/src" -p 443:443 $(IMAGE_NAME)

run-container-background: RUN_OPTIONS = "-d" ## Run container in background (detached mode)
run-container-background: run-container

start-container: ## Start Container
	docker start $(CONTAINER_NAME)

stop-container: ## Stop Container
	docker stop $(CONTAINER_NAME)

watch-containers: ## Watch running containers
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