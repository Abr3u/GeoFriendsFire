package pt.utl.ist.meic.geofriendsfire.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by ricar on 14/04/2017.
 */

public class IpAddressResponse {

    @SerializedName("address")
    @Expose
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
