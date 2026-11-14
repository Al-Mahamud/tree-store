package com.shobuj.store.order;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Gives back what an order took when it is cancelled: stock, sales counts and the coupon use. */
@Component
public class OrderStock {

    private final JdbcClient jdbc;

    OrderStock(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Call with the order row locked, in the transaction that cancels it. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(long orderId) {
        jdbc.sql("""
                        update variants k set stock = k.stock + i.quantity
                          from (select variant_id, sum(quantity) as quantity from order_items
                                 where order_id = :o and variant_id is not null group by variant_id) i
                         where k.id = i.variant_id""")
                .param("o", orderId).update();
        jdbc.sql("""
                        update products p set sold_count = greatest(0, p.sold_count - i.quantity)
                          from (select product_id, sum(quantity) as quantity from order_items
                                 where order_id = :o and product_id is not null group by product_id) i
                         where p.id = i.product_id""")
                .param("o", orderId).update();
        jdbc.sql("""
                        update coupons set used_count = greatest(0, used_count - 1)
                         where code = (select coupon_code from orders where id = :o)""")
                .param("o", orderId).update();
    }
}
