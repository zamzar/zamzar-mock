package com.zamzar.mock;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.zamzar.mock.pagination.Anchor;
import com.zamzar.mock.pagination.PageCoordinates;
import com.zamzar.mock.pagination.PagedResponse;

import java.util.List;

public class PaginationTransformer implements ResponseTransformerV2 {
    public static final String NAME = "pagination-transformer";

    public static final String FILE_SOURCE_PARAMETER = "fileSource";
    public static final String PATHS_PARAMETER = "paths";
    public static final String ID_FIELD_NAME_PARAMETER = "idFieldName";

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        final Parameters parameters = serveEvent.getTransformerParameters();
        final Request request = serveEvent.getRequest();

        final PagedResponse paged = new PagedResponse(
            getFileSource(parameters),
            getPaths(parameters),
            getCoordinates(request),
            getIdFieldName(parameters)
        );

        return Response.Builder.like(response).but()
            .body(paged.toPrettyString())
            .build();
    }

    protected FileSource getFileSource(Parameters parameters) {
        return (FileSource) parameters.get(FILE_SOURCE_PARAMETER);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getPaths(Parameters parameters) {
        return (List<String>) parameters.get(PATHS_PARAMETER);
    }

    protected String getIdFieldName(Parameters parameters) {
        return (String) parameters.get(ID_FIELD_NAME_PARAMETER);
    }

    protected PageCoordinates getCoordinates(Request request) {
        final int limit = request.queryParameter("limit").isPresent() ?
            Integer.parseInt(request.queryParameter("limit").values().get(0)) :
            PageCoordinates.DEFAULT_LIMIT;

        Anchor anchor;
        if (request.queryParameter("before").isPresent()) {
            anchor = Anchor.before(request.queryParameter("before").values().get(0));
        } else if (request.queryParameter("after").isPresent()) {
            anchor = Anchor.after(request.queryParameter("after").values().get(0));
        } else {
            anchor = PageCoordinates.DEFAULT_ANCHOR;
        }

        return new PageCoordinates(anchor, limit);
    }
}
