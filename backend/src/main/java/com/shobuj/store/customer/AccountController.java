package com.shobuj.store.customer;

import com.shobuj.store.catalog.CatalogViews.ProductCard;
import com.shobuj.store.catalog.ProductSearch;
import com.shobuj.store.checkout.Bags;
import com.shobuj.store.checkout.CartService;
import com.shobuj.store.checkout.CartService.CartView;
import com.shobuj.store.customer.AddressService.AddressInput;
import com.shobuj.store.customer.AddressService.SavedAddress;
import com.shobuj.store.customer.CustomerService.Profile;
import com.shobuj.store.order.OrderService;
import com.shobuj.store.order.OrderService.History;
import com.shobuj.store.order.OrderService.OrderView;
import com.shobuj.store.order.OrderService.PastItem;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.CurrentCustomer;
import com.shobuj.store.shared.LoginSessions;
import com.shobuj.store.shared.StoreProperties.Zone;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in customer's own things: profile, password, addresses, orders and wishlist. */
@RestController
@RequestMapping("/api/v1/account")
class AccountController {

    record ProfileUpdate(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email) {
    }

    record PasswordChange(@NotBlank(message = "Please enter your current password.") String currentPassword,
                          @NotBlank(message = "Please choose a new password.")
                          @Size(min = 8, max = 72, message = "Use at least 8 characters.") String newPassword) {
    }

    record AddressBody(
            @Size(max = 20, message = "Keep the label short, like Home or Office.") String label,
            @NotBlank(message = "Please enter the receiver's name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter a mobile number.") String phone,
            @NotBlank(message = "Please enter the address.") @Size(min = 5, max = 200, message = "Please enter the full address.") String address,
            @NotBlank(message = "Please enter the area or thana.") @Size(max = 80) String area,
            @NotBlank(message = "Please enter the city or district.") @Size(max = 60) String city,
            @NotNull(message = "Please choose inside or outside Dhaka.") Zone zone,
            @Size(max = 200, message = "Notes can be up to 200 characters.") String note,
            Boolean makeDefault) {
    }

    record WishlistAdd(@NotNull Long productId) {
    }

    record Reordered(int added, List<String> unavailable, CartView cart) {
    }

    private final CustomerService customers;
    private final AddressService addresses;
    private final OrderService orders;
    private final ProductSearch search;
    private final CartService carts;
    private final Bags bags;
    private final LoginSessions sessions;
    private final JdbcClient jdbc;

    AccountController(CustomerService customers, AddressService addresses, OrderService orders, ProductSearch search,
                      CartService carts, Bags bags, LoginSessions sessions, JdbcClient jdbc) {
        this.customers = customers;
        this.addresses = addresses;
        this.orders = orders;
        this.search = search;
        this.carts = carts;
        this.bags = bags;
        this.sessions = sessions;
        this.jdbc = jdbc;
    }

    @GetMapping("/profile")
    Profile profile() {
        return customers.profile(CurrentCustomer.require());
    }

    @PutMapping("/profile")
    Profile updateProfile(@Valid @RequestBody ProfileUpdate body) {
        return customers.update(CurrentCustomer.require(), body.name().trim(), AuthController.blankToNull(body.email()));
    }

    /** Changing the password signs out every other device. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody PasswordChange body, HttpServletRequest request) {
        long id = CurrentCustomer.require();
        customers.changePassword(id, body.currentPassword(), body.newPassword());
        sessions.endOthers(id, request);
    }

    @GetMapping("/addresses")
    List<SavedAddress> addresses() {
        return addresses.list(CurrentCustomer.require());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    SavedAddress addAddress(@Valid @RequestBody AddressBody body) {
        return addresses.create(CurrentCustomer.require(), input(body), Boolean.TRUE.equals(body.makeDefault()));
    }

    @PutMapping("/addresses/{id}")
    SavedAddress updateAddress(@PathVariable long id, @Valid @RequestBody AddressBody body) {
        long customer = CurrentCustomer.require();
        SavedAddress saved = addresses.update(customer, id, input(body));
        return Boolean.TRUE.equals(body.makeDefault()) ? addresses.makeDefault(customer, id) : saved;
    }

    @PostMapping("/addresses/{id}/default")
    SavedAddress makeDefault(@PathVariable long id) {
        return addresses.makeDefault(CurrentCustomer.require(), id);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAddress(@PathVariable long id) {
        addresses.delete(CurrentCustomer.require(), id);
    }

    @GetMapping("/orders")
    History orders(@RequestParam(defaultValue = "0") int page) {
        return orders.history(CurrentCustomer.require(), Math.max(0, page), 10);
    }

    @GetMapping("/orders/{number}")
    OrderView order(@PathVariable String number) {
        return orders.forCustomer(CurrentCustomer.require(), number);
    }

    /**
     * Puts an earlier order's plants back in the bag, in the same size and planter. Each option is
     * raised to the earlier quantity (never lowered) within stock; sold-out ones are listed.
     */
    @PostMapping("/orders/{number}/reorder")
    Reordered reorder(@PathVariable String number, HttpServletRequest request) {
        long customer = CurrentCustomer.require();
        List<PastItem> past = orders.itemsForReorder(customer, number);
        Optional<UUID> bag = bags.current(request);
        int added = 0;
        List<String> unavailable = new ArrayList<>();
        for (PastItem item : past) {
            String label = item.name() + " (" + item.size() + ", " + item.planter() + ")";
            Integer cap = item.variantId() == null ? null : jdbc.sql("""
                            select least(v.stock, :max) from variants v join products p on p.id = v.product_id
                             where v.id = :id and p.status = 'ACTIVE'""")
                    .param("id", item.variantId()).param("max", CartService.MAX_PER_LINE)
                    .query(Integer.class).optional().orElse(null);
            if (cap == null || cap == 0) {
                unavailable.add(label);
                continue;
            }
            int inBag = bag.map(id -> carts.lines(id).stream().filter(l -> l.variantId() == item.variantId())
                    .mapToInt(CartService.Line::quantity).sum()).orElse(0);
            int wanted = Math.min(Math.max(inBag, item.quantity()), cap);
            if (wanted > inBag) {
                try {
                    bag = Optional.of(carts.add(bag, customer, item.variantId(), wanted - inBag));
                    added++;
                } catch (ApiException e) {
                    unavailable.add(label);
                }
            }
        }
        return new Reordered(added, unavailable, carts.view(bag.isPresent() ? bag : carts.ofCustomer(customer)));
    }

    /** Saved plants, newest first. */
    @GetMapping("/wishlist")
    List<ProductCard> wishlist() {
        long customer = CurrentCustomer.require();
        List<Long> ids = wishlistIds(customer);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductCard> cards = search.cards("p.id in (:ids)", Map.of("ids", ids), "p.id", ids.size()).stream()
                .collect(Collectors.toMap(ProductCard::id, Function.identity()));
        return ids.stream().filter(cards::containsKey).map(cards::get).toList();
    }

    /** Just the ids, so every product card can show whether it is saved. */
    @GetMapping("/wishlist/ids")
    List<Long> wishlistIds() {
        return wishlistIds(CurrentCustomer.require());
    }

    @PostMapping("/wishlist")
    List<Long> save(@Valid @RequestBody WishlistAdd body) {
        long customer = CurrentCustomer.require();
        try {
            jdbc.sql("insert into wishlist_items (customer_id, product_id) values (:c, :p) on conflict do nothing")
                    .param("c", customer).param("p", body.productId()).update();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.notFound("product");
        }
        return wishlistIds(customer);
    }

    @DeleteMapping("/wishlist/{productId}")
    List<Long> unsave(@PathVariable long productId) {
        long customer = CurrentCustomer.require();
        jdbc.sql("delete from wishlist_items where customer_id = :c and product_id = :p")
                .param("c", customer).param("p", productId).update();
        return wishlistIds(customer);
    }

    private List<Long> wishlistIds(long customer) {
        return jdbc.sql("""
                        select w.product_id from wishlist_items w join products p on p.id = w.product_id
                         where w.customer_id = :c and p.status = 'ACTIVE' order by w.created_at desc""")
                .param("c", customer).query(Long.class).list();
    }

    private static AddressInput input(AddressBody body) {
        String label = body.label() == null || body.label().isBlank() ? "Home" : body.label().trim();
        return new AddressInput(label, body.name().trim(), AuthController.phone(body.phone()), body.address().trim(),
                body.area().trim(), body.city().trim(), body.zone(), AuthController.blankToNull(body.note()));
    }
}
