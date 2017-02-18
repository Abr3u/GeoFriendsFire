package utility;

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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import one.util.streamex.StreamEx;
import pt.utl.ist.meic.CheckIn;
import pt.utl.ist.meic.DataPoint;

public class FileManager {

	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private static final String pathGlobalCSV = "C:/Users/ricar/Desktop/dataset/Gowalla_totalCheckins.csv";

	// New York
	private static final Double LOW_LATI = 40.543155;
	private static final Double HIGH_LATI = 40.904894;
	private static final Double LOW_LONGI = -74.056834;
	private static final Double HIGH_LONGI = -73.726044;

	// StayPoint filtering
	private static final int SP_MAX_TIME_SECONDS = 30 * 60;

	private List<CheckIn> userCheckIns;

	public void stuff() throws IOException  {
		System.out.println("getUserSP");
		Reader in = new FileReader(pathGlobalCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		String lastUser = "";
		for (CSVRecord record : records) {
			// Filter checkIns outside the range and not belonging to the user
			String user = record.get(0);
			//Date date = df.parse(record.get(1));
			Double latitude = Double.parseDouble(record.get(2));
			Double longitude = Double.parseDouble(record.get(3));

			// ajustar coordenadas de acordo com os users aqui
			if (!lastUser.equals(user) && latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI && longitude <= HIGH_LONGI) {
				System.out.println("user -- > "+user);
				lastUser = user;
			}

		}

	}

	public List<CheckIn> getUserCheckInsCsv(String userId, boolean global) throws ParseException, IOException {
		System.out.println("getUserSP");

		userCheckIns = new ArrayList<CheckIn>();
		Reader in;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		if (global) {
			// checkIns do utilizador do csv global
			in = new FileReader(pathGlobalCSV);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			for (CSVRecord record : records) {
				// Filter checkIns outside the range and not belonging to the
				// user
				String user = record.get(0);
				Date date = df.parse(record.get(1));
				Double latitude = Double.parseDouble(record.get(2));
				Double longitude = Double.parseDouble(record.get(3));

				// ajustar coordenadas de acordo com os users aqui
				if (user.equals(userId) && latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI && longitude <= HIGH_LONGI) {
					userCheckIns.add(new CheckIn(date, new DataPoint(latitude, longitude)));
				}

			}
			System.out.println("tamanho -> "+userCheckIns.size());
			createCsvCheckIns(userId+".csv");
			return userCheckIns;
		} else {
			// checkIns do csv do utilizador
			in = new FileReader(userId+".csv");
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

	private static double distanceBetween(double lat1, double lat2, double lon1, double lon2) {

		final int R = 6371; // Radius of the earth

		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		distance = Math.pow(distance, 2);

		return Math.sqrt(distance);
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
		System.out.println("createCSVStayPoints --- END");
	}

}
