package com.zamzar.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.zamzar.mock.examples.ExamplesRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class ConfigureWireMock {

    protected static final String PATH_TO_EXAMPLES = "src/main/resources";

    protected static final String API_KEY = "GiVUYsF4A8ssq93FR48H";

    protected static final String BASE_PATH = "/v1";

    protected final WireMockServer wiremock;

    @Deprecated
    protected final FileSource fileSource;

    protected final ExamplesRepository examples;


    public static void main(String[] args) throws IOException {
        printBanner();

        final FileSource fileSource = new SingleRootFileSource(PATH_TO_EXAMPLES);
        final WireMockServer wireMockServer = startWireMock(fileSource);
        new ConfigureWireMock(wireMockServer, fileSource).run();

        // Keep the application running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
        }
    }

    protected static void printBanner() {
        try (InputStream inputStream = ConfigureWireMock.class.getResourceAsStream("/banner.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException | NullPointerException e) {
        }

        System.out.println();
    }

    protected static WireMockServer startWireMock(FileSource fileSource) {
        final WireMockConfiguration config = options()
            .fileSource(fileSource)
            .extensions(new IndexTransformer());

        final WireMockServer wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        System.out.println("zamzar-mock is running at: " + wireMockServer.baseUrl());
        System.out.println();

        return wireMockServer;
    }

    public ConfigureWireMock(WireMockServer wiremock, FileSource fileSource) {
        this.wiremock = wiremock;
        this.fileSource = fileSource;
        this.examples = new ExamplesRepository(fileSource);
    }

    public void run() {
        stubAccount();
        stubFiles();
        stubFormats();
        stubImports();
        stubJobs();
        stubCatchAlls();
    }

    protected void stubAccount() {
        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/account"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("account.json")));
    }

    protected void stubFiles() {
        final List<Integer> fileIds = examples
            .all("files")
            .stream()
            .map(Integer::parseInt)
            .sorted((a, b) -> b - a) // files are returned in descending ID order from list endpoint
            .collect(Collectors.toList());

        fileIds.forEach(this::stubFile);
        stubPaginatedList("files", "id", false);
        stubFileUpload();
    }

    protected void stubFile(int id) {
        final String scenarioName = "FileDeletion" + id;

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/files/" + id + "/content"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/octet-stream")
                .withBodyFile("files/content/" + id)));

        wiremock.stubFor(delete(urlPathEqualTo(BASE_PATH + "/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/files/" + id + "/content"))
            .inScenario(scenarioName)
            .whenScenarioStateIs("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));
    }

    protected void stubFormats() {
        final List<String> formats =
            examples.all("formats")
                .stream()
                .sorted() // formats are returned in ascending name order from list endpoint
                .collect(Collectors.toList());

        formats.forEach(this::stubFormat);
        stubPaginatedList("formats", "name", true);
    }

    protected void stubFormat(String name) {
        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/formats/" + name))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("formats/" + name + ".json")));
    }

    protected void stubFileUpload() {
        wiremock.stubFor(post(urlPathEqualTo(BASE_PATH + "/files"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/1.json")));
    }

    protected void stubImports() {
        final List<Integer> importIds = examples
            .all("imports")
            .stream()
            .map(Integer::parseInt)
            .sorted((a, b) -> b - a) // imports are returned in descending ID order from list endpoint
            .collect(Collectors.toList());

        importIds.forEach(this::stubImport);
        stubPaginatedList("imports", ".initialising", "id", false);
        stubStartImport();
    }

    protected void stubImport(int id) {
        final String scenarioName = "ImportProgression" + id;

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/imports/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("ImportDownloading")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/imports/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("ImportDownloading")
            .willSetStateTo("ImportCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".downloading.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/imports/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("ImportCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".completed.json")));

        stubDestroy("imports", String.valueOf(id), scenarioName);
    }

    protected void stubStartImport() {
        stubCreate("imports", "application/x-www-form-urlencoded");

        // Some clients (including v2 of our own PHP SDK) use multipart/form-data even though there
        // is no file to upload. This is a common mistake, so we should handle it gracefully.
        stubCreate("imports", "multipart/form-data");

        wiremock.stubFor(post(urlPathEqualTo(BASE_PATH + "/imports"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .withRequestBody(matching(".*url=.*unknown.*"))
            .withRequestBody(notContaining("filename"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/422.unknown_filename.json")));
    }

    protected void stubJobs() {
        final List<Integer> jobIds = examples
            .all("jobs")
            .stream()
            .map(Integer::parseInt)
            .sorted((a, b) -> b - a) // jobs are returned in descending ID order from list endpoint
            .collect(Collectors.toList());

        jobIds.forEach(this::stubJob);
        stubPaginatedList(
            "jobs",
            ".initialising",
            "id",
            false
        );
        stubPaginatedList(
            "jobs/successful",
            "jobs",
            ".completed",
            "id",
            false,
            n -> n.get("status").asText().equals("successful")
        );
        stubSubmitJob();
    }

    protected void stubJob(int id) {
        final String scenarioName = "JobProgression" + id;

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("JobConverting")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("JobConverting")
            .willSetStateTo("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".converting.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".completed.json")));

        wiremock.stubFor(delete(urlPathEqualTo(BASE_PATH + "/jobs/" + id))
            .inScenario(scenarioName)
            .willSetStateTo("JobCancelled")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".cancelled.json")));

        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("JobCancelled")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".cancelled.json")));

        stubDestroy("jobs", String.valueOf(id), scenarioName);
    }

    protected void stubSubmitJob() {
        stubCreate("jobs", "multipart/form-data");

        wiremock.stubFor(post(urlPathEqualTo(BASE_PATH + "/jobs"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .withRequestBody(matching(".*name=\"target_format\"[\\s\\S]*unsupported.*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/422.target_format.json")));
    }

    protected void stubPaginatedList(String resource, String idFieldName, boolean isAscending) {
        stubPaginatedList(resource, resource, "", idFieldName, isAscending, n -> true);
    }

    protected void stubPaginatedList(String resource, String filenameSuffix, String idFieldName, boolean isAscending) {
        stubPaginatedList(resource, resource, filenameSuffix, idFieldName, isAscending, n -> true);
    }

    protected void stubPaginatedList(String path, String resource, String filenameSuffix, String idFieldName, boolean isAscending, Predicate<JsonNode> filter) {
        wiremock.stubFor(get(urlMatching(BASE_PATH + "/" + path + "(\\?.*)?"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers(IndexTransformer.NAME)
                .withTransformerParameter(IndexTransformer.EXAMPLES_REPOSITORY_PARAMETER, examples)
                .withTransformerParameter(IndexTransformer.RESOURCE_PARAMETER, resource)
                .withTransformerParameter(IndexTransformer.FILENAME_SUFFIX_PARAMETER, filenameSuffix)
                .withTransformerParameter(IndexTransformer.ID_FIELD_NAME_PARAMETER, idFieldName)
                .withTransformerParameter(IndexTransformer.ASCENDING_PARAMETER, isAscending)
                .withTransformerParameter(IndexTransformer.PREDICATE_PARAMETER, filter)
            ));
    }

    protected void stubCreate(String resource, String contentType) {
        wiremock.stubFor(post(urlPathEqualTo(BASE_PATH + "/" + resource))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing(contentType))
            .atPriority(2) // to allow overriding for, say, returning 422s
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile(resource + "/1.initialising.json")
            ));
    }

    protected void stubDestroy(String resource, String id, String scenarioName) {
        wiremock.stubFor(post(urlPathEqualTo(BASE_PATH + "/" + resource + "/" + id + "/destroy"))
            .inScenario(scenarioName)
            .willSetStateTo("Destroyed")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));


        wiremock.stubFor(get(urlPathEqualTo(BASE_PATH + "/" + resource + "/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("Destroyed")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));
    }

    protected void stubCatchAlls() {
        wiremock.stubFor(any(urlPathMatching(".*"))
            .atPriority(99)
            .withHeader("Authorization", notMatching("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/401.json")));

        wiremock.stubFor(any(urlPathMatching(".*"))
            .atPriority(99)
            .withHeader("Authorization", absent())
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/401.json")));
    }
}

