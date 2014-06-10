package net.respectnetwork.csp.application.types;

import java.beans.PropertyEditorSupport;

public enum PaymentType {
    GiftCode,
    PromoCode,
    CreditCard;

    private static final PaymentType[] values = values();

    public static class Converter extends PropertyEditorSupport {
        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            PaymentType value = null;
            for (PaymentType paymentType : values) {
                if (paymentType.toString().equalsIgnoreCase(text)) {
                    value = paymentType;
                    break;
                }
            }

            setValue(value);
        }
    }
}
