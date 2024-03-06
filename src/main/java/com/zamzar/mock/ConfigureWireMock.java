package com.zamzar.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class ConfigureWireMock {
    public static void main(String[] args) {
        printBanner();

        final WireMockServer wireMockServer = startWireMock();
        new ConfigureWireMock(wireMockServer).run();

        // Keep the application running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {}
    }

    protected static WireMockServer startWireMock() {
        final WireMockConfiguration config = options().fileSource(new SingleRootFileSource("src/main/resources"));
        final WireMockServer wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        System.out.println("zamzar-mock is running at: " + wireMockServer.baseUrl());
        System.out.println();

        return wireMockServer;
    }

    protected static void printBanner() {
        try (InputStream inputStream = ConfigureWireMock.class.getResourceAsStream("/banner.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException | NullPointerException e) {}

        System.out.println();
    }

    protected static final String API_KEY = "GiVUYsF4A8ssq93FR48H";

    protected WireMockServer wiremock;

    public ConfigureWireMock(WireMockServer wiremock) {
        this.wiremock = wiremock;
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
        IntStream.rangeClosed(1, 7).forEach(this::stubFile);
        stubFileUpload();
    }

    protected void stubFile(int id) {
        wiremock.stubFor(get(urlPathEqualTo("/files/" + id))
            .inScenario("FileDeletion" + id)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id + "/content"))
            .inScenario("FileDeletion" + id)
            .whenScenarioStateIs(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/octet-stream")
                .withBodyFile("files/content/" + id)));

        wiremock.stubFor(delete(urlEqualTo("/files/" + id))
            .inScenario("FileDeletion" + id)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("files/" + id + ".json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id))
            .inScenario("FileDeletion" + id)
            .whenScenarioStateIs("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));

        wiremock.stubFor(get(urlPathEqualTo("/files/" + id + "/content"))
            .inScenario("FileDeletion" + id)
            .whenScenarioStateIs("FileDeleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("errors/404.json")));
    }

    protected void stubFormats() {
        List.of("mp3").forEach(this::stubFormat);
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
        IntStream.rangeClosed(1, 1).forEach(this::stubImport);
        stubStartImport();
    }

    protected void stubImport(int id) {
        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
            .inScenario("ImportProgression" + id)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("ImportDownloading")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
            .inScenario("ImportProgression" + id)
            .whenScenarioStateIs("ImportDownloading")
            .willSetStateTo("ImportCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".downloading.json")));

        wiremock.stubFor(get(urlEqualTo("/imports/" + id))
            .inScenario("ImportProgression" + id)
            .whenScenarioStateIs("ImportCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/" + id + ".completed.json")));
    }

    protected void stubStartImport() {
        wiremock.stubFor(post(urlPathEqualTo("/imports"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("imports/1.initialising.json")));
    }

    protected void stubJobs() {
        IntStream.rangeClosed(1, 3).forEach(this::stubJob);
        stubSubmitJob();
    }

    protected void stubJob(int id) {
        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario("JobProgression" + id)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("JobConverting")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".initialising.json")));

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario("JobProgression" + id)
            .whenScenarioStateIs("JobConverting")
            .willSetStateTo("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".converting.json")));

        wiremock.stubFor(get(urlEqualTo("/jobs/" + id))
            .inScenario("JobProgression" + id)
            .whenScenarioStateIs("JobCompleted")
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/" + id + ".completed.json")));
    }

    protected void stubSubmitJob() {
        wiremock.stubFor(post(urlPathEqualTo("/jobs"))
            .withHeader("Authorization", equalTo("Bearer " + API_KEY))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .withRequestBody(containing("name=\"target_format\""))
            .atPriority(2)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("jobs/1.initialising.json")));

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

