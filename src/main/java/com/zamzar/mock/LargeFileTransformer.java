package com.zamzar.mock;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class LargeFileTransformer implements ResponseTransformerV2 {

    public static final String NAME = "large-file-transformer";

    public static final String GET_SIZE_IN_MB_PARAMETER = "sizeInMb";

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        byte[] largeContent = new byte[getSizeInMb(serveEvent.getTransformerParameters()) * 1024 * 1024];

        return Response.Builder.like(response).but()
            .headers(new HttpHeaders(new HttpHeader("Content-Type", "application/octet-stream")))
            .body(largeContent)
            .status(200)
            .build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected int getSizeInMb(Parameters parameters) {
        return parameters.getInt(GET_SIZE_IN_MB_PARAMETER);
    }
}

