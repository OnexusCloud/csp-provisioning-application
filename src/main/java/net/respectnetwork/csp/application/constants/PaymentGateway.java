package net.respectnetwork.csp.application.constants;

import net.respectnetwork.csp.application.model.CSPModel;

public enum PaymentGateway {

    STRIPE("STRIPE"),
    SAGE_PAY("SAGEPAY"),
    BRAIN_TREE("BRAINTREE"),
    PIN_NET_AU("PIN_NET_AU"),
    GIFT_CODE_ONLY("GIFT_CODE_ONLY");

    private final String modelString;

    private PaymentGateway(String modelString) {
        this.modelString = modelString;
    }

    public boolean is(CSPModel cspModel) {
        return (cspModel != null && modelString.equals(cspModel.getPaymentGatewayName()));
    }
}
