package com.shobuj.store.notify;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * SSL Wireless "SMS Plus" API v3 ({@code POST /api/v3/send-sms}). Needs an API token and a sender
 * id (SID) from SSL Wireless. Written from their published API; confirm with a real account
 * before launch.
 */
public class SslWirelessGateway implements SmsGateway {

    private final String baseUrl;
    private final String apiToken;
    private final String sid;
    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public SslWirelessGateway(String baseUrl, String apiToken, String sid) {
        if (apiToken == null || apiToken.isBlank() || sid == null || sid.isBlank()) {
            throw new IllegalStateException("SMS_DRIVER=sslwireless needs SSLWIRELESS_API_TOKEN and SSLWIRELESS_SID");
        }
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
        this.sid = sid;
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(5));
        requests.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().requestFactory(requests).build();
    }

    @Override
    public String name() {
        return "sslwireless";
    }

    @Override
    public Result send(String phone, String text, String reference) {
        Map<String, String> body = Map.of("api_token", apiToken, "sid", sid, "msisdn", "88" + phone, "sms", text,
                "csms_id", reference);
        String answer = http.post().uri(baseUrl + "/api/v3/send-sms").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
        JsonNode node = json.readTree(answer == null ? "{}" : answer);
        if ("SUCCESS".equalsIgnoreCase(node.path("status").asString(""))) {
            return Result.ok(node.path("smsinfo").path(0).path("reference_id").asString(reference));
        }
        String error = node.path("error_message").asString("");
        return Result.failed(error.isBlank() ? "Rejected: " + node.path("status_code").asString("unknown") : error);
    }
}
