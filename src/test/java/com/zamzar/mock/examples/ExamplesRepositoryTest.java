package com.zamzar.mock.examples;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExamplesRepositoryTest {

    protected final FileSource fileSource = new SingleRootFileSource("src/test/resources");
    protected final ExamplesRepository repo = new ExamplesRepository(fileSource);

    @BeforeEach
    public void setUp() {
        // Delete the update created in the create() test
        if (repo.exists("updates", "2")) {
            repo.delete("updates", "2");
        }
    }

    @Test
    public void all() {
        final Collection<String> actual = repo.all("widgets").stream().sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("bolt", "flux-capacitor"), actual);
    }

    @Test
    public void allIgnoresStateByDefault() {
        final Collection<String> actual = repo.all("updates").stream().sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("1"), actual);
    }

    @Test
    public void allWithState() {
        final Collection<String> actual = repo.all("updates", true).stream().sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList("1.completed", "1.initialising", "1.installing"), actual);
    }

    @Test
    public void exists() {
        assertTrue(repo.exists("updates", "1"));
        assertFalse(repo.exists("updates", "2"));
    }

    @Test
    public void read() {
        assertTrue(repo.read("widgets", "bolt").contains("\"name\": \"bolt\""));
        assertTrue(repo.read("updates", "1.initialising").contains("\"id\": 1"));
    }
}
