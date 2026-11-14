package com.shobuj.store.customer;

import com.shobuj.store.shared.ApiException;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer accounts: a mobile number and password, a name and an optional email. */
@Service
public class CustomerService {

    public record Profile(long id, String name, String phone, String email) {
    }

    private record Credentials(long id, String hash) {
    }

    private final JdbcClient jdbc;
    private final PasswordEncoder passwords;
    /** Compared against when the number is unknown, so a wrong number takes as long as a wrong password. */
    private final String dummyHash;

    CustomerService(JdbcClient jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.dummyHash = passwords.encode("not-a-real-password");
    }

    public boolean exists(String phone) {
        return jdbc.sql("select count(*) from customers where phone = :p").param("p", phone).query(Integer.class).single() > 0;
    }

    /** A new account whose number was just proven with a code. */
    @Transactional
    public Profile register(String name, String phone, String email, String password) {
        try {
            long id = jdbc.sql("""
                            insert into customers (name, phone, email, password_hash, phone_verified_at)
                            values (:name, :phone, :email, :hash, now()) returning id""")
                    .param("name", name).param("phone", phone).param("email", email)
                    .param("hash", passwords.encode(password))
                    .query(Long.class).single();
            return new Profile(id, name, phone, email);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.phone_taken",
                    "An account with this mobile number already exists. Please sign in instead.",
                    Map.of("phone", "This number already has an account."));
        }
    }

    /** The customer id if the password matches. */
    public Optional<Long> authenticate(String phone, String password) {
        Optional<Credentials> found = jdbc.sql("select id, password_hash from customers where phone = :phone")
                .param("phone", phone)
                .query((rs, n) -> new Credentials(rs.getLong(1), rs.getString(2))).optional();
        boolean matches = passwords.matches(password, found.map(Credentials::hash).orElse(dummyHash));
        return matches ? found.map(Credentials::id) : Optional.empty();
    }

    public Profile profile(long id) {
        return jdbc.sql("select id, name, phone, email from customers where id = :id").param("id", id)
                .query((rs, n) -> new Profile(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .optional()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth.required", "Please sign in."));
    }

    public Optional<Profile> find(long id) {
        return jdbc.sql("select id, name, phone, email from customers where id = :id").param("id", id)
                .query((rs, n) -> new Profile(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .optional();
    }

    @Transactional
    public Profile update(long id, String name, String email) {
        jdbc.sql("update customers set name = :name, email = :email, updated_at = now() where id = :id")
                .param("name", name).param("email", email).param("id", id).update();
        return profile(id);
    }

    /**
     * Orders placed as a guest with this (now proven) number join the account, so they show in its
     * history and "buy again". Returns how many.
     */
    @Transactional
    public int linkGuestOrders(long id, String phone) {
        return jdbc.sql("update orders set customer_id = :id where phone = :p and customer_id is null")
                .param("id", id).param("p", phone).update();
    }

    /** After a code proved the number: a new password. Returns the account id, if there is one. */
    @Transactional
    public Optional<Long> resetPassword(String phone, String password) {
        Optional<Long> id = jdbc.sql("select id from customers where phone = :p").param("p", phone).query(Long.class).optional();
        id.ifPresent(c -> jdbc.sql("""
                        update customers set password_hash = :h, phone_verified_at = coalesce(phone_verified_at, now()),
                                             updated_at = now()
                         where id = :id""")
                .param("h", passwords.encode(password)).param("id", c).update());
        return id;
    }

    @Transactional
    public void changePassword(long id, String current, String next) {
        String hash = jdbc.sql("select password_hash from customers where id = :id").param("id", id)
                .query(String.class).single();
        if (!passwords.matches(current, hash)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "auth.wrong_password", "Please check the highlighted fields.",
                    Map.of("currentPassword", "That isn't your current password."));
        }
        jdbc.sql("update customers set password_hash = :hash, updated_at = now() where id = :id")
                .param("hash", passwords.encode(next)).param("id", id).update();
    }
}
