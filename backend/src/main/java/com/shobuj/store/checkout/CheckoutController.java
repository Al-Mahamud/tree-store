package com.shobuj.store.checkout;

import com.shobuj.store.checkout.CheckoutService.Customer;
import com.shobuj.store.checkout.CheckoutService.PaymentMethod;
import com.shobuj.store.checkout.CheckoutService.Quote;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.customer.AddressService;
import com.shobuj.store.shared.BangladeshPhone;
import com.shobuj.store.shared.CurrentCustomer;
import com.shobuj.store.shared.StoreProperties.Zone;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
class CheckoutController {

    record QuoteRequest(@NotNull Zone zone) {
    }

    record PlaceOrder(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter your mobile number.") String phone,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email,
            @NotBlank(message = "Please enter your address.") @Size(min = 5, max = 200, message = "Please enter your full address.") String address,
            @NotBlank(message = "Please enter your area or thana.") @Size(max = 80) String area,
            @NotBlank(message = "Please enter your city or district.") @Size(max = 60) String city,
            @NotNull(message = "Please choose a delivery area.") Zone zone,
            @Size(max = 500, message = "Notes can be up to 500 characters.") String note,
            @NotNull(message = "Please choose how to pay.") PaymentMethod paymentMethod,
            /* Signed-in customers: keep this address for next time. */
            Boolean saveAddress,
            /* Care texts after delivery; on unless this is false. */
            Boolean careTexts) {
    }

    record Placed(String number, String token, int total, String paymentUrl) {
    }

    private final CheckoutService checkout;
    private final Bags bags;
    private final AddressService addresses;
    private final com.shobuj.store.payment.PaymentService payments;
    private final com.shobuj.store.notify.CareTexts care;

    CheckoutController(CheckoutService checkout, Bags bags, AddressService addresses,
                       com.shobuj.store.payment.PaymentService payments, com.shobuj.store.notify.CareTexts care) {
        this.care = care;
        this.payments = payments;
        this.checkout = checkout;
        this.bags = bags;
        this.addresses = addresses;
    }

    @PostMapping("/quote")
    Quote quote(@Valid @RequestBody QuoteRequest body, HttpServletRequest request) {
        UUID cart = bags.current(request).orElseThrow(CartService::emptyCart);
        return checkout.quote(cart, body.zone());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Placed place(@Valid @RequestBody PlaceOrder body, HttpServletRequest request, HttpServletResponse response) {
        UUID cart = bags.current(request).orElseThrow(CartService::emptyCart);
        Optional<Long> customerId = CurrentCustomer.id();
        String phone = BangladeshPhone.normalise(body.phone()).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                Map.of("phone", "Enter an 11-digit mobile number, like 01712345678.")));
        if (body.paymentMethod() == PaymentMethod.ONLINE && !payments.onlineAvailable()) {
            throw ApiException.badRequest("payment.unavailable",
                    "Online payment isn't available right now. Please choose cash on delivery.");
        }
        Customer customer = new Customer(body.name().trim(), phone, blankToNull(body.email()), body.address().trim(),
                body.area().trim(), body.city().trim(), body.zone(), blankToNull(body.note()));
        CheckoutService.Placed placed = checkout.place(cart, customer, body.paymentMethod(), customerId.orElse(null));
        if (Boolean.FALSE.equals(body.careTexts())) {
            care.setOn(placed.orderId(), false);
        }
        if (customerId.isPresent() && Boolean.TRUE.equals(body.saveAddress())) {
            addresses.rememberFromCheckout(customerId.get(), new AddressService.AddressInput("Home", customer.name(), phone,
                    customer.address(), customer.area(), customer.city(), customer.zone(), null));
        }
        bags.forget(request, response);
        String paymentUrl = null;
        if (body.paymentMethod() == PaymentMethod.ONLINE) {
            try {
                paymentUrl = payments.start(placed.orderId());
            } catch (ApiException e) {
                // The order is saved and waits for payment; its page offers to try again.
            }
        }
        return new Placed(placed.number(), placed.accessToken(), placed.total(), paymentUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
