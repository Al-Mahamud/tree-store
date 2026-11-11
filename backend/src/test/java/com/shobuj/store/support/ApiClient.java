package com.shobuj.store.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Talks to the running application like the web app does: keeps cookies and sends the CSRF
 * cookie back in {@code X-XSRF-TOKEN}. One instance is one browser.
 */
public final class ApiClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public record Response(int status, JsonNode body) {
    }

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http;

    public ApiClient(int port) {
        this.baseUrl = "http://localhost:" + port;
        this.http = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    public Response get(String path) {
        return send("GET", path, null);
    }

    public Response post(String path, Object body) {
        return send("POST", path, body);
    }

    public Response patch(String path, Object body) {
        return send("PATCH", path, body);
    }

    public Response put(String path, Object body) {
        return send("PUT", path, body);
    }

    public Response delete(String path) {
        return send("DELETE", path, null);
    }

    /** A write without the CSRF header, to prove the protection works. */
    public Response postWithoutCsrf(String path, Object body) {
        return send("POST", path, body, false);
    }

    private Response send(String method, String path, Object body) {
        return send(method, path, body, true);
    }

    private Response send(String method, String path, Object body, boolean withCsrf) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Accept", "application/json");
            if (!method.equals("GET") && withCsrf) {
                request.header("X-XSRF-TOKEN", csrfToken());
            }
            if (body != null) {
                request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            } else {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            String text = response.body();
            boolean isJson = response.headers().firstValue("Content-Type").orElse("").contains("json");
            JsonNode json = text == null || text.isBlank() ? JSON.nullNode()
                    : isJson ? JSON.readTree(text) : JSON.getNodeFactory().stringNode(text);
            return new Response(response.statusCode(), json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Any request sets the XSRF-TOKEN cookie; make one if we have none yet. */
    private String csrfToken() throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 2; attempt++) {
            for (HttpCookie cookie : cookies.getCookieStore().getCookies()) {
                if (cookie.getName().equals("XSRF-TOKEN")) {
                    return cookie.getValue();
                }
            }
            http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/csrf")).build(),
                    HttpResponse.BodyHandlers.discarding());
        }
        return "";
    }
}
