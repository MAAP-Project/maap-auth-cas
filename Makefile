export CONTAINER_NAME = maap-auth-cas
export IMAGE_NAME = maap-auth-cas
export RUN_OPTIONS = 

build-image:	## Build image
	docker build --force-rm -t $(IMAGE_NAME) .

build-image-verbose: ## Build Image and show verbose output
	docker build --force-rm -t $(IMAGE_NAME) --progress=plain .

login-container: ## Open terminal window using running container
	docker exec -it $(CONTAINER_NAME) /bin/bash

remove-images: remove-containers	## Remove all images related to this project. Also removes project's containers.
	docker image ls | awk '{print $$1}' | grep "${IMAGE_NAME}" | awk '{print $$1}' | xargs -I {} docker rmi -f {}

remove-containers:  ## Remove all containers related to this project.
	docker ps -a | awk '{ print $$1,$$2 }' | grep "${CONTAINER_NAME}" | awk '{print $$1}' | xargs -I {} docker rm -f {}

run-container: ## Run Container
	docker run --name $(CONTAINER_NAME) $(RUN_OPTIONS) -v "src":"/tmp/maap-auth-cas/src" -p 443:443 $(IMAGE_NAME)

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
