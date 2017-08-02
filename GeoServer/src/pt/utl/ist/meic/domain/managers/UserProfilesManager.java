package pt.utl.ist.meic.domain.managers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.thegreshams.firebase4j.error.FirebaseException;
import pt.utl.ist.meic.domain.UserProfile;
import pt.utl.ist.meic.firebase.FirebaseHelper;
import pt.utl.ist.meic.firebase.models.User;
import pt.utl.ist.meic.utility.FileManager;

public class UserProfilesManager {	
	
	public static Map<String, UserProfile> createUserProfilesFromFirebase() throws UnsupportedEncodingException, FirebaseException {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();

		List<User> users = FirebaseHelper.getUserListFromFirebase();
		users.forEach(x -> {
			UserProfile profile = new UserProfile(x.id);
			profile.crossings = (x.suggestions.equals("NORMAL")) ? false : true;
			profile.username = x.username;
			id_userProfile.put(x.id, profile);
		});
		
		return id_userProfile;
	}
	
	public static Map<String,UserProfile> createUserProfilesGowalla(FileManager fileManager) {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();
		
		Set<String> idList = fileManager.getAmsAmsIdListFromFile().stream().collect(Collectors.toSet());
		for (String id : idList) {
			UserProfile profile = new UserProfile(id);
			profile.loadRealFriendsFromGowalla(fileManager);
			id_userProfile.put(id, profile);
		}
		return id_userProfile;
	}
	
	public static Map<String,UserProfile> createUserProfilesGowalla(FileManager fileManager, int limit) {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();
		
		Set<String> idList = fileManager.getAmsAmsIdListFromFile().stream().limit(limit).collect(Collectors.toSet());
		for (String id : idList) {
			UserProfile profile = new UserProfile(id);
			profile.loadRealFriendsFromGowalla(fileManager);
			id_userProfile.put(id, profile);
		}
		return id_userProfile;
	}
}
