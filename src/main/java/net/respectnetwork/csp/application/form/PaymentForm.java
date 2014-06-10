package net.respectnetwork.csp.application.form;

import com.google.common.base.Objects;
import net.respectnetwork.csp.application.types.PaymentType;

/**
 * Class for Data used in Confirmation and Payments Form
 */
public class PaymentForm {

    public final static String TXN_TYPE_SIGNUP = "signup";
    public final static String TXN_TYPE_BUY_GC = "buyGiftCard";
    public final static String TXN_TYPE_DEP = "buyDependentCloud";

    /** signup or buyGiftCard or buyDependentCloud */
    private String txnType;

    /** number of clouds being purchased */
    private int numberOfClouds;

    private String giftCodes;
    private boolean giftCodesOnly;

    private String terms;
    private PaymentType paymentType;

    public PaymentForm() {
        this.giftCodes = "";
    }

    public PaymentForm(PaymentForm paymentFormIn) {
        this.giftCodes = paymentFormIn.giftCodes;
        this.txnType = paymentFormIn.txnType;
        this.numberOfClouds = paymentFormIn.numberOfClouds;
        this.giftCodesOnly = paymentFormIn.giftCodesOnly;
        this.terms = paymentFormIn.terms;
        this.paymentType = paymentFormIn.paymentType;
    }

    public String getGiftCodes() {
        return giftCodes;
    }

    public void setGiftCodes(String giftCodes) {
        this.giftCodes = giftCodes;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public int getNumberOfClouds() {
        return numberOfClouds;
    }

    public void setNumberOfClouds(int numberOfClouds) {
        this.numberOfClouds = numberOfClouds;
    }

    public boolean isGiftCodesOnly() {
        return giftCodesOnly;
    }

    public void setGiftCodesOnly(boolean giftCodesOnly) {
        this.giftCodesOnly = giftCodesOnly;
    }

    public boolean isTermsChecked() {
        return "on".equalsIgnoreCase(terms);
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public boolean isByGiftCode() {
        return (paymentType == PaymentType.GiftCode);
    }

    public boolean isByCreditCard() {
        return (paymentType == PaymentType.CreditCard);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("giftCodes", giftCodes)
                .add("txnType", txnType)
                .add("numberOfClouds", numberOfClouds)
                .add("giftCodesOnly", giftCodesOnly)
                .add("terms", terms)
                .add("paymentType", paymentType)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PaymentForm that = (PaymentForm) o;

        return Objects.equal(this.giftCodes, that.giftCodes) &&
                Objects.equal(this.txnType, that.txnType) &&
                Objects.equal(this.numberOfClouds, that.numberOfClouds) &&
                Objects.equal(this.giftCodesOnly, that.giftCodesOnly) &&
                Objects.equal(this.terms, that.terms) &&
                Objects.equal(this.paymentType, that.paymentType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(giftCodes, txnType, numberOfClouds, giftCodesOnly, terms, paymentType);
    }
}
