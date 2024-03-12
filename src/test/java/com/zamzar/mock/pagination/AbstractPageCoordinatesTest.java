package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractPageCoordinatesTest {

    protected static void assertPageIsEmpty(List<?> actual) {
        assertEquals(0, actual.size());
    }

    protected static void assertPageContains(PageExpectation expected, List<JsonNode> actual, String idFieldName) {
        assertEquals(expected.first, actual.get(0).get(idFieldName).asText(), "Unexpected first item");
        assertEquals(expected.last, actual.get(actual.size() - 1).get(idFieldName).asText(), "Unexpected last item");
        assertEquals(expected.size, actual.size(), "Unexpected page size");
    }

    protected static class PageExpectation {
        public final String first;
        public final String last;
        public final int size;

        public PageExpectation(String first, String last, int size) {
            this.first = first;
            this.last = last;
            this.size = size;
        }
    }
}
