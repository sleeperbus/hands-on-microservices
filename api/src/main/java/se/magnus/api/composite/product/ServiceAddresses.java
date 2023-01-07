package se.magnus.api.composite.product;

import lombok.Getter;

@Getter
public class ServiceAddresses {
    private final String cmp;
    private final String pro;
    private final String rev;
    private final String rec;

    public ServiceAddresses(String compositeAddress, String productAddress, String reviewAddress, String recommendationAddress) {
        this.cmp = compositeAddress;
        this.pro = productAddress;
        this.rev = reviewAddress;
        this.rec = recommendationAddress;
    }

    public ServiceAddresses() {
        this.cmp = null;
        this.pro = null;
        this.rev = null;
        this.rec = null;
    }
}
