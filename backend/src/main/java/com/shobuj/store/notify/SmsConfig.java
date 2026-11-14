package com.shobuj.store.notify;

import com.shobuj.store.shared.StoreProperties;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Picks the text-message sender from {@code store.sms.driver}. */
@Configuration(proxyBeanMethods = false)
class SmsConfig {

    @Bean
    SmsGateway smsGateway(StoreProperties properties, Environment environment) {
        String driver = properties.sms() == null ? "log" : properties.sms().driver();
        if ("sslwireless".equalsIgnoreCase(driver)) {
            return new SslWirelessGateway(
                    environment.getProperty("store.sms.sslwireless.base-url", "https://smsplus.sslwireless.com"),
                    environment.getProperty("store.sms.sslwireless.api-token", ""),
                    environment.getProperty("store.sms.sslwireless.sid", ""));
        }
        if (!environment.acceptsProfiles(Profiles.of("local", "test"))) {
            LoggerFactory.getLogger(SmsConfig.class).warn("SMS_DRIVER is 'log': customers will not receive any texts");
        }
        return new LogSmsGateway();
    }
}
