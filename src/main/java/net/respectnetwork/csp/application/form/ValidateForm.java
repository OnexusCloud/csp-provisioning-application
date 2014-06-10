package net.respectnetwork.csp.application.form;

import com.google.common.base.Objects;

/**
 * Class for Data used in Confirmation and Payments Form
 */
public class ValidateForm {

    /** Email Code */
    private String emailCode;

    /** SMS Code */
    private String smsCode;

    /** Terms */
    private String terms;

    /** Reset password */
    private boolean resetPwd;

    /**
     * @return the emailCode
     */
    public String getEmailCode() {
        return emailCode;
    }

    /**
     * @param emailCode the emailCode to set
     */
    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    /**
     * @return the smsCode
     */
    public String getSmsCode() {
        return smsCode;
    }

    /**
     * @param smsCode the smsCode to set
     */
    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
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

    public boolean isResetPwd() {
        return resetPwd;
    }

    public void setResetPwd(boolean resetPwd) {
        this.resetPwd = resetPwd;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("emailCode", emailCode)
                .add("smsCode", smsCode)
                .add("terms", terms)
                .add("resetPwd", resetPwd)
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

        ValidateForm that = (ValidateForm) o;

        return Objects.equal(this.emailCode, that.emailCode) &&
                Objects.equal(this.smsCode, that.smsCode) &&
                Objects.equal(this.terms, that.terms) &&
                Objects.equal(this.resetPwd, that.resetPwd);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(emailCode, smsCode, terms, resetPwd);
    }
}
