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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
		
		private static List<DataPoint> checkIns;
		private static List<DataPoint> userCheckIns;
	

	public static void getUserCheckInsCSV(String pathToCSV, String userId) throws ParseException, IOException {
		userCheckIns = new ArrayList<DataPoint>();
		Reader in = new FileReader(pathToCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			String user = record.get(0);
			// Date date = df.parse(record.get(1));
			Double latitude = Double.parseDouble(record.get(2));
			Double longitude = Double.parseDouble(record.get(3));

			// USA = latitude 25 50 longitude -125 -70

			if (user.equals(userId) && latitude >= 20 && latitude <= 50 && longitude >= -130 && longitude <= -110) {
				userCheckIns.add(new DataPoint(latitude, longitude));
			}
		}
	}

	public static void getCheckInsCSV(String pathToCSV) throws FileNotFoundException, IOException, ParseException {
		checkIns = new ArrayList<DataPoint>();
		Reader in = new FileReader(pathToCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			Date date = df.parse(record.get(1));
			Double latitude = Double.parseDouble(record.get(2));
			Double longitude = Double.parseDouble(record.get(3));

			// USA = latitude 25 50 longitude -125 -70

			if (latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI && longitude <= HIGH_LONGI) {
				checkIns.add(new DataPoint(latitude, longitude));
			}
		}
	}

	public static void createCSV(String fileName) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (DataPoint point : userCheckIns) {
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
	}

	public static void convertTXTtoCSV() throws IOException {
		final Path path = Paths.get("C:\\Android\\GeoFriendsFire\\GeoServer\\dataset");
		final Path txt = path.resolve("Gowalla_totalCheckins.txt");
		final Path csv = path.resolve("Gowalla_totalCheckins.csv");
		try (final Stream<String> lines = Files.lines(txt);
				final PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv, StandardOpenOption.CREATE_NEW))) {
			lines.map((line) -> line.split("\\s+")).map((line) -> Stream.of(line).collect(Collectors.joining(",")))
					.forEach(pw::println);
		}
	}

	
	
}
