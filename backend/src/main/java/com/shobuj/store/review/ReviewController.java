package com.shobuj.store.review;

import com.shobuj.store.shared.ApiException;
import com.shobuj.store.shared.CurrentCustomer;
import com.shobuj.store.shared.Pictures;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Reviews on product pages, and a customer's own reviews (under /account, which needs signing in). */
@RestController
class ReviewController {

    private final ReviewService reviews;

    ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/api/v1/products/{slug}/reviews")
    ReviewService.Page page(@PathVariable String slug, @RequestParam(required = false) String sort,
                            @RequestParam(defaultValue = "0") int page) {
        return reviews.page(slug, sort, page);
    }

    @GetMapping("/api/v1/account/reviews")
    ReviewService.AccountReviews mine() {
        return reviews.account(CurrentCustomer.require());
    }

    @GetMapping("/api/v1/account/reviews/for/{slug}")
    ReviewService.ForProduct forProduct(@PathVariable String slug) {
        return reviews.forProduct(CurrentCustomer.require(), slug);
    }

    @PutMapping("/api/v1/account/reviews/for/{slug}")
    ReviewService.Mine save(@PathVariable String slug, @RequestBody ReviewService.Input body) {
        return reviews.save(CurrentCustomer.require(), slug, body);
    }

    @DeleteMapping("/api/v1/account/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        reviews.delete(CurrentCustomer.require(), id);
    }

    @PostMapping("/api/v1/account/reviews/{id}/photos")
    ReviewService.Mine addPhoto(@PathVariable long id, @RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > Pictures.MAX_BYTES) {
            throw ApiException.badRequest("image.size", "Photos can be up to 5 MB.");
        }
        return reviews.addPhoto(CurrentCustomer.require(), id, file.getBytes());
    }

    @DeleteMapping("/api/v1/account/reviews/{id}/photos/{photoId}")
    ReviewService.Mine deletePhoto(@PathVariable long id, @PathVariable long photoId) {
        return reviews.deletePhoto(CurrentCustomer.require(), id, photoId);
    }
}
