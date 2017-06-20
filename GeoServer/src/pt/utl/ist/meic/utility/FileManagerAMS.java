package pt.utl.ist.meic.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import net.thegreshams.firebase4j.error.FirebaseException;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.DataPoint;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.firebase.FirebaseHelper;

public class FileManagerAMS {

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private static final String pathGlobalCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/Gowalla_totalCheckins.csv";
	private static final String pathUserCheckInsCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/usersCI/";
	private static final String pathGowallaFriends = "C:/Android/GeoFriendsFire/GeoServer/dataset/Gowalla_edges.csv";

	private static final String pathUserIdsAmsAms = "C:/Android/GeoFriendsFire/GeoServer/dataset/userIdsAmsAms.txt";
	private static final String pathAmsAmsFriendCount = "C:/Android/GeoFriendsFire/GeoServer/dataset/AmsAmsRelevantFriendCount.csv";
	private static final String pathAmsAmsRelevantFriends = "C:/Android/GeoFriendsFire/GeoServer/dataset/AmsAmsRelevantFriends.csv";
	private static final String pathAmsClusterCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/AMSCluster.csv";// checkIns
	private static final String pathAmsCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/AMS.csv";// user e datas

	// AMS
	private static final Double HIGH_LATI = 52.427128848;
	private static final Double LOW_LATI = 52.3261076319;
	private static final Double HIGH_LONGI = 4.98538970;
	private static final Double LOW_LONGI = 4.8308944702;

	private List<CheckIn> userCheckIns;

	// [Begin] userIdsAmsAms

	public void createUserIdsAmsAms() throws IOException {
		Set<String> userIdAms = getUserIdsCheckInsAms();
		System.out.println("Ams " + userIdAms.size());
		Set<String> userIdsAmsAms = filterUserIdsAtLeastOneFriend(userIdAms);
		System.out.println("AmsAms " + userIdsAmsAms.size());
		createTxtFileUserIdsAmsAms(userIdsAmsAms);
	}

	private void createTxtFileUserIdsAmsAms(Set<String> ids) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(pathUserIdsAmsAms);

			// Write a new point object to the CSV file
			for (String id : ids) {
				fileWriter.append(id);
				fileWriter.append(NEW_LINE_SEPARATOR);
			}

			System.out.println("TXT file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in TxtFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}

	private Set<String> filterUserIdsAtLeastOneFriend(Set<String> userIdsAms) throws IOException {
		Reader in = new FileReader(pathGowallaFriends);

		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		Set<String> toReturn = new HashSet<String>();
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (userIdsAms.contains(key) && userIdsAms.contains(value)) {
				toReturn.add(key);
				toReturn.add(value);
			}
		}
		return toReturn;

	}

	private Set<String> getUserIdsCheckInsAms() throws IOException {
		Reader in = new FileReader(pathAmsCSV);

		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		Set<String> toReturn = new HashSet<String>();
		for (CSVRecord record : records) {
			toReturn.add(record.get(0));
		}
		return toReturn;
	}

	// [End] userIdsAmsAms

	// [Begin] AmsAmsRelevantFriends & AmsAmsRelevantFriendsCount

	public void createAmsAmsRelevantFriendsAndCount(Set<String> userIdsAms) throws IOException {
		// AmsAmsRelevantFriends.csv
		Map<String, List<String>> relevant_user_friend = getRelevantFriends(userIdsAms);
		createAmsAmsRelevantCSV(relevant_user_friend);

		// AmsAmsFriendCount.csv
		Map<String, Integer> relevant_user_friend_count = countNumberFriends(relevant_user_friend);
		createFriendCountCSV(relevant_user_friend_count);

	}

	private void createFriendCountCSV(Map<String, Integer> relevant_user_friend_count) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(pathAmsAmsFriendCount);

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : relevant_user_friend_count.entrySet()) {
				fileWriter.append(String.valueOf(entry.getKey()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(entry.getValue()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}

	private Map<String, Integer> countNumberFriends(Map<String, List<String>> relevant_user_friend) {
		Map<String, Integer> relevant_user_friend_count = new HashMap<String, Integer>();
		relevant_user_friend.entrySet().stream().forEach(x -> {
			relevant_user_friend_count.put(x.getKey(), x.getValue().size());
		});
		return relevant_user_friend_count;
	}

	private void createAmsAmsRelevantCSV(Map<String, List<String>> relevant_user_friend) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(pathAmsAmsRelevantFriends);

			// Write a new point object to the CSV file
			for (Map.Entry<String, List<String>> entry : relevant_user_friend.entrySet()) {
				String user = entry.getKey();
				for (String s : entry.getValue()) {
					fileWriter.append(user);
					fileWriter.append(DELIMITER);
					fileWriter.append(s);
					fileWriter.append(NEW_LINE_SEPARATOR);
				}
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}

	private Map<String, List<String>> getRelevantFriends(Set<String> userIdsAms)
			throws FileNotFoundException, IOException {
		Reader in = new FileReader(pathGowallaFriends);

		Map<String, List<String>> relevant_user_friend = new HashMap<String, List<String>>();
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (userIdsAms.contains(key) && userIdsAms.contains(value)) {
				if (relevant_user_friend.containsKey(key)) {
					ArrayList<String> aux = new ArrayList<String>(relevant_user_friend.get(key));
					aux.add(value);
					relevant_user_friend.put(key, aux);
				} else {
					relevant_user_friend.put(key, new ArrayList<String>() {
						{
							add(value);
						}
					});
				}
			}
		}
		return relevant_user_friend;
	}

	// [End] AmsAmsRelevantFriends & AmsAmsRelevantFriendsCount

	public Set<String> getAmsAmsIdListFromFile() {
		Set<String> toReturn = new HashSet<String>();

		Path path = Paths.get(pathUserIdsAmsAms);
		try (Stream<String> lines = Files.lines(path)) {
			lines.forEach(s -> {
				if (!s.isEmpty()) {
					toReturn.add(s);
				}
			});
		} catch (IOException ex) {

		}
		System.out.println(toReturn.size() + " unique Ids");
		return toReturn;
	}

	public List<String> getRealFriendsFromGowalla(String username) throws IOException {

		List<String> realFriends = new ArrayList<>();

		Reader in = new FileReader(pathAmsAmsRelevantFriends);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (key.equals(username)) {
				realFriends.add(value);
			}
		}
		return realFriends;
	}

	public List<CheckIn> getUserCheckInsCsv(String userId) throws ParseException, IOException {
		userCheckIns = new ArrayList<CheckIn>();
		Reader in;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		int userIdInt = Integer.parseInt(userId);
		String userCheckInsPath = pathUserCheckInsCSV + userId + ".csv";
		File file = new File(userCheckInsPath);
		if (!file.exists()) {
			// checkIns do utilizador do csv global
			in = new FileReader(pathAmsCSV);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			for (CSVRecord record : records) {

				String user = record.get(0);
				Date date = df.parse(record.get(3));
				Double latitude = Double.parseDouble(record.get(1));
				Double longitude = Double.parseDouble(record.get(2));

				if (user.equals(userId) && latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI
						&& longitude <= HIGH_LONGI) {
					userCheckIns.add(new CheckIn(date, new DataPoint(latitude, longitude)));
				}
				// csv is order by id, no need to look past userId
				if (Integer.parseInt(user) > userIdInt) {
					break;
				}

			}
			System.out.println("criar csv para user " + userId + " com tamanho -> " + userCheckIns.size());
			createCsvCheckIns(userCheckInsPath);
			return userCheckIns;
		} else {
			// checkIns do csv do utilizador
			in = new FileReader(userCheckInsPath);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			for (CSVRecord record : records) {
				Date date = df.parse(record.get(2));
				Double latitude = Double.parseDouble(record.get(0));
				Double longitude = Double.parseDouble(record.get(1));
				userCheckIns.add(new CheckIn(date, new DataPoint(latitude, longitude)));
			}
			return userCheckIns;
		}
	}

	public void createCsvCheckIns(String fileName) {
		System.out.println("createCsvStayPoints");
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (CheckIn point : userCheckIns) {
				fileWriter.append(String.valueOf(point.getDataPoint().getLatitude()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(point.getDataPoint().getLongitude()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(point.getDateFormatted()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}

	public void createAmsCheckinsForClustering() throws IOException, ParseException {
		// creating newYork.csv from global
		System.out.println("createAmsCheckinsClustering");
		int checkIncounter = 0;
		Reader in = new FileReader(pathAmsCSV);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);

		List<DataPoint> list = new ArrayList<DataPoint>();

		for (CSVRecord record : records) {
			Double latitude = Double.parseDouble(record.get(1));
			Double longitude = Double.parseDouble(record.get(2));
			list.add(new DataPoint(latitude, longitude));
		}
		System.out.println(list.size() + " checkIns");
		// BEGIN

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(pathAmsClusterCSV);

			// Write a new point object to the CSV file
			for (DataPoint point : list) {
				fileWriter.append(String.valueOf(point.getLatitude()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(point.getLongitude()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
		System.out.println("createCSVAmsClustering --- END");

	}
}
