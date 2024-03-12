package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PageCoordinatesStringIdAscendingTest extends AbstractPageCoordinatesTest {

    protected static final String ID_FIELD_NAME = "name";

    @Test
    public void defaultsToFirstPageAndLimitOfFifty() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates().applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "zzz", 26), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursLimit() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "jjj", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void honoursAfter() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("iii"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("jjj", "sss", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenAfterSpecifiesLastFewItems() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("qqq")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("rrr", "zzz", 9), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenAfterIsFinalItem() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.after("zzz")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void honoursBefore() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("nnn"), 10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("ddd", "mmm", 10), page, ID_FIELD_NAME);
    }

    @Test
    public void truncatesWhenBeforeSpecifiesFirstFewItems() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("iii")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "hhh", 8), page, ID_FIELD_NAME);
    }

    @Test
    public void emptyWhenBeforeIsFirstItem() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("aaa")).applyTo(all, ID_FIELD_NAME);

        assertPageIsEmpty(page);
    }

    @Test
    public void retrievesAllItemsBeforeGivenIndexWhenNoLimitSpecified() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(Anchor.before("zzz")).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "yyy", 25), page, ID_FIELD_NAME);
    }

    @Test
    public void negativeLimitIsIgnored() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(-1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "zzz", 26), page, ID_FIELD_NAME);
    }

    @Test
    public void zeroLimitIsIgnored() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(0).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "zzz", 26), page, ID_FIELD_NAME);
    }

    @Test
    public void limitGreaterThanMaxIsIgnored() {
        final List<JsonNode> all = items(26);
        final List<JsonNode> page = new PageCoordinates(PageCoordinates.MAX_LIMIT + 1).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "zzz", 26), page, ID_FIELD_NAME);
    }

    @Test
    public void limitLargerThanAllItems() {
        final List<JsonNode> all = items(9);
        final List<JsonNode> page = new PageCoordinates(10).applyTo(all, ID_FIELD_NAME);

        assertPageContains(new PageExpectation("aaa", "iii", 9), page, ID_FIELD_NAME);
    }

    // generates a list of items of the form <'aaa', 'bbb', 'ccc', ...>
    protected static List<JsonNode> items(int numberOfItems) {
        final ObjectMapper mapper = new ObjectMapper();

        return IntStream.range(0, numberOfItems)
            .mapToObj(i -> String.valueOf(Character.toChars('a' + i)).repeat(3))
            .map(n -> mapper.createObjectNode().put(ID_FIELD_NAME, n))
            .collect(Collectors.toList());
    }
}
