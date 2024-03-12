package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PageCoordinatesNumericIdAscendingTest extends AbstractPageCoordinatesTest {

    protected static final String ID_FIELD_NAME = "id";

    @Test
    public void defaultsToFirstPageAndLimitOfFifty() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates().applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "50", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursLimit() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "10", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursAfter() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("9"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("10", "19", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenAfterSpecifiesLastFewItems() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("91")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("92", "100", 9), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenAfterIsFinalItem() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("100")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void honoursBefore() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("80"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("70", "79", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenBeforeSpecifiesFirstFewItems() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("9")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "8", 8), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenBeforeIsFirstItem() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("1")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void retrievesAllItemsBeforeGivenIndexWhenNoLimitSpecified() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("50")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "49", 49), page, ID_FIELD_NAME);
    }

    @Test
    public void negativeLimitIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(-1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "50", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void zeroLimitIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(0).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "50", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void limitGreaterThanMaxIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(PageCoordinates.MAX_LIMIT + 1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "50", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void limitLargerThanAllItems() {
        final List<JsonNode> all = items(9);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("1", "9", 9), page, ID_FIELD_NAME);
    }

    protected static List<JsonNode> items(int numberOfItems) {
        final ObjectMapper mapper = new ObjectMapper();

        return IntStream.rangeClosed(1, numberOfItems)
            .boxed()
            .map(n -> mapper.createObjectNode().put(ID_FIELD_NAME, n))
            .collect(Collectors.toList());
    }
}
