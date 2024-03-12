package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class PagedResponse {
    protected static final String DEFAULT_ID_FIELD = "id";
    protected final FileSource source;
    protected final List<String> paths;
    protected final PageCoordinates coordinates;

    protected final String idField;
    protected final ObjectMapper mapper = new ObjectMapper();

    public PagedResponse(FileSource source, List<String> paths, PageCoordinates coordinates) {
        this(source, paths, coordinates, DEFAULT_ID_FIELD);
    }

    public PagedResponse(FileSource source, List<String> paths, PageCoordinates coordinates, String idField) {
        this.source = source;
        this.paths = paths;
        this.coordinates = coordinates;
        this.idField = idField;
    }


    public JsonNode prepare() {
        // Load contents of each file, parse with GSON, and add to a collection
        final List<JsonNode> all = readJsonFiles();
        final int total = all.size();

        final List<JsonNode> pagedItems = coordinates.applyTo(all, idField);
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
        paging.put("limit", coordinates.limit);

        final ObjectNode response = mapper.createObjectNode();
        response.putArray("data").addAll(pagedItems);
        response.set("paging", paging);

        return response;
    }

    public String toPrettyString() {
        return prepare().toPrettyString();
    }

    protected List<JsonNode> readJsonFiles() {
        return paths.stream().map(this::readJsonFile).collect(Collectors.toList());
    }

    protected JsonNode readJsonFile(String path) {
        final BinaryFile file = source.getBinaryFileNamed(path);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(file.getStream()))) {
            return new ObjectMapper().readTree(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + path, e);
        }
    }

}
