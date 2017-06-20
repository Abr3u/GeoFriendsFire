package test;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import pt.utl.ist.meic.GeoServer;
import pt.utl.ist.meic.domain.DataPoint;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.domain.managers.CheckInsManager;
import pt.utl.ist.meic.domain.managers.UserProfilesManager;
import pt.utl.ist.meic.utility.FileManager;

public class TestStuff {

	private static final int LEVEL = 0;
	private static final int NUM_CLUSTERS = 0;
	
	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	
	private static final Double HIGH_LATI = 52.427128848;
	private static final Double LOW_LATI = 52.3261076319;
	private static final Double HIGH_LONGI = 4.98538970;
	private static final Double LOW_LONGI = 4.8308944702;
	
	private static String pathGlobalCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/Gowalla_totalCheckins.csv";
	private static String pathAMSCSV = "C:/Android/GeoFriendsFire/GeoServer/dataset/AMS_checkIns.csv";

	
	public void createAMSCheckins() throws IOException, ParseException {
		// creating newYork.csv from global
		System.out.println("createAMSCheckins");
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
			fileWriter = new FileWriter(pathAMSCSV);

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
		System.out.println("createCSVnewYork --- END");

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
