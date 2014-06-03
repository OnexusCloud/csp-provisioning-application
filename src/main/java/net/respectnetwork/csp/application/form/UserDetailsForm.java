package net.respectnetwork.csp.application.form;

import com.google.common.base.Objects;

public class UserDetailsForm {

    /** eMail */
    private String email;

    /** Country Code */
    private String cc;

    /** Phone */
    private String mobilePhone;

    /** Password */
    private String password;

    /** Confirm Password */
    private String confirmPassword;

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the country code
     */
    public String getCc() {
        return cc;
    }

    /**
     * @param cc the country code to set
     */
    public void setCc(String cc) {
        if (cc != null) {
            cc = cc.replace(" ", ""); // remove any formatting spaces
        }
        this.cc = cc;
    }

    /**
     * @return the regional mobile number
     */
    public String getMobilePhone() {
        return mobilePhone;
    }

    /**
     * @param mobilePhone the region mobile phone to set
     */
    public void setMobilePhone(String mobilePhone) {
        if (mobilePhone != null) {
            mobilePhone = mobilePhone.replace(" ", ""); // remove any formatting spaces
        }
        this.mobilePhone = mobilePhone;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the international format phone
     */
    public String getPhone() {
        return cc + "." + mobilePhone;
    }

    /**
     * @return the confirm password
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * @param confirmPassword the confirm password to set
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("email", email)
                .add("cc", cc)
                .add("mobilePhone", mobilePhone)
//                .add("password", password)
//                .add("confirmPassword", confirmPassword)
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

        UserDetailsForm that = (UserDetailsForm) o;

        return Objects.equal(this.email, that.email) &&
                Objects.equal(this.cc, that.cc) &&
                Objects.equal(this.mobilePhone, that.mobilePhone) &&
                Objects.equal(this.password, that.password) &&
                Objects.equal(this.confirmPassword, that.confirmPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email, cc, mobilePhone, password, confirmPassword);
    }
}
