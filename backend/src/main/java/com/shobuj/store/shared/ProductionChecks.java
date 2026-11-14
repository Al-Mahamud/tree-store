package com.shobuj.store.shared;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * On a live server, refuses to start with settings that would put customers or money at risk, and
 * says what to fix. Warnings are logged for settings that are allowed but unusual.
 */
@Component
@Profile("prod")
public class ProductionChecks implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ProductionChecks.class);

    private final Environment env;

    public ProductionChecks(Environment env) {
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = problems();
        warnings().forEach(w -> log.warn("Production setting: {}", w));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("The shop can't start with these settings:\n - " + String.join("\n - ", problems));
        }
    }

    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        String web = get("store.web-url");
        if (!web.startsWith("https://")) {
            problems.add("WEB_URL must be the shop's https:// address (it is '" + web + "').");
        }
        if (get("spring.datasource.password").isBlank()) {
            problems.add("DB_PASSWORD is empty.");
        }
        if (!"true".equals(get("server.servlet.session.cookie.secure"))) {
            problems.add("Sign-in cookies must be Secure (server.servlet.session.cookie.secure=true).");
        }
        boolean staging = "true".equals(get("store.staging"));
        if (get("store.sms.driver").equalsIgnoreCase("log") && !staging) {
            problems.add("SMS_DRIVER is 'log', so nobody could get a sign-up code. Set it to 'sslwireless' "
                    + "(or STAGING=true on a test server).");
        }
        String callback = get("store.payment.sslcommerz.callback-url");
        if (!get("store.payment.sslcommerz.store-id").isBlank() && !callback.startsWith("https://")) {
            problems.add("The SSLCommerz callback address must be https:// (it is '" + callback + "').");
        }
        return problems;
    }

    public List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        boolean payments = !get("store.payment.sslcommerz.store-id").isBlank();
        if (!payments) {
            warnings.add("SSLCOMMERZ_STORE_ID is not set: only cash on delivery is offered.");
        } else if (get("store.payment.sslcommerz.base-url").contains("sandbox")) {
            warnings.add("SSLCommerz is in sandbox mode: no real money is taken.");
        }
        if ("true".equals(get("store.staging"))) {
            warnings.add("STAGING=true: this server is for testing, not for customers.");
        }
        return warnings;
    }

    private String get(String key) {
        String value = env.getProperty(key);
        return value == null ? "" : value.trim();
    }
}
