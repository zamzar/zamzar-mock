.PHONY: clean build test run publish

clean:
	@rm -rf target
	@docker stop zamzar-mock || true
	@docker rm zamzar-mock || true
	@docker rmi zamzar-mock || true

build:
    # --platform and --load can't be used together: https://github.com/docker/buildx/issues/59
	@docker buildx build --platform=linux/amd64,linux/arm64 --progress=plain -t zamzar-mock .
	@docker buildx build --load -t zamzar-mock .

test: build
	@docker stop zamzar-mock-test 2> /dev/null || true
	@docker rm zamzar-mock-test 2> /dev/null || true
	@docker run --detach --rm --name zamzar-mock-test -p 8081:8080 zamzar-mock > /dev/null
	@curl \
	    --retry 5 --retry-delay 1 --retry-all-errors \
	    --fail \
	    -4 \
	    http://localhost:8081/jobs/1 -H 'Authorization: Bearer GiVUYsF4A8ssq93FR48H'
	@docker stop zamzar-mock-test > /dev/null

run: build
	@docker run --rm --name zamzar-mock -p 8080:8080 zamzar-mock

publish:
	@tags=$$(git tag --points-at HEAD | sed 's/^v//'); \
	head=$$(git rev-parse --short HEAD); \
    if [ -z "$$tags" ]; then \
        echo "Skipping publish: there are no tags found pointing at $$head"; \
    fi; \
    for tag in $$tags; do \
        docker image tag zamzar-mock zamzar/zamzar-mock:$$tag && docker push zamzar/zamzar-mock:$$tag; \
    done; \
    docker image tag zamzar-mock zamzar/zamzar-mock:latest && docker push zamzar/zamzar-mock:latest