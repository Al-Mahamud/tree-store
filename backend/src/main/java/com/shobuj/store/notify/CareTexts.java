package com.shobuj.store.notify;

import com.shobuj.store.shared.StoreProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Care texts after delivery: settling in (day 2), watering (day 14) and the guarantee ending
 * (day 25 of 30). Each goes out at 10am Dhaka time and is written when it is sent, from the care
 * facts of the dearest plant in the order. Shoppers can turn them off at checkout or on their order page.
 */
@Service
public class CareTexts {

    static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
    /** Days after delivery for steps 1, 2 and 3. */
    static final int[] DAYS = {2, 14, 25};
    static final LocalTime SEND_AT = LocalTime.of(10, 0);
    /** Texts are only sent in the daytime, and dropped when the app was down for days. */
    static final int FIRST_HOUR = 9;
    static final int LAST_HOUR = 20;
    static final Duration TOO_LATE = Duration.ofDays(2);

    /** The plant a reminder talks about. */
    record Plant(String name, String light, String water, String placement) {
    }

    private record Due(long id, long orderId, int step, Instant dueAt, boolean on, String status, String number,
                       String phone) {
    }

    private final JdbcClient jdbc;
    private final SmsOutbox outbox;
    private final Clock clock;
    private final String shop;

    CareTexts(JdbcClient jdbc, SmsOutbox outbox, Clock clock, StoreProperties properties) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.clock = clock;
        this.shop = OrderTexts.shopAddress(properties.webUrl());
    }

    /** Called when an order is delivered (inside the same transaction). */
    public void schedule(long orderId, Instant deliveredAt) {
        ZonedDateTime day = deliveredAt.atZone(DHAKA).toLocalDate().atTime(SEND_AT).atZone(DHAKA);
        for (int step = 1; step <= DAYS.length; step++) {
            jdbc.sql("insert into care_reminders (order_id, step, due_at) values (:o, :s, :d) on conflict do nothing")
                    .param("o", orderId).param("s", step)
                    .param("d", Timestamp.from(day.plusDays(DAYS[step - 1]).toInstant())).update();
        }
    }

    /** Turns care texts on or off for one order. */
    public void setOn(long orderId, boolean on) {
        jdbc.sql("update orders set care_texts = :on where id = :id").param("on", on).param("id", orderId).update();
    }

    /** Queues the reminders that are due. Returns how many texts were queued. */
    @Transactional
    public int sendDue() {
        return sendDue(clock.instant());
    }

    /** As {@link #sendDue()}, at a given time (tests). */
    @Transactional
    public int sendDue(Instant now) {
        int hour = now.atZone(DHAKA).getHour();
        List<Due> due = jdbc.sql("""
                        select r.id, r.order_id, r.step, r.due_at, o.care_texts, o.status, o.number, o.phone
                          from care_reminders r join orders o on o.id = r.order_id
                         where r.done_at is null and r.due_at <= :now
                         order by r.due_at limit 100
                           for update of r skip locked""")
                .param("now", Timestamp.from(now))
                .query((rs, n) -> new Due(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getTimestamp(4).toInstant(),
                        rs.getBoolean(5), rs.getString(6), rs.getString(7), rs.getString(8)))
                .list();
        int queued = 0;
        for (Due r : due) {
            String outcome;
            if (!r.on() || !r.status().equals("DELIVERED")) {
                outcome = "OFF";
            } else if (r.dueAt().plus(TOO_LATE).isBefore(now)) {
                outcome = "LATE";
            } else if (hour < FIRST_HOUR || hour >= LAST_HOUR) {
                continue;
            } else {
                Optional<Plant> plant = plant(r.orderId());
                if (plant.isEmpty()) {
                    outcome = "OFF";
                } else {
                    outbox.queue(r.phone(), "CARE_REMINDER", text(r.step(), plant.get(), r.number(), shop), r.orderId(), false);
                    queued++;
                    outcome = "SENT";
                }
            }
            jdbc.sql("update care_reminders set done_at = :now, outcome = :o where id = :id")
                    .param("now", Timestamp.from(now)).param("o", outcome).param("id", r.id()).update();
        }
        return queued;
    }

    private Optional<Plant> plant(long orderId) {
        return jdbc.sql("""
                        select p.name, p.light, p.water, p.placement
                          from order_items i join products p on p.id = i.product_id
                         where i.order_id = :o
                         order by i.unit_price desc, i.id limit 1""")
                .param("o", orderId)
                .query((rs, n) -> new Plant(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .optional();
    }

    /** The text for one step, in the SMS alphabet and at most 160 characters. */
    static String text(int step, Plant plant, String orderNumber, String shop) {
        String name = OrderTexts.cut(plant.name(), 22);
        return switch (step) {
            case 1 -> plant.placement().equals("OUTDOOR")
                    ? "Shobuj: Your " + name + " has arrived! Keep it in light shade for 3-4 days, then plant it out "
                            + "on a cool evening and water it deeply."
                    : "Shobuj: Your " + name + " is settling in. Give it " + light(plant.light())
                            + " and don't repot for 3 weeks. A few droopy leaves at first are normal.";
            case 2 -> "Shobuj: 2 weeks with your " + name + "! " + water(plant.water()) + " Guides: " + shop + "/guides";
            case 3 -> "Shobuj: The 30-day plant guarantee on order " + orderNumber
                    + " ends in 5 days. If anything looks unwell, send us a photo: " + shop + "/pages/guarantee";
            default -> throw new IllegalArgumentException("step " + step);
        };
    }

    private static String light(String light) {
        return switch (light) {
            case "LOW" -> "a spot out of direct sun";
            case "MEDIUM" -> "medium, indirect light";
            case "BRIGHT" -> "bright, indirect light";
            default -> "as much direct sun as you can";
        };
    }

    private static String water(String water) {
        return switch (water) {
            case "LOW" -> "Let the soil dry out between waterings: too much water is the usual killer.";
            case "HIGH" -> "Keep the soil moist but never waterlogged, daily in hot weather.";
            default -> "Water when the top 2-3 cm of soil feels dry.";
        };
    }
}
