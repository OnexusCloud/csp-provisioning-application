package net.respectnetwork.csp.application.form;

import com.google.common.base.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;

public class LoginForm {

    /** cloud name */
    private String cloudName;

    /** password */
    private String password;

    /** redirect */
    private String redirect;

    public LoginForm() {
    }

    public LoginForm(String redirect) {
        this.redirect = redirect;
    }

    public LoginForm(LoginForm that) {
        this.redirect = that.redirect;
        this.cloudName = that.cloudName;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public boolean hasRedirect() {
        return !isNullOrEmpty(redirect);
    }

    /** For backwards comparability - remove me eventually */
    public String getSecrettoken() {
        return password;
    }

    /** For backwards comparability - remove me eventually */
    public void setSecrettoken(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("cloudName", cloudName)
                .add("redirect", redirect)
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

        LoginForm that = (LoginForm) o;

        return Objects.equal(this.cloudName, that.cloudName) &&
                Objects.equal(this.password, that.password) &&
                Objects.equal(this.redirect, that.redirect);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cloudName, password, redirect);
    }
}
