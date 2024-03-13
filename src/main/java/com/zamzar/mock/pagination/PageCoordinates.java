package com.zamzar.mock.pagination;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

public class PageCoordinates {
    public static final int MAX_LIMIT = 50;
    public static final int DEFAULT_LIMIT = MAX_LIMIT;
    public static Anchor DEFAULT_ANCHOR = null;
    protected final Anchor anchor;
    protected final int limit;

    public PageCoordinates() {
        this(DEFAULT_ANCHOR, DEFAULT_LIMIT);
    }

    public PageCoordinates(Anchor anchor) {
        this(anchor, DEFAULT_LIMIT);
    }

    public PageCoordinates(int limit) {
        this(DEFAULT_ANCHOR, limit);
    }

    public PageCoordinates(Anchor anchor, int limit) {
        this.anchor = anchor;
        this.limit = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    public List<JsonNode> applyTo(List<JsonNode> items, String idFieldName) {
        final Range range = range(items, idFieldName);
        return items.subList(range.from, range.to);
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        return "PageCoordinates{" +
            "anchor=" + anchor +
            ", limit=" + limit +
            '}';
    }

    protected Range range(List<JsonNode> items, String idFieldName) {
        if (anchor == null) {
            int from = 0;
            int to = Math.min(limit, items.size());
            return new Range(from, to);

        } else if (anchor.orientation == Anchor.Orientation.AFTER) {
            // start from the item after the anchor
            int from = Math.min(getIndexOfAnchor(items, idFieldName) + 1, items.size());
            int to = Math.min(from + limit, items.size());
            return new Range(from, to);

        } else {
            // end at the item before the anchor
            int to = Math.max(getIndexOfAnchor(items, idFieldName), 0);
            int from = Math.max(to - limit, 0);
            return new Range(from, to);
        }
    }

    private int getIndexOfAnchor(List<JsonNode> items, String idFieldName) {
        return items.stream()
            .filter(i -> Objects.equals(i.get(idFieldName).asText(), anchor.ref)).
            findFirst().map(items::indexOf)
            .orElseThrow(() -> new IllegalArgumentException(anchor.ref + " not found in items"));
    }

    protected static class Range {
        protected final int from;
        protected final int to;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }
}
