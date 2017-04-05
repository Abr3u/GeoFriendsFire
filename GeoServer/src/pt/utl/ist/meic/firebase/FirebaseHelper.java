package pt.utl.ist.meic.firebase;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.DataPoint;
import pt.utl.ist.meic.domain.UserProfile;

public class FirebaseHelper {

	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";

	public static List<User> getUserListFromFirebase() throws FirebaseException, UnsupportedEncodingException {
		List<User> toReturn = new ArrayList<>();

		Firebase firebase = new Firebase(FIREBASE_URL + "/users");

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			toReturn.add(parseUser(x));
		});

		return toReturn;
	}

	private static User parseUser(Map.Entry<String, Object> x) {
		String userValues = x.getValue().toString();
		String[] aux = userValues.split(",");
		String username = aux[1].split("=")[1];
		username = username.substring(0, username.length() - 1);
		return new User(x.getKey(), username);
	}

	public static List<Event> getEventListFromFirebase() throws FirebaseException, UnsupportedEncodingException {
		List<Event> toReturn = new ArrayList<>();

		Firebase firebase = new Firebase(FIREBASE_URL + "/events");

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			toReturn.add(parseEvent(x));
		});

		return toReturn;
	}

	private static Event parseEvent(Entry<String, Object> x) {
		String eventValues = x.getValue().toString();

		String[] aux = eventValues.split(",");
		String authorId = aux[0].split("=")[1];
		String authorName = aux[1].split("=")[1];
		String categoryStr = aux[2].split("=")[1];
		String creationDate = aux[3].split("=")[1];
		String description = aux[4].split("=")[1];
		description = description.substring(0, description.length() - 1);

		EventCategory category = null;
		switch (categoryStr) {
		case "Shop":
			category = EventCategory.Shop;
			break;
		case "Food":
			category = EventCategory.Food;
			break;
		case "Sports":
			category = EventCategory.Sports;
			break;
		}
		return new Event(x.getKey(), authorId, authorName, category, creationDate, description);
	}

	public static List<CheckIn> getUserCheckIns(String userId) throws FirebaseException, UnsupportedEncodingException{
		List<CheckIn> toReturn = new ArrayList<>();

		Firebase firebase = new Firebase(FIREBASE_URL + "/locations/"+userId);

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			try {
				toReturn.add(parseCheckIn(x));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		});

		return toReturn;
	}

	private static CheckIn parseCheckIn(Entry<String, Object> x) throws ParseException {
		String checkInValues = x.getValue().toString();
		String[] aux = checkInValues.split(",");
		Double latitude = Double.parseDouble(aux[0].split("=")[1]);
		Double longitude = Double.parseDouble(aux[1].split("=")[1]);
		String time = aux[2].split("=")[1];
		time = time.substring(0, time.length() - 1);
		
		DateFormat df = CheckIn.getDateFormat();
		return new CheckIn(df.parse(time), new DataPoint(latitude, longitude));
	}

	public static void writeNewClustersFirebase(List<ClusterWithMean> globalClusters, int level, long totalCheckIns)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteClustersFirebase(level);

		Firebase firebase = new Firebase(FIREBASE_URL + "/clusters" + level);

		for (int i = 0; i < globalClusters.size(); i++) {
			// "PUT cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("mean", globalClusters.get(i).getmean().toString());
			dataMap.put("size", globalClusters.get(i).getVectors().size());
			dataMap.put("sizePerc", new Double(globalClusters.get(i).getVectors().size()) / totalCheckIns);

			FirebaseResponse response = firebase.put("cluster" + i, dataMap);
			System.out.println("\n\nResult of PUT cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}

	}

	private static void deleteClustersFirebase(int level) throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("clusters" + level);
		System.out.println(response.getBody().toString());
	}

	public static void writeNewFriendsFirebase(List<UserProfile> profiles, int limitProfiles, int limitSuggestions)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteFriendsFirebase();

		Firebase firebase = new Firebase(FIREBASE_URL + "/friends");
		for (UserProfile profile : profiles) {
			if (limitProfiles > 0) {
				// "POST firends to /friends
				Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
				profile.getSimilarities().entrySet().stream()
						.sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(limitSuggestions)
						.forEach(x -> dataMap.put(x.getKey(), x.getValue()));

				FirebaseResponse response = firebase.put("user" + profile.userId, dataMap);
				System.out.println("\n\nResult of PUT friends:\n" + response.getRawBody().toString());
				System.out.println("\n");
				limitProfiles--;
			} else {
				break;
			}
		}

	}

	private static void deleteFriendsFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("friends");
		System.out.println(response.getBody().toString());
	}

}
