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
import pt.utl.ist.meic.utility.FileManagerAMS;

public class UserProfilesManager {
	
	private boolean firebase;
	private FileManager fileManager;
	
	public UserProfilesManager(FileManager fileManager, boolean firebaseWorkflow) {
		this.firebase = firebaseWorkflow;
		this.fileManager = fileManager;
	}

	public Map<String, UserProfile> createUserProfiles(){
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();
		
		if(firebase){
			try {
				id_userProfile = createUserProfilesFromFirebase();
			} catch (UnsupportedEncodingException | FirebaseException e) {
				e.printStackTrace();
			}
		}else{
			id_userProfile = createUserProfilesGowalla();
		}
		
		return id_userProfile;
	}
	
	
	private Map<String, UserProfile> createUserProfilesFromFirebase() throws UnsupportedEncodingException, FirebaseException {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();

		List<User> users = FirebaseHelper.getUserListFromFirebase();
		users.forEach(x -> {
			UserProfile profile = new UserProfile(x.id);
			profile.username = x.username;
			id_userProfile.put(x.id, profile);
		});
		
		return id_userProfile;
	}
	
	private Map<String,UserProfile> createUserProfilesGowalla() {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();
		
		Set<String> idList = fileManager.getNyNyIdListFromFile();
		for (String id : idList) {
			UserProfile profile = new UserProfile(id);
			profile.loadRealFriendsFromGowalla(fileManager);
			id_userProfile.put(id, profile);
		}
		return id_userProfile;
	}
	
	//TODO remove
	public static Map<String,UserProfile> createUserProfilesAMS() {
		Map<String,UserProfile> id_userProfile = new HashMap<String, UserProfile>();
		
		FileManagerAMS ams = new FileManagerAMS();
		Set<String> idList = ams.getAmsAmsIdListFromFile();
		for (String id : idList) {
			UserProfile profile = new UserProfile(id);
			profile.loadRealFriendsFromGowalla(ams);
			id_userProfile.put(id, profile);
		}
		return id_userProfile;
	}
}
