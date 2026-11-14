package com.shobuj.store.notify;

/** Something that can deliver one text message to a Bangladeshi mobile number (01XXXXXXXXX). */
public interface SmsGateway {

    /** What the provider said: its message id when accepted, or why not. */
    record Result(boolean sent, String providerId, String error) {

        static Result ok(String id) {
            return new Result(true, id, null);
        }

        static Result failed(String error) {
            return new Result(false, null, error);
        }
    }

    String name();

    /** {@code reference} is our own id for the message, sent along so duplicates can be spotted. */
    Result send(String phone, String text, String reference);
}
