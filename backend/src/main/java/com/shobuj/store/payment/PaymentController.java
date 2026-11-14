package com.shobuj.store.payment;

import com.shobuj.store.order.OrderService;
import com.shobuj.store.payment.PaymentService.Options;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Paying for an order online, or switching it to cash on delivery. Guests prove access with the order's token. */
@RestController
class PaymentController {

    record Access(String token) {
    }

    record PaymentPage(String paymentUrl) {
    }

    private final PaymentService payments;
    private final OrderService orders;

    PaymentController(PaymentService payments, OrderService orders) {
        this.payments = payments;
        this.orders = orders;
    }

    @GetMapping("/api/v1/payments/options")
    Options options() {
        return payments.options();
    }

    @PostMapping("/api/v1/orders/{number}/pay")
    PaymentPage pay(@PathVariable String number, @RequestBody(required = false) Access body) {
        long id = orders.idFor(number, body == null ? null : body.token());
        return new PaymentPage(payments.start(id));
    }

    @PostMapping("/api/v1/orders/{number}/cash-on-delivery")
    OrderService.OrderView cash(@PathVariable String number, @RequestBody(required = false) Access body) {
        long id = orders.idFor(number, body == null ? null : body.token());
        payments.switchToCash(id);
        return orders.byId(id).orElseThrow();
    }
}
