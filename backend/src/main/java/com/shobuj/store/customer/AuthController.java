package com.shobuj.store.customer;

import com.shobuj.store.checkout.Bags;
import com.shobuj.store.customer.CustomerService.Profile;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.BangladeshPhone;
import com.shobuj.store.shared.CurrentCustomer;
import com.shobuj.store.shared.LoginSessions;
import com.shobuj.store.shared.SignInLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create an account, sign in and sign out. The bag filled as a guest moves into the account. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    record Register(
            @NotBlank(message = "Please enter your name.") @Size(max = 80, message = "Name is too long.") String name,
            @NotBlank(message = "Please enter your mobile number.") String phone,
            @Email(message = "That email doesn't look right.") @Size(max = 120) String email,
            @NotBlank(message = "Please choose a password.")
            @Size(min = 8, max = 72, message = "Use at least 8 characters.") String password,
            @NotBlank(message = "Enter the 6-digit code we texted you.") String code) {
    }

    record CodeRequest(@NotBlank(message = "Please enter your mobile number.") String phone,
                       @NotNull PhoneCodes.Purpose purpose) {
    }

    record Reset(@NotBlank(message = "Please enter your mobile number.") String phone,
                 @NotBlank(message = "Enter the 6-digit code we texted you.") String code,
                 @NotBlank(message = "Please choose a new password.")
                 @Size(min = 8, max = 72, message = "Use at least 8 characters.") String newPassword) {
    }

    record SignIn(@NotBlank(message = "Please enter your mobile number.") String phone,
                  @NotBlank(message = "Please enter your password.") String password) {
    }

    /** {@code linkedOrders}: earlier guest orders that joined a new account. */
    record Me(Profile customer, Integer linkedOrders) {

        Me(Profile customer) {
            this(customer, null);
        }
    }

    private final CustomerService customers;
    private final LoginSessions sessions;
    private final SignInLimiter limiter;
    private final Bags bags;
    private final PhoneCodes codes;

    AuthController(CustomerService customers, LoginSessions sessions, SignInLimiter limiter, Bags bags,
                   PhoneCodes codes) {
        this.codes = codes;
        this.customers = customers;
        this.sessions = sessions;
        this.limiter = limiter;
        this.bags = bags;
    }

    @GetMapping("/me")
    Me me() {
        return new Me(CurrentCustomer.id().flatMap(customers::find).orElse(null));
    }

    /** Texts a code to prove the number: before creating an account, or to reset a password. */
    @PostMapping("/codes")
    PhoneCodes.Sent sendCode(@Valid @RequestBody CodeRequest body, HttpServletRequest request) {
        String phone = phone(body.phone());
        boolean hasAccount = customers.exists(phone);
        if (body.purpose() == PhoneCodes.Purpose.REGISTER && hasAccount) {
            throw taken();
        }
        // For a reset, the answer is the same whether or not the number has an account.
        return codes.issue(phone, body.purpose(), request.getRemoteAddr(),
                body.purpose() == PhoneCodes.Purpose.REGISTER || hasAccount);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Me register(@Valid @RequestBody Register body, HttpServletRequest request, HttpServletResponse response) {
        String phone = phone(body.phone());
        if (customers.exists(phone)) {
            throw taken();
        }
        codes.verify(phone, PhoneCodes.Purpose.REGISTER, body.code());
        Profile profile = customers.register(body.name().trim(), phone, blankToNull(body.email()), body.password());
        int linked = customers.linkGuestOrders(profile.id(), phone);
        signIn(profile.id(), request, response);
        return new Me(profile, linked);
    }

    /** Forgot password: the texted code proves the number; every other device is signed out. */
    @PostMapping("/password-reset")
    Me resetPassword(@Valid @RequestBody Reset body, HttpServletRequest request, HttpServletResponse response) {
        String phone = phone(body.phone());
        codes.verify(phone, PhoneCodes.Purpose.RESET, body.code());
        long id = customers.resetPassword(phone, body.newPassword()).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "code.invalid", "Please check the highlighted fields.",
                Map.of("code", "That code isn't right or has expired. Ask for a new one.")));
        limiter.succeeded(phone);
        sessions.endAllCustomerSessions(id);
        signIn(id, request, response);
        return new Me(customers.profile(id));
    }

    private static ApiException taken() {
        return new ApiException(HttpStatus.CONFLICT, "auth.phone_taken",
                "An account with this mobile number already exists. Please sign in instead.",
                Map.of("phone", "This number already has an account."));
    }

    @PostMapping("/login")
    Me login(@Valid @RequestBody SignIn body, HttpServletRequest request, HttpServletResponse response) {
        String phone = BangladeshPhone.normalise(body.phone()).orElse(body.phone().trim());
        String address = request.getRemoteAddr();
        limiter.check(phone, address);
        long id = customers.authenticate(phone, body.password()).orElseThrow(() -> {
            limiter.failed(phone, address);
            return new ApiException(HttpStatus.UNAUTHORIZED, "auth.bad_credentials",
                    "That mobile number and password don't match.");
        });
        limiter.succeeded(phone);
        signIn(id, request, response);
        return new Me(customers.profile(id));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.end(request);
        bags.forget(request, response);
    }

    private void signIn(long customerId, HttpServletRequest request, HttpServletResponse response) {
        sessions.startCustomer(customerId, request, response);
        bags.claim(customerId, request, response);
    }

    static String phone(String input) {
        return BangladeshPhone.normalise(input).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                "request.invalid", "Please check the highlighted fields.",
                Map.of("phone", "Enter an 11-digit mobile number, like 01712345678.")));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
