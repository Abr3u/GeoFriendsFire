package pt.utl.ist.meic.geofriendsfire.network;

import pt.utl.ist.meic.geofriendsfire.models.IpAddressResponse;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by ricar on 13/04/2017.
 */

public interface IdentMeEndpoints {
    @GET("/.json")
    Call<IpAddressResponse> getIpAddress();
}
