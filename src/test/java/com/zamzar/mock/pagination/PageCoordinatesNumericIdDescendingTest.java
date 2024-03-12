package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PageCoordinatesNumericIdDescendingTest extends AbstractPageCoordinatesTest {

    protected static final String ID_FIELD_NAME = "id";

    @Test
    public void defaultsToFirstPageAndLimitOfFifty() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates().applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "51", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursLimit() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "91", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursAfter() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("99"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("98", "89", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenAfterSpecifiesLastFewItems() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("10")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("9", "1", 9), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenAfterIsFinalItem() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("1")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void honoursBefore() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("80"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("90", "81", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenBeforeSpecifiesFirstFewItems() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("92")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "93", 8), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenBeforeIsFirstItem() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("100")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void retrievesAllItemsBeforeGivenIndexWhenNoLimitSpecified() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("51")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "52", 49), page, ID_FIELD_NAME);
    }

    @Test
    public void negativeLimitIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(-1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "51", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void zeroLimitIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(0).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "51", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void limitGreaterThanMaxIsIgnored() {
        final List<JsonNode> all = items(100);
        final List<JsonNode> page = new PageCoordinates(PageCoordinates.MAX_LIMIT + 1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("100", "51", 50), page, ID_FIELD_NAME);
    }

    @Test
    public void limitLargerThanAllItems() {
        final List<JsonNode> all = items(9);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("9", "1", 9), page, ID_FIELD_NAME);
    }

    protected static List<JsonNode> items(int numberOfItems) {
        final ObjectMapper mapper = new ObjectMapper();

        return IntStream.rangeClosed(1, numberOfItems)
            .boxed()
            .sorted((a, b) -> b - a)  // reverse order (lke files / jobs / imports)
            .map(n -> mapper.createObjectNode().put(ID_FIELD_NAME, n))
            .collect(Collectors.toList());
    }
}
