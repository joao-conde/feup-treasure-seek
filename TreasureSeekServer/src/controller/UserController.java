package controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import main.DBOperations;
import model.Treasure;
import model.User;
import util.NotAuthorizedException;
import util.ResourceNotFoundException;
import util.Utils.Pair;

public class UserController {
	
	private static final String FACEBOOK_API_ADDRES = "https://graph.facebook.com/v2.11/"; 
	private ArrayList<String> dbServerHostAddresses = new ArrayList<>();

	public UserController(ArrayList<String> dbServerHostAddresses) {
		this.dbServerHostAddresses = dbServerHostAddresses;
	}


	public User loginUser(String token, DBOperations remoteObject) {
		
		HttpURLConnection  facebookConneciton = null;
		String urlString = FACEBOOK_API_ADDRES + "me?fields=name,email&access_token=" + token; 
		
		try {
		    //Create connection
		    URL url = new URL(urlString);
		    facebookConneciton = (HttpURLConnection) url.openConnection();
		    facebookConneciton.setRequestMethod("GET");
 

		    //Get Response  
		    InputStream is = facebookConneciton.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    //StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		    
		    Scanner scanner = new Scanner(rd);
		    
		    String responseString = scanner.nextLine();
		    

		    rd.close();
		    
		    //System.out.println(response);
		    
		    JSONObject userInfo = new JSONObject(responseString);
		    
		    if(userInfo.has("error")) {
		    		
		    		scanner.close();
		    		return null;
		    		

		    }
		    
		    System.out.println(userInfo.toString());
		    
		    User user = remoteObject.getUser(true, Long.parseLong(userInfo.getString("id")));
		    
		    if(user == null)
		    		user = remoteObject.insertUser(true, userInfo.getLong("id"), userInfo.getString("email"),  token, userInfo.getString("name"), dbServerHostAddresses);
		    else
		    		remoteObject.updateUser(true, (long)user.getValue("id"), token, dbServerHostAddresses);
		    		    		    
		    scanner.close();
		    return user;
		    
		 
		} catch (Exception e) {
		 
			e.printStackTrace();
	  
		} finally {
		  
		    if (facebookConneciton != null) {
		    		facebookConneciton.disconnect();
		    }
	    
		}
		
		return null;
		
	}
		
	
	public boolean logoutUser(long id, String token, DBOperations remoteObject) {
		
		try {
			
			User user = remoteObject.getUser(true, id);
			
			if(user == null)
				return false;
						
			if(!token.equals(user.getValue("token")))
				return false;
			
			return remoteObject.updateUser(true, id, "", dbServerHostAddresses);
			
			
			
		} catch (RemoteException | SQLException e) {
			
			return false;
		}
				
	}
	
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasures(long userId, DBOperations remoteObject) {
		
		try {
			return remoteObject.getAllTreasuresWithFoundInfo(userId);
		}
		catch(RemoteException | SQLException e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
	}
	
	public Pair<Boolean,Treasure> validateTreasure(int treasureId, String answer, String token, long userId, DBOperations remoteObject) throws ResourceNotFoundException, NotAuthorizedException, RemoteException, SQLException {
		
			
			User user = remoteObject.getUser(true, userId);
			
			if(user == null)
				throw new ResourceNotFoundException();
			
						
			if(!token.equals(user.getValue("token")))
				throw new NotAuthorizedException();
			
			Treasure treasure = remoteObject.getTreasure(treasureId);
			
			if(treasure == null)
				throw new ResourceNotFoundException();
			
			String correctAnswer = (String)treasure.getValue("answer");
			
			String regex = ".*" + correctAnswer + ".*"; 
			Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(answer);
			
			boolean isCorrectAnswer = matcher.matches(); 
			
			if(!isCorrectAnswer)
				return new Pair<Boolean,Treasure>(false,treasure);
						
			return new Pair<Boolean,Treasure>(remoteObject.insertFoundTreasure(treasureId, userId, dbServerHostAddresses), treasure);
			
			
		
	}
	
	
}
