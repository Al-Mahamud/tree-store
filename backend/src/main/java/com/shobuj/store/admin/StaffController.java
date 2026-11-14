package com.shobuj.store.admin;

import com.shobuj.store.admin.StaffService.Staff;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.BangladeshPhone;
import com.shobuj.store.shared.CurrentStaff;
import com.shobuj.store.shared.LoginSessions;
import com.shobuj.store.shared.SignInLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff sign-in for the admin area. Signing in as staff replaces any customer sign-in in this browser. */
@RestController
@RequestMapping("/api/v1/staff")
class StaffController {

    record SignIn(@NotBlank(message = "Please enter your mobile number.") String phone,
                  @NotBlank(message = "Please enter your password.") String password) {
    }

    record Me(Staff staff) {
    }

    private final StaffService staff;
    private final LoginSessions sessions;
    private final SignInLimiter limiter;

    StaffController(StaffService staff, LoginSessions sessions, SignInLimiter limiter) {
        this.staff = staff;
        this.sessions = sessions;
        this.limiter = limiter;
    }

    @GetMapping("/me")
    Me me() {
        return new Me(CurrentStaff.id().flatMap(staff::find).orElse(null));
    }

    @PostMapping("/login")
    Me login(@Valid @RequestBody SignIn body, HttpServletRequest request, HttpServletResponse response) {
        String phone = BangladeshPhone.normalise(body.phone()).orElse(body.phone().trim());
        String key = "staff:" + phone;
        String address = "staff:" + request.getRemoteAddr();
        limiter.check(key, address);
        long id = staff.authenticate(phone, body.password()).orElseThrow(() -> {
            limiter.failed(key, address);
            return new ApiException(HttpStatus.UNAUTHORIZED, "auth.bad_credentials",
                    "That mobile number and password don't match a staff account.");
        });
        limiter.succeeded(key);
        sessions.startStaff(id, request, response);
        return new Me(staff.require(id));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        sessions.end(request);
    }
}
