package com.zamzar.mock.examples;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.TextFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Wiremock logs all transformers, including their parameters.
// Rather than make this class serializable by Jackson, we use @JsonIgnoreType to skip serialization altogether.
@JsonIgnoreType
public class ExamplesRepository {
    protected final FileSource fileSource;

    protected final ObjectMapper mapper = new ObjectMapper();

    public ExamplesRepository(FileSource fileSource) {
        this.fileSource = fileSource.child("__files");
    }

    public Collection<String> all(String resource) {
        return all(resource, false);
    }

    public Collection<String> all(String resource, boolean includeState) {
        final Set<String> all = fileSource
            .child(resource)
            .listFilesRecursively()
            .stream()
            .filter(f -> f.getPath().endsWith(".json"))
            .map(file -> extractBaseName(file, includeState))
            .collect(Collectors.toSet());

        // Special case for the files resource: remove the special large file
        // This should not appear in indexes nor should it be stubbed with a file on disk
        if ("files".equals(resource)) {
            all.remove("0");
        }

        return all;
    }

    public boolean exists(String resource, String id) {
        return all(resource).contains(id);
    }

    public String read(String resource, String id) {
        return fileSource.child(resource).getTextFileNamed(id + ".json").readContentsAsString();
    }

    public JsonNode parse(String resource, String id) throws JsonProcessingException {
        return mapper.readTree(read(resource, id));
    }

    public void delete(String resource, String id) {
        final List<String> templateFilenames = all(resource, true)
            .stream()
            .filter(s -> s.startsWith(id))
            .collect(Collectors.toList());

        for (String templateFilename : templateFilenames) {
            fileSource.child(resource).deleteFile(templateFilename + ".json");
        }
    }

    protected static String extractBaseName(TextFile file, boolean includeState) {
        return extractBaseName(Paths.get(file.getPath()), includeState);
    }

    protected static String extractBaseName(Path path, boolean includeState) {
        String fileName = path.getFileName().toString();

        int dotIndex = includeState ? fileName.lastIndexOf(".") : fileName.indexOf(".");
        if (dotIndex > 0) { // Ensure that there is an extension to remove
            fileName = fileName.substring(0, dotIndex);
        }

        return fileName;
    }
}
