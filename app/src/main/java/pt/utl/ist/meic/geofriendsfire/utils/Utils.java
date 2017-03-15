package pt.utl.ist.meic.geofriendsfire.utils;


import android.location.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class Utils {

    private static double one_lati_in_km = 110.575;
    private static double one_longi_in_km = 111.303;


    //radius in km
    public static Map<String,Double> getBoundingBox(Location location, double radius){
        double latiRatio = radius / one_lati_in_km;
        double longiRatio = radius / one_longi_in_km;

        HashMap box = new HashMap<String,Double>();
        box.put("bot",location.getLatitude()-latiRatio);
        box. put("top",location.getLatitude()+latiRatio);
        box.put("left",location.getLongitude()-longiRatio);
        box.put("right",location.getLongitude()+longiRatio);

        return box;
    }

    /*
 * Calculate distance between two points in latitude and longitude.
 * Uses Haversine method as its base.
 *
 * lat1, lon1 Start point; lat2, lon2 End point
 * @returns Distance in Meters
 */
    public static double distance(double lat1, double lat2, double lon1,double lon2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }


    public static class ObservableList<T> {

        public final List<T> list;
        private final PublishSubject<T> onAdd;

        public ObservableList() {
            this.list = new ArrayList<T>();
            this.onAdd = PublishSubject.create();
        }
        public void add(T value) {
            list.add(value);
            onAdd.onNext(value);
        }

        public Observable<T> getObservable() {
            return onAdd;
        }
    }
}
