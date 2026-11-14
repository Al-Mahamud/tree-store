package com.shobuj.store.admin;

import com.shobuj.store.shared.Pictures;
import com.shobuj.store.admin.AdminCatalog.ProductInput;
import com.shobuj.store.admin.AdminSettings.CollectionInput;
import com.shobuj.store.admin.AdminSettings.CouponInput;
import com.shobuj.store.checkout.DeliveryFees;
import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.BangladeshPhone;
import com.shobuj.store.review.ReviewService;
import com.shobuj.store.shared.CurrentStaff;
import com.shobuj.store.shared.LoginSessions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The admin area's API. Security lets only signed-in staff in; {@link AdminGuard} checks the account is still active. */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    record StatusChange(@NotBlank String status, @Size(max = 300) String note, Boolean restock,
                        @Size(max = 60) String courier, @Size(max = 60) String trackingCode) {
    }

    record Refund(@NotBlank(message = "Enter the refund reference from SSLCommerz.") @Size(max = 80) String reference) {
    }

    record Note(@NotBlank(message = "Write a note.") @Size(max = 300) String note) {
    }

    record StockChange(@NotNull Integer stock) {
    }

    record ImageChange(Long planterId, @Size(max = 160) String alt) {
    }

    record Fees(Integer insideDhaka, Integer outsideDhaka, Integer freeOver) {
    }

    record NewStaff(@NotBlank(message = "Enter their name.") @Size(max = 60, message = "Name is too long.") String name,
                    @NotBlank(message = "Enter their mobile number.") String phone,
                    @NotBlank(message = "Choose a starting password.")
                    @Size(min = 10, max = 72, message = "Use at least 10 characters.") String password) {
    }

    record Activation(@NotNull Boolean active) {
    }

    record NewPassword(@NotBlank(message = "Choose a password.")
                       @Size(min = 10, max = 72, message = "Use at least 10 characters.") String password) {
    }

    record OwnPassword(@NotBlank(message = "Enter your current password.") String currentPassword,
                       @NotBlank(message = "Choose a new password.")
                       @Size(min = 10, max = 72, message = "Use at least 10 characters.") String newPassword) {
    }

    record Move(int direction) {
    }

    /** Either may be left out: {@code status} PUBLISHED or HIDDEN; an empty {@code reply} removes it. */
    record ReviewChange(String status, String reply) {
    }

    private final AdminOrders orders;
    private final AdminCatalog catalog;
    private final AdminSettings settings;
    private final AdminCategories categories;
    private final DeliveryFees fees;
    private final StaffService staff;
    private final LoginSessions sessions;
    private final ReviewService reviews;

    AdminController(AdminOrders orders, AdminCatalog catalog, AdminSettings settings, AdminCategories categories,
                    DeliveryFees fees, StaffService staff, LoginSessions sessions, ReviewService reviews) {
        this.reviews = reviews;
        this.orders = orders;
        this.catalog = catalog;
        this.settings = settings;
        this.categories = categories;
        this.fees = fees;
        this.staff = staff;
        this.sessions = sessions;
    }

    private String actor() {
        return staff.actor(CurrentStaff.require());
    }

    @GetMapping("/dashboard")
    AdminOrders.Dashboard dashboard() {
        return orders.dashboard();
    }

    // Orders

    @GetMapping("/orders")
    AdminOrders.Page orders(@RequestParam(defaultValue = "OPEN") String filter, @RequestParam(required = false) String zone,
                            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
        return orders.list(filter, zone, q, Math.max(0, page));
    }

    @GetMapping("/orders/{number}")
    AdminOrders.Detail order(@PathVariable String number) {
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/status")
    AdminOrders.Detail changeStatus(@PathVariable String number, @Valid @RequestBody StatusChange body) {
        orders.changeStatus(number, body.status(), body.note(), body.restock() == null || body.restock(), body.courier(),
                body.trackingCode(), actor());
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/refund")
    AdminOrders.Detail refund(@PathVariable String number, @Valid @RequestBody Refund body) {
        orders.refund(number, body.reference().trim(), actor());
        return orders.detail(number);
    }

    @PostMapping("/orders/{number}/notes")
    AdminOrders.Detail note(@PathVariable String number, @Valid @RequestBody Note body) {
        orders.addNote(number, body.note(), actor());
        return orders.detail(number);
    }

    // Plants, pictures and stock

    @GetMapping("/products")
    AdminCatalog.ProductPage products(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) Long category,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "false") boolean low,
                                      @RequestParam(defaultValue = "0") int page) {
        return catalog.products(q, category, status, low, Math.max(0, page));
    }

    @GetMapping("/products/{id}")
    AdminCatalog.ProductView product(@PathVariable long id) {
        return catalog.product(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    AdminCatalog.ProductView createProduct(@RequestBody ProductInput body) {
        return catalog.save(null, body);
    }

    @PutMapping("/products/{id}")
    AdminCatalog.ProductView updateProduct(@PathVariable long id, @RequestBody ProductInput body) {
        return catalog.save(id, body);
    }

    @PostMapping("/products/{id}/images")
    AdminCatalog.ProductView addImage(@PathVariable long id, @RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false) Long planterId,
                                      @RequestParam(required = false) String alt) throws IOException {
        return catalog.addImage(id, planterId, alt, bytes(file));
    }

    @PatchMapping("/products/{id}/images/{imageId}")
    AdminCatalog.ProductView updateImage(@PathVariable long id, @PathVariable long imageId,
                                         @Valid @RequestBody ImageChange body) {
        return catalog.updateImage(id, imageId, body.planterId(), body.alt());
    }

    @PostMapping("/products/{id}/images/{imageId}/move")
    AdminCatalog.ProductView moveImage(@PathVariable long id, @PathVariable long imageId, @RequestBody Move body) {
        return catalog.moveImage(id, imageId, body.direction());
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    AdminCatalog.ProductView deleteImage(@PathVariable long id, @PathVariable long imageId) {
        return catalog.deleteImage(id, imageId);
    }

    @GetMapping("/stock")
    List<AdminCatalog.StockRow> stock(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean low) {
        return catalog.stock(q, low);
    }

    @PatchMapping("/variants/{id}/stock")
    AdminCatalog.StockRow setStock(@PathVariable long id, @Valid @RequestBody StockChange body) {
        return catalog.setStock(id, body.stock());
    }

    // Categories and collections

    @GetMapping("/categories")
    List<AdminCategories.Node> categories() {
        return categories.tree();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    List<AdminCategories.Node> createCategory(@RequestBody AdminCategories.Input body) {
        return categories.create(body);
    }

    @PutMapping("/categories/{id}")
    List<AdminCategories.Node> updateCategory(@PathVariable long id, @RequestBody AdminCategories.Input body) {
        return categories.update(id, body);
    }

    @PostMapping("/categories/{id}/move")
    List<AdminCategories.Node> moveCategory(@PathVariable long id, @RequestBody Move body) {
        return categories.move(id, body.direction());
    }

    @PostMapping("/categories/{id}/image")
    List<AdminCategories.Node> categoryImage(@PathVariable long id, @RequestPart("file") MultipartFile file) throws IOException {
        return categories.setImage(id, bytes(file));
    }

    @DeleteMapping("/categories/{id}")
    List<AdminCategories.Node> deleteCategory(@PathVariable long id) {
        return categories.delete(id);
    }

    @GetMapping("/collections")
    List<AdminSettings.CollectionView> collections() {
        return settings.collections();
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    List<AdminSettings.CollectionView> createCollection(@RequestBody CollectionInput body) {
        return settings.saveCollection(null, body);
    }

    @PutMapping("/collections/{id}")
    List<AdminSettings.CollectionView> updateCollection(@PathVariable long id, @RequestBody CollectionInput body) {
        return settings.saveCollection(id, body);
    }

    @PostMapping("/collections/{id}/move")
    List<AdminSettings.CollectionView> moveCollection(@PathVariable long id, @RequestBody Move body) {
        return settings.moveCollection(id, body.direction());
    }

    @PostMapping("/collections/{id}/image")
    List<AdminSettings.CollectionView> collectionImage(@PathVariable long id, @RequestPart("file") MultipartFile file)
            throws IOException {
        return settings.setCollectionImage(id, bytes(file));
    }

    // Coupons and delivery

    @GetMapping("/coupons")
    List<AdminSettings.CouponView> coupons() {
        return settings.coupons();
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    AdminSettings.CouponView createCoupon(@RequestBody CouponInput body) {
        return settings.saveCoupon(null, body);
    }

    @PutMapping("/coupons/{id}")
    AdminSettings.CouponView updateCoupon(@PathVariable long id, @RequestBody CouponInput body) {
        return settings.saveCoupon(id, body);
    }

    @GetMapping("/delivery")
    DeliveryFees.Fees delivery() {
        return fees.current();
    }

    @PutMapping("/delivery")
    DeliveryFees.Fees setDelivery(@RequestBody Fees body) {
        Map<String, String> errors = new LinkedHashMap<>();
        fee(errors, "insideDhaka", body.insideDhaka(), 5000);
        fee(errors, "outsideDhaka", body.outsideDhaka(), 5000);
        fee(errors, "freeOver", body.freeOver(), 1_000_000);
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.", errors);
        }
        return fees.save(body.insideDhaka(), body.outsideDhaka(), body.freeOver(), actor());
    }

    private static void fee(Map<String, String> errors, String field, Integer value, int max) {
        if (value == null || value < 0 || value > max) {
            errors.put(field, "Enter an amount from ৳0 to ৳" + String.format("%,d", max) + ".");
        }
    }

    // Reviews

    @GetMapping("/reviews")
    ReviewService.AdminPage reviews(@RequestParam(required = false) String filter, @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page) {
        return reviews.adminList(filter, q, page);
    }

    @PatchMapping("/reviews/{id}")
    void changeReview(@PathVariable long id, @RequestBody ReviewChange body) {
        reviews.moderate(id, body.status(), body.reply(), actor());
    }

    // Texts

    @GetMapping("/texts")
    AdminSettings.TextPage texts(@RequestParam(required = false) String status, @RequestParam(required = false) String phone,
                                 @RequestParam(defaultValue = "0") int page) {
        return settings.texts(status, phone, Math.max(0, page));
    }

    @PostMapping("/texts/{id}/retry")
    AdminSettings.TextPage retryText(@PathVariable long id) {
        settings.retryText(id);
        return settings.texts(null, null, 0);
    }

    // Staff

    @GetMapping("/staff")
    List<StaffService.StaffRow> staffList() {
        return staff.list();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    List<StaffService.StaffRow> addStaff(@Valid @RequestBody NewStaff body) {
        String phone = BangladeshPhone.normalise(body.phone()).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "request.invalid", "Please check the highlighted fields.",
                Map.of("phone", "Enter an 11-digit mobile number, like 01712345678.")));
        staff.add(body.name().trim(), phone, body.password());
        return staff.list();
    }

    /** Switching someone off also signs them out everywhere at once. */
    @PostMapping("/staff/{id}/active")
    List<StaffService.StaffRow> setActive(@PathVariable long id, @Valid @RequestBody Activation body) {
        staff.setActive(id, body.active(), CurrentStaff.require());
        if (!body.active()) {
            sessions.endAllStaffSessions(id);
        }
        return staff.list();
    }

    /** A new password for someone else (e.g. they forgot theirs); they are signed out everywhere. */
    @PostMapping("/staff/{id}/password")
    List<StaffService.StaffRow> resetStaffPassword(@PathVariable long id, @Valid @RequestBody NewPassword body) {
        if (id == CurrentStaff.require()) {
            throw ApiException.conflict("staff.self", "Change your own password under My password.");
        }
        staff.setPassword(id, body.password());
        sessions.endAllStaffSessions(id);
        return staff.list();
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeOwnPassword(@Valid @RequestBody OwnPassword body, HttpServletRequest request) {
        long me = CurrentStaff.require();
        staff.changeOwnPassword(me, body.currentPassword(), body.newPassword());
        sessions.endOtherStaffSessions(me, request);
    }

    private static byte[] bytes(MultipartFile file) throws IOException {
        if (file.getSize() > Pictures.MAX_BYTES) {
            throw ApiException.badRequest("image.size", "Pictures can be up to 5 MB.");
        }
        return file.getBytes();
    }
}
