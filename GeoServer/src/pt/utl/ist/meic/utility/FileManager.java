package pt.utl.ist.meic.utility;

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
import java.util.Date;
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
	private static final String pathNewYorkCSV = "C:/Users/ricar/Desktop/dataset/newYork.csv";

	// New York
	private static final Double LOW_LATI = 40.543155;
	private static final Double HIGH_LATI = 40.904894;
	private static final Double LOW_LONGI = -74.056834;
	private static final Double HIGH_LONGI = -73.726044;

	// StayPoint filtering
	private static final int SP_MAX_TIME_SECONDS = 30 * 60;

	private List<CheckIn> userCheckIns;
	
	
	public List<String> stuff2() throws IOException, ParseException {
		//gets the Ids of the users from new york
		System.out.println("stuff2");
		Reader in = new FileReader(pathNewYorkCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		List<String> idsList = new ArrayList<String>();
		String lastId = "";
		for (CSVRecord record : records) {
			String userId = String.valueOf(record.get(0));
			Date date = df.parse(record.get(3));
			Double latitude = Double.parseDouble(record.get(1));
			Double longitude = Double.parseDouble(record.get(2));
			if(!userId.equals(lastId)){
				idsList.add(userId);
				lastId = userId;
			}
		}
		return idsList;
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

	public List<CheckIn> getUserCheckInsCsv(String userId, boolean createCSV) throws ParseException, IOException {
		userCheckIns = new ArrayList<CheckIn>();
		Reader in;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		int userIdInt = Integer.parseInt(userId);
		if (createCSV) {
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
			createCsvCheckIns(userId + ".csv");
			return userCheckIns;
		} else {
			// checkIns do csv do utilizador
			in = new FileReader(userId + ".csv");
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

	public void createTXTFileUserIds(List<String> ids) {
		System.out.println("createCsvStayPoints");
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter("userIds.txt");

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
	
	public void createCsvSimilarities(List<UserProfile> profiles, String fileName) {
		System.out.println("createCsvSimilarities");

		try {
		 final FileWriter fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for(UserProfile profile : profiles){				
				profile.getSimilarities().entrySet().stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.forEach(x->{
					
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

	public List<String> getIdListFromFile() {
		//limited to 10
		List<String> toReturn = new ArrayList<String>();

		Path path = Paths.get("C:/Android/GeoFriendsFire/GeoServer/userIds.txt");
        try (Stream<String> lines = Files.lines(path)) {
            lines.limit(10).forEach(s -> {
            	if(!s.isEmpty()){
            		toReturn.add(s);
            	}
            });
        } catch (IOException ex) {
        	
        }
		System.out.println(toReturn.size()+" unique Ids");
		return toReturn;
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

	

}
