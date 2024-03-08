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

For more information on available endpoints, please see the [Zamzar API documentation](https://developers.zamzar.com/docs).