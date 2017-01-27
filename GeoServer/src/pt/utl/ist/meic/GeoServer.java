package pt.utl.ist.meic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

public class GeoServer {

	public static void main(String[] args) throws FirebaseException, JsonParseException, JsonMappingException, IOException, JacksonUtilityException {

		
		
		
	}

	private static void writeStuffFirebase()
			throws FirebaseException, JacksonUtilityException, UnsupportedEncodingException {
		// get the base-url (ie: 'http://gamma.firebase.com/username')
		String firebase_baseUrl = "https://geofriendsfire.firebaseio.com/clusters";
		
		// create the firebase
		Firebase firebase = new Firebase( firebase_baseUrl );	

		// "PUT" (test-map into the fb4jDemo-root)
		Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
		Map<String, Object> dataMap2 = new LinkedHashMap<String, Object>();
		dataMap2.put( "Sub-Key1", "This is the first sub-value" );
		dataMap.put( "try", dataMap2 );
		FirebaseResponse response = firebase.post( dataMap );
		System.out.println( "\n\nResult of PUT (for the test-PUT to fb4jDemo-root):\n" + response );
		System.out.println("\n");
	}
	
}




