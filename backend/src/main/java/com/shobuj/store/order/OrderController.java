package com.shobuj.store.order;

import com.shobuj.store.order.OrderService.OrderView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private final OrderService orders;

    OrderController(OrderService orders) {
        this.orders = orders;
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
