package pt.utl.ist.meic;

import java.awt.Checkbox;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import ca.pfv.spmf.algorithms.clustering.dbscan.AlgoDBSCAN;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceCosine;
import ca.pfv.spmf.algorithms.clustering.distanceFunctions.DistanceFunction;
import ca.pfv.spmf.algorithms.clustering.kmeans.AlgoKMeans;
import ca.pfv.spmf.patterns.cluster.Cluster;
import ca.pfv.spmf.patterns.cluster.ClusterWithMean;
import ca.pfv.spmf.patterns.cluster.DoubleArray;

public class GeoServer {

	// Delimiter used in CSV file
	private static final String DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private static final String pathGlobalCSV = "C:\\Android\\GeoFriendsFire\\GeoServer\\dataset\\Gowalla_totalCheckins.csv";
	private static final String CLUSTER_FILE_NAME = "clusters.ser";
	private static final String FIREBASE_URL = "https://geofriendsfire.firebaseio.com";

	private static final Double LOW_LATI = 40.543155;
	private static final Double HIGH_LATI = 40.904894;
	private static final Double LOW_LONGI = -74.056834;
	private static final Double HIGH_LONGI = -73.726044;

	private static final double MAX_DISTANCE = 0.001;// dbscan - meters
	private static final int MIN_POINTS = 10;// dbscan - at least 2 * dimension
	private static final int NUM_CLUSTERS = 5;// kmeans

	private static List<ClusterWithMean> clusters;
	private static List<DataPoint> checkIns;

	public static void main(String[] args) throws ParseException, IOException {
		/*
		applyKmeans();
		try {
			writeNewClustersFirebase();
		} catch (FirebaseException | JacksonUtilityException e) {
			e.printStackTrace();
		}*/

	// getCheckInsCSV(pathCSV);
	// System.out.println("got checkIns -> "+checkIns.size());
	// createCSV("newYork.csv");

	// applyDBSCAN();
	// readClustersFromDisk();
	// System.out.println("clusters -> "+clusters.size());

	}

	private static void applyKmeans() throws IOException {
		DistanceFunction distanceFunction = new DistanceCosine();

		// Apply the algorithm
		AlgoKMeans algoKMeans = new AlgoKMeans();
		clusters = algoKMeans.runAlgorithm("newYork.csv", NUM_CLUSTERS, distanceFunction, DELIMITER);
		algoKMeans.printStatistics();

		// Print the clusters found by the algorithm
		// For each cluster:
		int i = 0;
		for (ClusterWithMean cluster : clusters) {
			System.out.println("Cluster " + i++);
			System.out.println("size -> " + cluster.getVectors().size());
			System.out.println("mean -> "+cluster.getmean().toString());
		}
		
		//writeClustersToDisk();
	}

	private static void applyDBSCAN() throws IOException {
		// Apply the algorithm
		AlgoDBSCAN algo = new AlgoDBSCAN();
		//clusters = algo.runAlgorithm("centralPark.csv", MIN_POINTS, MAX_DISTANCE, DELIMITER);
		algo.printStatistics();

		// writeClustersToDisk();
	}

	private static void writeClustersToDisk() {
		try {
			FileOutputStream fos = new FileOutputStream(CLUSTER_FILE_NAME);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(clusters);
			oos.close();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static void readClustersFromDisk() {
		try {
			FileInputStream fis = new FileInputStream(CLUSTER_FILE_NAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			clusters = (List<ClusterWithMean>) ois.readObject();
			ois.close();
			fis.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		} catch (ClassNotFoundException c) {
			System.out.println("Class not found");
			c.printStackTrace();
			return;
		}
	}

	private static void getCheckInsCSV(String pathToCSV) throws FileNotFoundException, IOException {
		checkIns = new ArrayList<DataPoint>();
		Reader in = new FileReader(pathToCSV);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
			// String user = record.get(0);
			// Date date = df.parse(record.get(1));
			Double latitude = Double.parseDouble(record.get(2));
			Double longitude = Double.parseDouble(record.get(3));

			// USA = latitude 25 50 longitude -125 -70

			if (latitude >= LOW_LATI && latitude <= HIGH_LATI && longitude >= LOW_LONGI && longitude <= HIGH_LONGI) {
				checkIns.add(new DataPoint(latitude, longitude));
			}
		}
	}

	private static void createCSV(String fileName) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName);

			// Write a new point object to the CSV file
			for (DataPoint point : checkIns) {
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

	private static void convertTXTtoCSV() throws IOException {
		final Path path = Paths.get("C:\\Android\\GeoFriendsFire\\GeoServer\\dataset");
		final Path txt = path.resolve("Gowalla_totalCheckins.txt");
		final Path csv = path.resolve("Gowalla_totalCheckins.csv");
		try (final Stream<String> lines = Files.lines(txt);
				final PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csv, StandardOpenOption.CREATE_NEW))) {
			lines.map((line) -> line.split("\\s+")).map((line) -> Stream.of(line).collect(Collectors.joining(",")))
					.forEach(pw::println);
		}
	}


	private static void writeNewClustersFirebase()
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {
		
		deleteClustersFirebase();
		
		Firebase firebase = new Firebase(FIREBASE_URL+"/clusters");
		for(int i =0;i<clusters.size();i++){
			// "POST cluster to /clusters
			Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("clusterMean", clusters.get(i).getmean().toString());
			FirebaseResponse response = firebase.post("cluster"+i,dataMap);
			System.out.println("\n\nResult of POST cluster:\n" + response.getRawBody().toString());
			System.out.println("\n");
		}
		
	}

	private static void deleteClustersFirebase() throws FirebaseException, UnsupportedEncodingException {

		Firebase firebase = new Firebase(FIREBASE_URL);
		FirebaseResponse response = firebase.delete("clusters");
		System.out.println(response.getBody().toString());
	}

}
