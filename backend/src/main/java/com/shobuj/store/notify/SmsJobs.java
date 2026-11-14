package com.shobuj.store.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Sends waiting texts every few seconds. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "store.sms.jobs.enabled", havingValue = "true", matchIfMissing = true)
class SmsJobs {

    private final SmsOutbox outbox;

    SmsJobs(SmsOutbox outbox) {
        this.outbox = outbox;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 5_000)
    void send() {
        outbox.sendQueued();
    }
}
