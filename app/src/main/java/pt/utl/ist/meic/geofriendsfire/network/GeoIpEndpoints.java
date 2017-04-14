package pt.utl.ist.meic.geofriendsfire.network;

import pt.utl.ist.meic.geofriendsfire.models.GeoIpResponse;
import pt.utl.ist.meic.geofriendsfire.models.IpAddressResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by ricar on 13/04/2017.
 */

public interface GeoIpEndpoints {
    @GET("/json/{ip}")
    Call<GeoIpResponse> getLocationInfoFromIp(@Path("ip") String ip);
}
