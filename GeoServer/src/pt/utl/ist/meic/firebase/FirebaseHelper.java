package pt.utl.ist.meic.firebase;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.SynchronousQueue;
import java.util.Random;

import org.codehaus.jackson.map.introspect.BasicClassIntrospector.GetterMethodFilter;

import ca.pfv.spmf.patterns.cluster.Cluster;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.DataPoint;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.firebase.models.ScalabilityMetrics;
import pt.utl.ist.meic.firebase.models.UXMetrics;
import pt.utl.ist.meic.firebase.models.Event;
import pt.utl.ist.meic.firebase.models.EventCategory;
import pt.utl.ist.meic.firebase.models.FirebaseCluster;
import pt.utl.ist.meic.firebase.models.User;
import pt.utl.ist.meic.utility.GeoHash;

public class FirebaseHelper {

	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";

	public static List<UXMetrics> getUXMetricsFromFirebase(boolean real,int totalEvents)
			throws FirebaseException, UnsupportedEncodingException {
		List<UXMetrics> toReturn = new ArrayList<>();

		String ref = FIREBASE_URL+"/UXEvaluator/";
        ref += (real) ? "real/" : "BAD/";
        ref += totalEvents;

		Firebase firebase = new Firebase(ref);

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			toReturn.add(parseUXMetrics(x));
		});

		return toReturn;
	}

	private static UXMetrics parseUXMetrics(Map.Entry<String, Object> x) {
		String metricsValues = x.getValue().toString();
		String[] aux = metricsValues.split(",");
		String timeUntilFirst = aux[0].split("=")[1];
		timeUntilFirst = timeUntilFirst.substring(0, timeUntilFirst.length() - 1);
		long time = Long.parseLong(timeUntilFirst);
		return new UXMetrics(time);
	}

	public static List<ScalabilityMetrics> getScalabilityMetricsTrajSizeFromFirebase(boolean real, int trajectorySize)
			throws FirebaseException, UnsupportedEncodingException {
		List<ScalabilityMetrics> toReturn = new ArrayList<>();

		String ref = FIREBASE_URL + "/EXPnetworkEvaluator";
		ref += (real) ? "/real" : "/BAD";
		ref+="/TrajSize";
		ref += "/" + trajectorySize;

		Firebase firebase = new Firebase(ref);

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			toReturn.add(parseMetrics(x));
		});

		return toReturn;
	}
	
	public static List<ScalabilityMetrics> getScalabilityMetricsNumEventsFromFirebase(boolean real, int numEvents)
			throws FirebaseException, UnsupportedEncodingException {
		List<ScalabilityMetrics> toReturn = new ArrayList<>();

		String ref = FIREBASE_URL + "/networkEvaluator";
		ref += (real) ? "/real" : "/BAD";
		ref+="/NumEvents";
		ref += "/" + numEvents;

		Firebase firebase = new Firebase(ref);

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			toReturn.add(parseMetrics(x));
		});

		return toReturn;
	}

	private static ScalabilityMetrics parseMetrics(Map.Entry<String, Object> x) {
		String metricsValues = x.getValue().toString();
		String[] aux = metricsValues.split(",");
		String bytesSpentStr = aux[0].split("=")[1];
		String updatesStr = aux[1].split("=")[1];
		updatesStr = updatesStr.substring(0, updatesStr.length() - 1);

		long bytesSpent = Long.parseLong(bytesSpentStr);
		int updates = Integer.parseInt(updatesStr);

		return new ScalabilityMetrics(bytesSpent, updates);
	}

	public static int getStoredLocationsSizeFromFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL + "/meta");

		FirebaseResponse response = firebase.get();
		return (int) response.getBody().get("totalLocations");
	}

	public static Long getLastUpdatedFromFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL + "/meta");

		FirebaseResponse response = firebase.get();
		return Long.parseLong(response.getBody().get("lastUpdated").toString());
	}

	public static long getCurrentLocationsSizeFromFirebase() throws FirebaseException, UnsupportedEncodingException {
		List<User> users = getUserListFromFirebase();

		long totalLocations = 0;
		for (User u : users) {
			Firebase firebase;
			try {
				firebase = new Firebase(FIREBASE_URL + "/locations/" + u.id);
				firebase.addQuery("shallow", "true");
				FirebaseResponse response = firebase.get();
				int locations = response.getBody().size();
				totalLocations += locations;
			} catch (FirebaseException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return totalLocations;
	}

	public static Map<String, UserProfile> populateUserEventsFromFirebase(Map<String, UserProfile> profiles)
			throws UnsupportedEncodingException, FirebaseException {
		List<Event> events = getEventListFromFirebase();
		events.forEach(x -> {
			if (profiles.containsKey(x.authorId)) {
				profiles.get(x.authorId).addEvent(x);
			}
		});
		profiles.entrySet().stream().forEach(x -> {
			x.getValue().calculateEventPercentages();
		});
		return profiles;
	}

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
		String suggestions = aux[1].split("=")[1];
		String username = aux[2].split("=")[1];
		username = username.substring(0, username.length() - 1);
		return new User(x.getKey(), username,suggestions);
	}

	private static List<Event> getEventListFromFirebase() throws FirebaseException, UnsupportedEncodingException {
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

	public static List<CheckIn> getUserCheckIns(String userId) throws FirebaseException, UnsupportedEncodingException {
		List<CheckIn> toReturn = new ArrayList<>();

		Firebase firebase = new Firebase(FIREBASE_URL + "/locations/" + userId);

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

	public static void writeNewClustersFirebase(List<ClusterWithMean> clusters, long totalCheckIns)
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteClustersFirebaseKMEANS();

		Firebase firebase = new Firebase(FIREBASE_URL + "/clusters");

		for (int i = 0; i < clusters.size(); i++) {
			// "PUT cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("mean", clusters.get(i).getmean().toString());
			dataMap.put("size", clusters.get(i).getVectors().size());
			dataMap.put("sizePerc", new Double(clusters.get(i).getVectors().size()) / totalCheckIns);

			FirebaseResponse response = firebase.put("cluster" + i, dataMap);
			System.out.println("\n\nResult of PUT cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}

	}

	private static void deleteClustersFirebaseKMEANS() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("clusters");
		System.out.println(response.getBody().toString());
	}
	
	
	public static List<FirebaseCluster> getGlobalClusters() throws FirebaseException, UnsupportedEncodingException {
		List<FirebaseCluster> toReturn = new ArrayList<>();

		Firebase firebase = new Firebase(FIREBASE_URL + "/clusters");

		FirebaseResponse response = firebase.get();
		response.getBody().entrySet().stream().forEach(x -> {
			try {
				toReturn.add(parseFirebaseCluster(x));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		});

		return toReturn;
	}

	private static FirebaseCluster parseFirebaseCluster(Entry<String, Object> x) throws ParseException {
		String clusterValues = x.getValue().toString();
		String[] tokens = clusterValues.split(",");
		
		String clusterMean = tokens[0].split("=")[1];
		String lati = clusterMean.split(";")[0];
		String longi = clusterMean.split(";")[1];
		longi = longi.substring(0, longi.length()-1);
		
		ClusterWithMean mean = new ClusterWithMean(0);
		mean.setMean(new DoubleArray(new double[] { Double.parseDouble(lati), Double.parseDouble(longi) }));
		
		Integer size = Integer.parseInt(tokens[1].split("=")[1]);
		
		String prct = tokens[2].split("=")[1];
		prct = prct.substring(0, prct.length()-1);
		Double sizePrct = Double.parseDouble(prct);

		return new FirebaseCluster(mean, size,sizePrct);
	}

	public static void writeNewFriendsFirebase(List<UserProfile> profiles, boolean limited, int limitProfiles,
			int limitSuggestions) throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {

		deleteFriendsFirebase();

		Firebase firebase = new Firebase(FIREBASE_URL + "/friends");
		if (limited) {
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
		} else {
			for (UserProfile profile : profiles) {
				Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
				profile.getSimilarities().entrySet().stream()
						.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
						.forEach(x -> dataMap.put(x.getKey(), x.getValue()));

				FirebaseResponse response = firebase.put("user" + profile.userId, dataMap);
				System.out.println("\n\nResult of PUT friends:\n" + response.getRawBody().toString());
				System.out.println("\n");
			}
		}

	}

	private static void deleteFriendsFirebase() throws FirebaseException, UnsupportedEncodingException {
		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("friends");
		System.out.println(response.getBody().toString());
	}

	public static void populateFakeEventsToFirebase(int numberOfEvents)
			throws FirebaseException, UnsupportedEncodingException, JacksonUtilityException {
		Firebase firebaseEventsRef = new Firebase(FIREBASE_URL + "/fakeEvents");
		Firebase firebaseLocationsRef = new Firebase(FIREBASE_URL + "/fakeEventsLocations");

		String authorName = "Bob";
		String authorId = "bobKey";
		EventCategory category = EventCategory.Food;
		String description = "testDescription #";
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String creationDate = df.format(new Date());

		for (int i = 0; i < numberOfEvents; i++) {
			// create fake event
			Event fake = new Event("" + i, authorId, authorName, category, creationDate, description + i);
			// push to firebase
			FirebaseResponse responseEvent = firebaseEventsRef.post(fake.toMap());
			String eventKey = (String) responseEvent.getBody().get("name");
			System.out.println("Event Key " + eventKey);

			// create fake location
			Map<String, Double> location = getRandomLocationTestingArea();
			GeoHash geoHash = new GeoHash(location.get("lati"), location.get("longi"));

			Map<String, Object> fakeEventLocation = new HashMap<String, Object>();
			fakeEventLocation.put("g", geoHash.getGeoHashString());
			fakeEventLocation.put("l", Arrays.asList(location.get("lati"), location.get("longi")));

			// push to firebase with the corresponding event key
			firebaseLocationsRef.put(eventKey, fakeEventLocation);
		}

		System.out.println("Finished populating fake Events");
	}

	private static Map<String, Double> getRandomLocationTestingArea() {
		Random random = new Random();
		Map<String, Double> testingAreaLimits = getTestingAreaLimits();

		double maxLati = testingAreaLimits.get("top");
		double minLati = testingAreaLimits.get("bot");
		double maxLongi = testingAreaLimits.get("right");
		double minLongi = testingAreaLimits.get("left");

		double lati = minLati + (maxLati - minLati) * random.nextDouble();
		double longi = minLongi + (maxLongi - minLongi) * random.nextDouble();

		Map<String, Double> toReturn = new HashMap<String, Double>() {
			{
				this.put("lati", lati);
				this.put("longi", longi);
			}
		};

		return toReturn;
	}

	private static Map<String, Double> getTestingAreaLimits() {
		return new HashMap<String, Double>() {
			{
				put("bot", 38.689615);
				put("top", 38.741777);
				put("left", -9.484146);
				put("right", -9.388799);
			}
		};
	}

}
