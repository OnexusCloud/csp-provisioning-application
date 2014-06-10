package net.respectnetwork.csp.application.form;

import com.google.common.base.Objects;

public class SignUpForm {

    private String cloudName;

    /** invite code */
    private String inviteCode;

    /** gift code */
    private String giftCode;

    private String nameAvailabilityCheckURL;

    public SignUpForm() {
    }

    public SignUpForm(String cloudName) {
        this.cloudName = cloudName;
    }

    /**
     * @return the cloudName
     */
    public String getCloudName() {
        return cloudName;
    }

    /**
     * @param cloudName the cloudName to set
     */
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getGiftCode() {
        return giftCode;
    }

    public void setGiftCode(String giftCode) {
        this.giftCode = giftCode;
    }

    public void setNameAvailabilityCheckURL(String url) {
        this.nameAvailabilityCheckURL = url;
    }

    public String getNameAvailabilityCheckURL() {
        return this.nameAvailabilityCheckURL;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("cloudName", cloudName)
                .add("inviteCode", inviteCode)
                .add("giftCode", giftCode)
                .add("nameAvailabilityCheckURL", nameAvailabilityCheckURL)
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

        SignUpForm that = (SignUpForm) o;

        return Objects.equal(this.cloudName, that.cloudName) &&
                Objects.equal(this.inviteCode, that.inviteCode) &&
                Objects.equal(this.giftCode, that.giftCode) &&
                Objects.equal(this.nameAvailabilityCheckURL, that.nameAvailabilityCheckURL);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cloudName, inviteCode, giftCode, nameAvailabilityCheckURL);
    }
}
