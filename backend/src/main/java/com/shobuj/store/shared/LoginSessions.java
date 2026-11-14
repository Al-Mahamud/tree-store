package com.shobuj.store.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Starts and ends server-side sessions (stored in PostgreSQL by Spring Session). */
@Component
public class LoginSessions {

    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    LoginSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** Signs in a customer ({@code customer:<id>}). */
    public void startCustomer(long customerId, HttpServletRequest request, HttpServletResponse response) {
        start(CurrentCustomer.PREFIX + customerId, "ROLE_CUSTOMER", request, response);
    }

    /** Signs in a member of staff ({@code staff:<id>}), who may use the admin area. */
    public void startStaff(long staffId, HttpServletRequest request, HttpServletResponse response) {
        start(CurrentStaff.PREFIX + staffId, "ROLE_ADMIN", request, response);
    }

    private void start(String principal, String role, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            // A new session id at sign-in prevents session fixation.
            request.changeSessionId();
        }
        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(role))));
        holder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    public void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        holder.clearContext();
    }

    /** Signs the customer out everywhere except this browser (after a password change). */
    public void endOthers(long customerId, HttpServletRequest request) {
        endOthers(CurrentCustomer.PREFIX + customerId, request);
    }

    /** Signs a customer out everywhere (after a password reset). */
    public void endAllCustomerSessions(long customerId) {
        sessions.findByPrincipalName(CurrentCustomer.PREFIX + customerId).keySet().forEach(sessions::deleteById);
    }

    /** Signs a member of staff out everywhere except this browser. */
    public void endOtherStaffSessions(long staffId, HttpServletRequest request) {
        endOthers(CurrentStaff.PREFIX + staffId, request);
    }

    /** Signs a member of staff out everywhere, e.g. when their access is removed. */
    public void endAllStaffSessions(long staffId) {
        sessions.findByPrincipalName(CurrentStaff.PREFIX + staffId).keySet().forEach(sessions::deleteById);
    }

    private void endOthers(String principal, HttpServletRequest request) {
        HttpSession current = request.getSession(false);
        String keep = current == null ? "" : current.getId();
        sessions.findByPrincipalName(principal).keySet().stream()
                .filter(id -> !id.equals(keep))
                .forEach(sessions::deleteById);
    }
}
