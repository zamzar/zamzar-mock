package com.zamzar.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.zamzar.mock.examples.ExamplesRepository;
import com.zamzar.mock.pagination.Anchor;
import com.zamzar.mock.pagination.PageCoordinates;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IndexTransformer implements ResponseTransformerV2 {
    public static final String NAME = "index-transformer";

    public static final String EXAMPLES_REPOSITORY_PARAMETER = "repo";
    public static final String RESOURCE_PARAMETER = "resource";
    public static final String ID_FIELD_NAME_PARAMETER = "idFieldName";
    public static final String ASCENDING_PARAMETER = "ascending";

    public static final String FILENAME_SUFFIX_PARAMETER = "filenameSuffix";

    public static final String PREDICATE_PARAMETER = "predicate";

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        final Parameters parameters = serveEvent.getTransformerParameters();
        final Request request = serveEvent.getRequest();

        final ExamplesRepository repository = getExamplesRepository(parameters);
        final String resource = getResource(parameters);
        final PageCoordinates coordinates = getCoordinates(request);
        final String idFieldName = getIdFieldName(parameters);
        final boolean isAscending = isAscending(parameters);
        final String filenameSuffix = getFilenameSuffix(parameters);
        final Predicate<JsonNode> predicate = getPredicate(parameters);

        try {
            final List<JsonNode> all = readAll(repository, resource, isAscending, filenameSuffix, predicate);
            final String responseBody = buildResponseBody(all, coordinates, idFieldName);

            return Response.Builder.like(response).but()
                .body(responseBody)
                .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not generate page for resource: " + resource, e);
        }
    }

    protected List<JsonNode> readAll(
        ExamplesRepository repo,
        String resource,
        boolean isAscending,
        String filenameSuffix,
        Predicate<JsonNode> filter
    ) throws JsonProcessingException {
        final Collection<String> allFilenames = repo.all(resource, true)
            .stream()
            .sorted(isAscending ? Comparator.naturalOrder() : Comparator.reverseOrder())
            .collect(Collectors.toList());

        final Set<String> seen = new HashSet<>();
        final List<JsonNode> all = new ArrayList<>();
        for (String filename : allFilenames) {
            // if the filename contains a dot, it has a "lifecycle" => parse the file with specified suffix
            // otherwise => parse the file as is
            final String id = filename.contains(".") ? filename.split("\\.")[0] : filename;
            final String relevantFilename = filename.contains(".") ? filename.split("\\.")[0] + filenameSuffix : filename;

            if (seen.add(id)) { // only add the first occurrence of an id
                final JsonNode parsed = repo.parse(resource, relevantFilename);
                if (filter.test(parsed)) {
                    all.add(parsed);
                }
            }
        }
        return all;
    }

    protected String buildResponseBody(List<JsonNode> all, PageCoordinates coordinates, String idField) {
        // Apply coordinates to obtain a page
        final List<JsonNode> pagedItems = coordinates.applyTo(all, idField);

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode response = mapper.createObjectNode();
        response.putArray("data").addAll(pagedItems);
        response.set("paging", buildPaging(pagedItems, all.size(), coordinates.getLimit(), idField));
        return response.toPrettyString();
    }

    protected JsonNode buildPaging(List<JsonNode> pagedItems, int total, int limit, String idField) {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode first = pagedItems.stream().findFirst().map(n -> n.get(idField)).orElse(null);
        final JsonNode last = pagedItems.stream().reduce((f, s) -> s).map(n -> n.get(idField)).orElse(null);
        final ObjectNode paging = mapper.createObjectNode();
        paging.put("total_count", total);
        if (first != null) {
            paging.set("first", first);
        }
        if (last != null) {
            paging.set("last", last);
        }
        paging.put("limit", limit);
        return paging;
    }

    protected ExamplesRepository getExamplesRepository(Parameters parameters) {
        return (ExamplesRepository) parameters.get(EXAMPLES_REPOSITORY_PARAMETER);
    }

    protected String getResource(Parameters parameters) {
        return (String) parameters.get(RESOURCE_PARAMETER);
    }

    protected String getIdFieldName(Parameters parameters) {
        return (String) parameters.get(ID_FIELD_NAME_PARAMETER);
    }

    protected boolean isAscending(Parameters parameters) {
        return (boolean) parameters.get(ASCENDING_PARAMETER);
    }

    protected String getFilenameSuffix(Parameters parameters) {
        return (String) parameters.getOrDefault(FILENAME_SUFFIX_PARAMETER, ".initialising");
    }

    @SuppressWarnings("unchecked")
    protected Predicate<JsonNode> getPredicate(Parameters parameters) {
        return (Predicate<JsonNode>) parameters.getOrDefault(PREDICATE_PARAMETER, (Predicate<JsonNode>) (n) -> true);
    }

    protected PageCoordinates getCoordinates(Request request) {
        final int limit = request.queryParameter("limit").isPresent() ?
            Integer.parseInt(request.queryParameter("limit").values().get(0)) :
            PageCoordinates.DEFAULT_LIMIT;

        Anchor anchor;
        if (request.queryParameter("before").isPresent()) {
            anchor = Anchor.before(request.queryParameter("before").values().get(0));
        } else if (request.queryParameter("after").isPresent()) {
            anchor = Anchor.after(request.queryParameter("after").values().get(0));
        } else {
            anchor = PageCoordinates.DEFAULT_ANCHOR;
        }

        return new PageCoordinates(anchor, limit);
    }
}
