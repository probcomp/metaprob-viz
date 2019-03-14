docker-build:
	docker build -t probcomp/metaprob-viz:latest .
.PHONY: docker-build

docker-bash:
	docker run \
		-it \
		probcomp/metaprob-viz:latest \
		bash
	.PHONY: docker-bash


docker-notebook:
	docker run \
		-it \
		--mount type=bind,source=${HOME}/.m2,destination=/home/metaprob/.m2 \
		--mount type=bind,source=${CURDIR},destination=/home/metaprob/projects/metaprob-viz \
		--publish 8888:8888/tcp \
		--publish 8081:8081/tcp \
		probcomp/metaprob-viz:latest \
	bash -c "lein jupyter notebook \
		--ip=0.0.0.0 \
		--port=8888 \
		--no-browser \
		--NotebookApp.token= \
		--notebook-dir ./tutorial"
.PHONY: docker-notebook
