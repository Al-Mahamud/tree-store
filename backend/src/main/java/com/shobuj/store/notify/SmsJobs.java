package com.shobuj.store.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Sends waiting texts every few seconds, and queues care texts that are due every five minutes. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "store.sms.jobs.enabled", havingValue = "true", matchIfMissing = true)
class SmsJobs {

    private final SmsOutbox outbox;
    private final CareTexts care;

    SmsJobs(SmsOutbox outbox, CareTexts care) {
        this.outbox = outbox;
        this.care = care;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 5_000)
    void send() {
        outbox.sendQueued();
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    void care() {
        care.sendDue();
    }
}
