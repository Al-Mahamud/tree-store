package com.shobuj.store.customer;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.StoreProperties.Zone;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's saved delivery addresses. The first one becomes the default. */
@Service
public class AddressService {

    public static final int MAX_ADDRESSES = 10;

    public record AddressInput(String label, String name, String phone, String address, String area, String city,
                               Zone zone, String note) {
    }

    public record SavedAddress(long id, String label, String name, String phone, String address, String area,
                               String city, Zone zone, String note, boolean isDefault) {
    }

    private final JdbcClient jdbc;

    AddressService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<SavedAddress> list(long customerId) {
        return jdbc.sql("""
                        select id, label, name, phone, address, area, city, zone, note, is_default
                          from addresses where customer_id = :c order by is_default desc, created_at desc, id desc""")
                .param("c", customerId)
                .query((rs, n) -> new SavedAddress(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), Zone.valueOf(rs.getString(8)), rs.getString(9),
                        rs.getBoolean(10)))
                .list();
    }

    @Transactional
    public SavedAddress create(long customerId, AddressInput input, boolean makeDefault) {
        int count = count(customerId);
        if (count >= MAX_ADDRESSES) {
            throw ApiException.conflict("address.limit", "You can save up to " + MAX_ADDRESSES + " addresses. Remove one first.");
        }
        boolean isDefault = makeDefault || count == 0;
        if (isDefault) {
            clearDefault(customerId);
        }
        long id = jdbc.sql("""
                        insert into addresses (customer_id, label, name, phone, address, area, city, zone, note, is_default)
                        values (:c, :label, :name, :phone, :address, :area, :city, :zone, :note, :def) returning id""")
                .param("c", customerId).param("label", input.label()).param("name", input.name())
                .param("phone", input.phone()).param("address", input.address()).param("area", input.area())
                .param("city", input.city()).param("zone", input.zone().name()).param("note", input.note())
                .param("def", isDefault)
                .query(Long.class).single();
        return get(customerId, id);
    }

    @Transactional
    public SavedAddress update(long customerId, long id, AddressInput input) {
        get(customerId, id);
        jdbc.sql("""
                        update addresses set label = :label, name = :name, phone = :phone, address = :address,
                                             area = :area, city = :city, zone = :zone, note = :note
                         where id = :id and customer_id = :c""")
                .param("label", input.label()).param("name", input.name()).param("phone", input.phone())
                .param("address", input.address()).param("area", input.area()).param("city", input.city())
                .param("zone", input.zone().name()).param("note", input.note()).param("id", id).param("c", customerId)
                .update();
        return get(customerId, id);
    }

    @Transactional
    public void delete(long customerId, long id) {
        SavedAddress removed = get(customerId, id);
        jdbc.sql("delete from addresses where id = :id and customer_id = :c").param("id", id).param("c", customerId).update();
        if (removed.isDefault()) {
            jdbc.sql("""
                            update addresses set is_default = true where id = (
                                select id from addresses where customer_id = :c order by created_at desc, id desc limit 1)""")
                    .param("c", customerId).update();
        }
    }

    @Transactional
    public SavedAddress makeDefault(long customerId, long id) {
        get(customerId, id);
        clearDefault(customerId);
        jdbc.sql("update addresses set is_default = true where id = :id").param("id", id).update();
        return get(customerId, id);
    }

    /** From checkout: saves the address unless the customer already has the same one. */
    @Transactional
    public void rememberFromCheckout(long customerId, AddressInput input) {
        boolean known = jdbc.sql("""
                        select count(*) from addresses
                         where customer_id = :c and lower(trim(address)) = lower(trim(:address))
                           and lower(trim(area)) = lower(trim(:area)) and lower(trim(city)) = lower(trim(:city))""")
                .param("c", customerId).param("address", input.address()).param("area", input.area())
                .param("city", input.city()).query(Integer.class).single() > 0;
        if (!known && count(customerId) < MAX_ADDRESSES) {
            create(customerId, input, false);
        }
    }

    private int count(long customerId) {
        return jdbc.sql("select count(*) from addresses where customer_id = :c").param("c", customerId).query(Integer.class).single();
    }

    private SavedAddress get(long customerId, long id) {
        return list(customerId).stream().filter(a -> a.id() == id).findFirst()
                .orElseThrow(() -> ApiException.notFound("address"));
    }

    private void clearDefault(long customerId) {
        jdbc.sql("update addresses set is_default = false where customer_id = :c and is_default").param("c", customerId).update();
    }
}
