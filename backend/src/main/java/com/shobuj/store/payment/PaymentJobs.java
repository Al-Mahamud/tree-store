package com.shobuj.store.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Every minute: ask the gateway about payments still waiting, and cancel orders not paid in time. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "store.payment.jobs.enabled", havingValue = "true", matchIfMissing = true)
class PaymentJobs {

    private final PaymentService payments;

    PaymentJobs(PaymentService payments) {
        this.payments = payments;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    void run() {
        payments.checkPending();
    }
}
