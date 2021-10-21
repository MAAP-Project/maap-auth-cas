export CONTAINER_NAME = maap-auth-cas
export IMAGE_NAME = maap-auth-cas

remove-image:	## Remove image
	docker image rm $(IMAGE_NAME)

remove-container:						## Remove container
	docker container rm $(CONTAINER_NAME)

build-image:	## Build image
	docker build --force-rm -t $(IMAGE_NAME) .

build-image-verbose: ## Build Image and show verbose output
	docker build --force-rm -t $(IMAGE_NAME) --progress=plain .

run-container: ## Run Container
	docker run --name $(CONTAINER_NAME) -p 80:8001 $(IMAGE_NAME)

start-container: ## Start Container
	docker start $(CONTAINER_NAME)

stop-container: ## Stop Container
	docker stop $(CONTAINER_NAME)

login-container: ## Login and open bash window for container
	docker exec -it $(CONTAINER_NAME) /bin/bash

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
