package com.shobuj.store.order;

import com.shobuj.store.order.OrderService.OrderView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private final OrderService orders;
    private final com.shobuj.store.notify.CareTexts care;

    OrderController(OrderService orders, com.shobuj.store.notify.CareTexts care) {
        this.orders = orders;
        this.care = care;
    }

    record CareTextsBody(boolean on) {
    }

    /** Turns care texts on or off: for whoever holds the order's link, or the customer who placed it. */
    @PutMapping("/{number}/care-texts")
    CareTextsBody careTexts(@PathVariable String number, @RequestParam(required = false) String token,
                            @RequestBody CareTextsBody body) {
        care.setOn(orders.idFor(number, token), body.on());
        return body;
    }

    @GetMapping("/track")
    OrderView track(@RequestParam String number, @RequestParam String phone) {
        return orders.track(number, phone);
    }

    @GetMapping("/{number}")
    OrderView get(@PathVariable String number, @RequestParam String token) {
        return orders.byToken(number, token);
    }
}
