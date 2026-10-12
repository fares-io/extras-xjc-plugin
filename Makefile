export SHELL  := /bin/bash

.DEFAULT_GOAL := usage

BOLD   := \033[1m
BRED   := \033[1;31m
RED    := \033[0;31m
GREEN  := \033[0;32m
CYAN   := \033[0;36m
YELLOW := \033[0;33m
NC     := \033[0m

usage:
	@printf "\n$(BOLD)Usage:$(NC)\n\n"
	@printf "  make clean             $(GREEN)# Clean build directory of this project $(NC)\n"
	@printf "  make test              $(GREEN)# Unit test the local project $(NC)\n"
	@printf "  make verify            $(GREEN)# Test and verify the local build $(NC)\n"
	@printf "  make install           $(GREEN)# Test and install the local project $(NC)\n"
	@printf "  make prepare           $(GREEN)# Tag a release and push the tag $(NC)\n"
	@printf "  make patch             $(GREEN)# Bump patch version and push upstream $(NC)\n"
	@printf "  make snapshot          $(GREEN)# Build and upload a snapshot to Central$(NC)\n\n"
	@printf "  make release           $(GREEN)# Build and upload a release to Central for validation$(NC)\n\n"
	@exit 0

# ---- defaults and macros ---------------------------------------------------------------------------------------------

# suppress any download for dependencies and plugins or upload messages which would clutter the console log
export MAVEN_OPTS ?= -Djava.awt.headless=true

# use this option to vary the maven runtime behaviour (e.g. log or memory settings in CI pipelines)
export MAVEN_CLI_OPTS ?=

# set the branch to patch after release tagging (standard release process, do not change)
BRANCH ?= develop

# the version macro is used for tagging the builds in git
# BEWARE: this macro expands at the start when the make file is run, if you do something
#         to the version file, this variable will be set to the old version !!!
VERSION = $(shell cat .bumpversion.cfg | grep current_version | awk '{ print $$3 }')

# the current time we can use to create unique revisions
TIMESTAMP ?= $(shell date '+%Y%m%d%H%M%S')

# ---- maven build and test --------------------------------------------------------------------------------------------

.PHONY: clean
clean:
	@mvn $(MAVEN_CLI_OPTS) clean

.PHONY: test
test:
	@mvn $(MAVEN_CLI_OPTS) --update-snapshots clean test

.PHONY: verify
verify:
	@mvn $(MAVEN_CLI_OPTS) --update-snapshots clean verify -Dgpg.skip=true

.PHONY: install
install:
	@mvn $(MAVEN_CLI_OPTS) --update-snapshots clean install

.PHONY: deps
deps:
	@mvn $(MAVEN_CLI_OPTS) --update-snapshots dependency:sources dependency:resolve -Dclassifier=javadoc

# prepare a release by tagging the merge request on master then push the tag to the repository
.PHONY: prepare
prepare:
	$(info tag release v$(VERSION))
	@git tag -s -a -m 'release final v$(VERSION)' v$(VERSION)
	git push --verbose -u origin v$(VERSION)

# bump a patch version number and push it into a specified branch (default develop)
.PHONY: patch
patch:
	$(info Patching local branch $(shell git symbolic-ref --short HEAD) for version $(VERSION))
	@bumpversion patch
	@git push --verbose -u origin '$(BRANCH)'

# releases the snapshot revision into the artifact repository
.PHONY: snapshot
snapshot:
	$(info release v$(VERSION)-SNAPSHOT)
	@mvn -Drevision=$(VERSION)-SNAPSHOT $(MAVEN_CLI_OPTS) clean deploy -Prelease

# releases the revision into the artifact repository
.PHONY: release
release:
	$(info release v$(VERSION))
	@mvn -Drevision=$(VERSION) $(MAVEN_CLI_OPTS) clean deploy -Prelease

