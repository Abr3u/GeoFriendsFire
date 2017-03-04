package pt.utl.ist.meic.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import one.util.streamex.StreamEx;
import pt.utl.ist.meic.domain.CheckIn;
import pt.utl.ist.meic.domain.DataPoint;
import pt.utl.ist.meic.domain.UserProfile;

public class FileManager {

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private static final String pathGlobalCSV = "C:/Users/ricar/Desktop/dataset/Gowalla_totalCheckins.csv";
	private static final String pathNewYorkCSV = "C:/Users/ricar/Desktop/dataset/newYork.csv";// users
																								// e
																								// datas
	private static final String pathUserCheckInsCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/usersCI/";
	private static final String pathUserIdsNY = "C:/Android/GeoFriendsFire/GeoServer/dataset/userIdsNyNy.txt";
	private static final String pathGowallaFriends = "C:/Android/GeoFriendsFire/GeoServer/dataset/Gowalla_edges.csv";
	private static final String pathGowallaFriendCount = "C:/Android/GeoFriendsFire/GeoServer/dataset/friendCount.csv";
	private static final String pathGowallaNyNyFriendCount = "C:/Android/GeoFriendsFire/GeoServer/dataset/NyUserNyFriendCount.csv";

	// New York
	private static final Double LOW_LATI = 40.543155;
	private static final Double HIGH_LATI = 40.904894;
	private static final Double LOW_LONGI = -74.056834;
	private static final Double HIGH_LONGI = -73.726044;

	private List<CheckIn> userCheckIns;
	
	public void createNyUserNyFriendsCount() throws IOException, ParseException {
		// gets the number of friends
		Reader in = new FileReader(pathGowallaFriends);

		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		Map<String, Integer> user_friends = new HashMap<String, Integer>();
		List<String> nyIds = getIdListFromFileNy();
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if(nyIds.contains(key) && nyIds.contains(value)){
				if(user_friends.containsKey(key)){
					user_friends.put(key, user_friends.get(key)+1);
				}else{
					user_friends.put(key, 1);
				}
			}
		}

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("NyUserNyFriendCount.csv");

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : user_friends.entrySet()) {
				fileWriter.append(String.valueOf(entry.getKey()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(entry.getValue()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}
			int totalFound = user_friends.entrySet().stream().mapToInt(x -> x.getValue()).sum();
			// fileWriter.append(String.valueOf(totalFound));

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
		System.out.println("createCSVFriendCount --- END");

	}
	
	public void createFriendCountNYUsers() throws IOException {
		HashMap<String, Integer> user_friendCount = new HashMap<String, Integer>();
		Reader in = new FileReader(pathGowallaFriendCount);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		List<String> newYorkIds = getIdListFromFileNy();
		// so considera friends dos users de NY
		for (CSVRecord record : records) {
			String userId = record.get(0);
			if (newYorkIds.contains(userId)) {
				user_friendCount.put(userId, Integer.parseInt(record.get(1)));
			}
		}

		System.out.println("createCsvFriendCountNY");
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("friendCountNY.csv");

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : user_friendCount.entrySet()) {
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

	public void stuff() throws IOException, ParseException {
		// creating newYork.csv from global
		System.out.println("stuff");
		int checkIncounter = 0;
		Reader in = new FileReader(pathGlobalCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		String lastUser = "";

		List<Auxiliar> list = new ArrayList<Auxiliar>();

		for (CSVRecord record : records) {
			// Filter checkIns outside the range and not belonging to the user
			String user = record.get(0);
			Date date = df.parse(record.get(1));
			Double latitude = Double.parseDouble(record.get(2));
			Double longitude = Double.parseDouble(record.get(3));

			if (latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI && longitude <= HIGH_LONGI) {
				// System.out.println("user -- > "+lastUser+" :::
				// "+checkIncounter);
				lastUser = user;
				checkIncounter = 0;
				list.add(new Auxiliar(date, new DataPoint(latitude, longitude), user));
			}
			if (lastUser.equals(user)) {
				checkIncounter++;
			}
		}
		System.out.println(list.size() + " checkIns");
		// BEGIN

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("newnewYork.csv");

			// Write a new point object to the CSV file
			for (Auxiliar point : list) {
				fileWriter.append(String.valueOf(point.userId));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(point.mDataPoint.getLatitude()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(point.mDataPoint.getLongitude()));
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
		System.out.println("createCSVStayPoints --- END");

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
			in = new FileReader(pathNewYorkCSV);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			for (CSVRecord record : records) {
				// Filter checkIns outside the range and not belonging to the
				// user
				String user = record.get(0);
				Date date = df.parse(record.get(3));
				Double latitude = Double.parseDouble(record.get(1));
				Double longitude = Double.parseDouble(record.get(2));

				// ajustar coordenadas de acordo com os users aqui
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


	public void createTXTFileUserIdsNewYork() throws IOException {
		System.out.println("createuserIdsFiltered");
		FileWriter fileWriter = null;

		Reader in = new FileReader(pathGowallaNyNyFriendCount);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		try {
			fileWriter = new FileWriter("userIdsNyNy.txt");

			// Write a new point object to the CSV file
			for (CSVRecord record : records) {
				fileWriter.append(record.get(0));
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

	public void createCsvSimilarities(List<UserProfile> profiles, String fileName, int limit) {
		System.out.println("createCsvSimilarities");

		try {
			final FileWriter fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (UserProfile profile : profiles) {
				profile.getSimilarities().entrySet().stream()
						.sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(limit).forEach(x -> {

							try {
								fileWriter.append(String.valueOf(profile.userId));
								fileWriter.append(DELIMITER);
								fileWriter.append(String.valueOf(x.getKey()));
								fileWriter.append(DELIMITER);
								fileWriter.append(String.valueOf(x.getValue()));
								fileWriter.append(NEW_LINE_SEPARATOR);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
			}
			fileWriter.flush();
			fileWriter.close();

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		}
	}


	public List<String> getIdListFromFileNy() {
		List<String> toReturn = new ArrayList<String>();

		Path path = Paths.get(pathUserIdsNY);
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

	private void createCsvFriendsFound(String fileName, Map<String, Integer> friendsFound, int totalFound) {
		System.out.println("createCsvFriendsFound");
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : friendsFound.entrySet()) {
				fileWriter.append(String.valueOf(entry.getKey()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(entry.getValue()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}
			// fileWriter.append(String.valueOf(totalFound));

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

	private void createCsvFriendsFoundPrct(String fileName, HashMap<String, Double> user_foundTotal) {
		System.out.println("createCsvFriendsFoundPrct");
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (Map.Entry<String, Double> entry : user_foundTotal.entrySet()) {
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

	private void convertTXTtoCSV() throws IOException {
		final Path path = Paths.get("C:/Users/ricar/Desktop/dataset/");
		final Path txt = path.resolve("Gowalla_edges.txt");
		final Path csv = path.resolve("Gowalla_edges.csv");
		try (final Stream<String> lines = Files.lines(txt);
				final PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv, StandardOpenOption.CREATE_NEW))) {
			lines.map((line) -> line.split("\\s+")).map((line) -> Stream.of(line).collect(Collectors.joining(",")))
					.forEach(pw::println);
		}
	}

	public void createFriendCountCSV() throws IOException, ParseException {
		// gets the number of friends
		Reader in = new FileReader(pathGowallaFriends);

		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		Map<String, Integer> user_friends = new HashMap<String, Integer>();
		String lastId = records.iterator().next().get(0);
		int count = 0;
		for (CSVRecord record : records) {
			String userId = String.valueOf(record.get(0));
			if (!userId.equals(lastId)) {
				user_friends.put(lastId, count);
				lastId = userId;
				count = 1;
			} else {
				count++;
			}
		}
		user_friends.put(lastId, count);

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("friendCount.csv");

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : user_friends.entrySet()) {
				fileWriter.append(String.valueOf(entry.getKey()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(entry.getValue()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}
			int totalFound = user_friends.entrySet().stream().mapToInt(x -> x.getValue()).sum();
			// fileWriter.append(String.valueOf(totalFound));

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
		System.out.println("createCSVFriendCount --- END");

	}

	public void getNumberCheckIns() throws IOException, ParseException {
		// gets the number of checkIns per user
		Reader in = new FileReader(pathNewYorkCSV);

		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		Map<String, Integer> user_checkIns = new HashMap<String, Integer>();
		String lastId = records.iterator().next().get(0);
		int count = 0;
		for (CSVRecord record : records) {
			String userId = String.valueOf(record.get(0));
			if (!userId.equals(lastId)) {
				user_checkIns.put(lastId, count);
				lastId = userId;
				count = 1;
			} else {
				count++;
			}
		}
		user_checkIns.put(lastId, count);

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("checkInsCountNY.csv");

			// Write a new point object to the CSV file
			for (Map.Entry<String, Integer> entry : user_checkIns.entrySet()) {
				fileWriter.append(String.valueOf(entry.getKey()));
				fileWriter.append(DELIMITER);
				fileWriter.append(String.valueOf(entry.getValue()));
				fileWriter.append(NEW_LINE_SEPARATOR);
			}
			int totalFound = user_checkIns.entrySet().stream().mapToInt(x -> x.getValue()).sum();
			// fileWriter.append(String.valueOf(totalFound));

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
		System.out.println("createCSVCheckInCountNY --- END");

	}

	public void createFoundCSV(String friendsPath, String foundPath) throws IOException {
		Map<String, List<String>> gowalla_friends = new HashMap<String, List<String>>();

		Reader in = new FileReader(pathGowallaFriends);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (gowalla_friends.containsKey(key)) {
				ArrayList<String> aux = new ArrayList<String>(gowalla_friends.get(key));
				aux.add(value);
				gowalla_friends.put(key, aux);
			} else {
				gowalla_friends.put(key, new ArrayList<String>() {
					{
						add(value);
					}
				});
			}
		}

		HashMap<String, Integer> user_friendsFound = new HashMap<String, Integer>();
		in = new FileReader(friendsPath);
		records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (gowalla_friends.containsKey(key)) {
				for (String friend : gowalla_friends.get(key)) {
					if (friend.equals(value)) {
						// increment number of friends found
						if (user_friendsFound.containsKey(key)) {
							user_friendsFound.put(key, user_friendsFound.get(key) + 1);
						} else {
							user_friendsFound.put(key, 1);
						}
					}
				}
			}
		}
		int totalFound = user_friendsFound.entrySet().stream().mapToInt(x -> x.getValue()).sum();
		createCsvFriendsFound(foundPath, user_friendsFound, totalFound);
	}

	public void createFoundPrctCSV(String geofriends, String prctPath) throws IOException {
		Map<String, Integer> user_friendCount = new HashMap<String, Integer>();

		Reader in = new FileReader(pathGowallaFriendCount);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			Integer value = Integer.parseInt(record.get(1));
			user_friendCount.put(key, value);
		}

		HashMap<String, Double> user_foundTotal = new HashMap<String, Double>();
		in = new FileReader(geofriends);
		records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (user_friendCount.containsKey(key)) {
				// found,total
				user_foundTotal.put(key, Double.parseDouble(value) / user_friendCount.get(key));
			}
		}
		createCsvFriendsFoundPrct(prctPath, user_foundTotal);
	}

	private class Auxiliar {
		public DataPoint mDataPoint;
		private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		private Date mDate;
		public String userId;

		public Auxiliar(Date date, DataPoint dataPoint, String user) {
			this.mDataPoint = dataPoint;
			this.mDate = date;
			this.userId = user;
		}

		public String getDateFormatted() {
			return df.format(mDate);
		}

	}

	public double calculatePrecision(String foundCSV, int totalSuggestedPerUser) throws IOException {
		Reader in = new FileReader(foundCSV);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		double totalFound = 0;
		for (CSVRecord record : records) {
			totalFound += Double.parseDouble(record.get(1));
		}
		in = new FileReader(pathGowallaNyNyFriendCount);
		records = CSVFormat.EXCEL.parse(in);
		int N = 0;
		// so considera friends dos users de NY
		for (CSVRecord record : records) {
				N++;
		}
		double recall = totalFound / (totalSuggestedPerUser*N);
		return recall;
	}

	public double calculateRecall(String foundCSV, int totalUsers) throws IOException {
		Reader in = new FileReader(foundCSV);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		double totalFound = 0;
		for (CSVRecord record : records) {
			totalFound += Double.parseDouble(record.get(1));
		}

		in = new FileReader(pathGowallaNyNyFriendCount);
		records = CSVFormat.EXCEL.parse(in);
		int totalGoawallFriendsNY = 0;
		// so considera friends dos users de NY
		for (CSVRecord record : records) {
				totalGoawallFriendsNY += Integer.parseInt(record.get(1));
		}

		double recall = totalFound / totalGoawallFriendsNY;
		return recall;
	}

	
	
	public void createNyUserFriendsCSV() throws IOException {
		Map<String, List<String>> gowalla_friends = new HashMap<String, List<String>>();

		Reader in = new FileReader(pathGowallaFriends);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		List<String> nyIds = getIdListFromFileNy();
		for (CSVRecord record : records) {
			String key = record.get(0);
			String value = record.get(1);
			if (nyIds.contains(key) && nyIds.contains(value)) {
				if (gowalla_friends.containsKey(key)) {
					ArrayList<String> aux = new ArrayList<String>(gowalla_friends.get(key));
					aux.add(value);
					gowalla_friends.put(key, aux);
				} else {
					gowalla_friends.put(key, new ArrayList<String>() {
						{
							add(value);
						}
					});
				}
			}
		}
		
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("nyUserFriends.csv");

			// Write a new point object to the CSV file
			for (Map.Entry<String, List<String>> entry : gowalla_friends.entrySet()) {
				for(String friend : entry.getValue()){
					fileWriter.append(String.valueOf(entry.getKey()));
					fileWriter.append(DELIMITER);
					fileWriter.append(friend);
					fileWriter.append(NEW_LINE_SEPARATOR);
				}
			}
			// fileWriter.append(String.valueOf(totalFound));

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

}
