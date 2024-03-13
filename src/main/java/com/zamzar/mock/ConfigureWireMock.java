package com.zamzar.mock;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class ConfigureWireMock {

    protected static final String PATH_TO_EXAMPLES = "src/main/resources";

    protected static final String API_KEY = "GiVUYsF4A8ssq93FR48H";

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
        wiremock.stubFor(get(urlEqualTo("/account"))
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
        stubPaginatedList("files", fileIds.stream().map(Object::toString), "id", false);
        stubFileUpload();
    }

    protected void stubFile(int id) {
        final String scenarioName = "FileDeletion" + id;

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id + "/content"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/octet-stream")
                .withBodyFile("files/content/" + id)));

        wiremock.stubFor(delete(urlEqualTo("/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id + "/content"))
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
        stubPaginatedList("formats", formats.stream().map(Object::toString), "name", true);
    }

    protected void stubFormat(String name) {
        wiremock.stubFor(get(urlEqualTo("/formats/" + name))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("formats/" + name + ".json")));
    }

    protected void stubFileUpload() {
        wiremock.stubFor(post(urlPathEqualTo("/files"))
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
        stubPaginatedList("imports", importIds.stream().map(i -> i + ".initialising"), "id", false);
        stubStartImport();
    }

    protected void stubImport(int id) {
        final String scenarioName = "ImportProgression" + id;

        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("ImportDownloading")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("ImportDownloading")
            .willSetStateTo("ImportCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".downloading.json")));

        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
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
        stubCreate("imports");
    }

    protected void stubJobs() {
        final List<Integer> jobIds = examples
            .all("jobs")
            .stream()
            .map(Integer::parseInt)
            .sorted((a, b) -> b - a) // jobs are returned in descending ID order from list endpoint
            .collect(Collectors.toList());

        jobIds.forEach(this::stubJob);
        stubPaginatedList("jobs", jobIds.stream().map(i -> i + ".initialising"), "id", false);
        stubSubmitJob();
    }

    protected void stubJob(int id) {
        final String scenarioName = "JobProgression" + id;

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("JobConverting")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("JobConverting")
            .willSetStateTo("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".converting.json")));

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario(scenarioName)
            .whenScenarioStateIs("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".completed.json")));

        wiremock.stubFor(delete(urlEqualTo("/jobs/" + id))
            .inScenario(scenarioName)
            .willSetStateTo("JobCancelled")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".cancelled.json")));

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
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
        stubCreate("jobs");

        wiremock.stubFor(post(urlEqualTo("/jobs"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .withRequestBody(matching(".*name=\"target_format\"[\\s\\S]*unsupported.*"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/422.target_format.json")));
    }

    protected void stubPaginatedList(String resource, Stream<String> filenames, String idFieldName, boolean isAscending) {
        final List<String> paths = filenames
            .map(filename -> "__files/" + resource + "/" + filename + ".json")
            .collect(Collectors.toList());

        wiremock.stubFor(get(urlMatching("/" + resource + "(\\?.*)?"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withTransformers(IndexTransformer.NAME)
                .withTransformerParameter(IndexTransformer.EXAMPLES_REPOSITORY_PARAMETER, examples)
                .withTransformerParameter(IndexTransformer.RESOURCE_PARAMETER, resource)
                .withTransformerParameter(IndexTransformer.ID_FIELD_NAME_PARAMETER, idFieldName)
                .withTransformerParameter(IndexTransformer.ASCENDING_PARAMETER, isAscending)
            ));
    }

    protected void stubCreate(String resource) {
        wiremock.stubFor(post(urlPathEqualTo("/" + resource))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .atPriority(2) // to allow overriding for, say, returning 422s
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile(resource + "/1.initialising.json")
            ));
    }

    protected void stubDestroy(String resource, String id, String scenarioName) {
        wiremock.stubFor(post(urlEqualTo("/" + resource + "/" + id + "/destroy"))
            .inScenario(scenarioName)
            .willSetStateTo("Destroyed")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));


        wiremock.stubFor(get(urlEqualTo("/" + resource + "/" + id))
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

