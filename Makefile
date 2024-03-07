.PHONY: clean build test run publish

clean:
	@rm -rf target
	@docker stop zamzar-mock || true
	@docker rm zamzar-mock || true
	@docker rmi zamzar-mock || true

build:
	@docker build -t zamzar-mock .

test: build
    @true

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