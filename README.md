# Zamzar Mock

[![@zamzar on Twitter](https://img.shields.io/badge/twitter-zamzar-blue)](https://twitter.com/zamzar)
[![Docker Pulls](https://img.shields.io/docker/pulls/zamzar/zamzar-mock)](https://hub.docker.com/r/zamzar/zamzar-mock)
[![GitHub License](https://img.shields.io/github/license/zamzar/zamzar-mock)](https://github.com/zamzar/zamzar-mock/blob/main/LICENSE)

`zamzar-mock` is an HTTP server that acts like the real Zamzar API, making your tests less brittle.

Run with:

```
docker run --rm --name zamzar-mock -p 8080:8080 zamzar/zamzar-mock
```

Then issue API requests as follows:

```
curl http://localhost:8080/jobs/1 -H 'Authorization: Bearer GiVUYsF4A8ssq93FR48H'
```

For more information on available endpoints, please see
the [Zamzar API documentation](https://developers.zamzar.com/docs).

## Features

* Support for all endpoints in the [Zamzar API](https://developers.zamzar.com/docs)
* Polling a job or import will cause it to advance through its lifecycle, from `initialising` to `successful` (
  or `failed`).
* Error responses are returned for requests containing keywords:
    * `POST /jobs` with a target format of `unsupported` will return a 422
    * `POST /imports` with a URL containing `unknown` and no filename will return a 422 (implying that the URL's
      filename cannot be inferred)
* Additional endpoints for testing:
    * `POST /jobs/ID/destroy` - Remove a job entirely
    * `POST /imports/ID/destroy` - Remove an import entirely
    * `POST /__admin/scenarios/reset` - Reset the server to its initial state

### Test Jobs

By default, `zamzar-mock` will boot with the following example jobs:

* 1 - A successful conversion from `example.mp3` to `example.txt`
* 2 - A successful conversion from `example.key` to `example-*.png` (multiple output files)
* 3 - A failed conversion from `example.doc` to `example.pdf`

### Test Imports

By default, `zamzar-mock` will boot with the following example imports:

* 1 - A successful import of `example.mp3`
* 2 - A failed import of `example.doc` (due to a file size limit)
* 3 - A failed import of `example.doc` (due to a credentials error)

### Test Files

By default, `zamzar-mock` will boot with at least 7 example files.

## Important Caveats

* `zamzar-mock` will not actually convert / import / export files.
* `zamzar-mock` cannot create new jobs or imports (though it will return a 2xx response for any POST request that would
  create a resource in the real API).
* The formats, conversions and credit costs provided by `zamzar-mock` are not guaranteed to be accurate. Please see the
  Zamzar API's [Supported Conversions](https://developers.zamzar.com/formats) for the most up-to-date information.