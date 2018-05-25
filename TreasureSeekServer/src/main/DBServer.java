package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Statement;
import java.util.Scanner;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import util.Utils.Pair;
import model.Treasure;
import model.User;
import util.Utils;

public class DBServer extends UnicastRemoteObject implements DBOperations {

	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] { "TLSv1.2" };
	public static String[] ENC_CYPHER_SUITES = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };
	public static final String DB_SERVER_OBJECT_NAME = "dbServerObject";
	
	
	private static final String DB_PATH = "../db/";
	
    private String DBNAME;
    private String DBURL;
    
    private String localAddress;
    
    private Connection connection;

          
    protected DBServer(String localAddress) throws Exception {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
		
		this.localAddress = localAddress;
				
		Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT,
				new SslRMIClientSocketFactory(),
				new SslRMIServerSocketFactory(null,ENC_PROTOCOLS,true));
			
				
		registry.bind(DB_SERVER_OBJECT_NAME, this);
		
		
		createConnection();
	}
    
    
	public static void main(String[] args) throws Exception {
		
		Utils.setSecurityProperties(false);  
		
		String localAddress = args[0];
						
		new DBServer(localAddress);
		
		System.out.println("DB Server running...");
//		Runtime.getRuntime().addShutdownHook(new Thread(new CloseDBServer(dbServer)));
	}	
	
	private void createConnection() throws SQLException, FileNotFoundException {
		DBNAME = "treasureSeekDB.db";
		DBURL = "jdbc:sqlite:../db/" + DBNAME;

		boolean dbFileExists = new File(DB_PATH + DBNAME).exists();

		connection = DriverManager.getConnection(DBURL);

		if (!dbFileExists) {
			String schema = "";
			Scanner scanner = new Scanner(new File(DB_PATH + "seed.sql"));

			while (scanner.hasNextLine()) {
				schema += scanner.nextLine();
			}

			scanner.close();

			Statement st = connection.createStatement();
			st.executeUpdate(schema);
			st.close();

		}		
	}

//	public static class CloseDBServer implements Runnable{
//		DBServer dbServer;
//	
//		public CloseDBServer(DBServer dbServer) {
//			this.dbServer = dbServer;
//		}
//
//		public void run() {
//			try {
//				
//				for(String dbAddress : dbServer.dbServerAddresList) {
//					dbServer.getDbOperations(dbAddress).removeDBServer(dbServer.localAddress);
//				}
//				
//				
//			} catch (RemoteException | NotBoundException e) {
//				e.printStackTrace();
//			}
//		};
//	}

	@Override
	public User insertUser(long id, String email, String token, String name, ArrayList<String> dbServerHostAddresses)
			throws RemoteException {

		try {

			PreparedStatement stmt = connection
					.prepareStatement("INSERT INTO user (id, email, token, name) VALUES (?, ?, ?, ?)");

			stmt.setLong(1, id);
			stmt.setString(2, email);
			stmt.setString(3, token);
			stmt.setString(4, name);
			stmt.executeUpdate();

			System.out.println("User inserted with success.");

			User user = new User();
			user.setValue("id", id);
			user.setValue("email", email);
			user.setValue("token", token);
			user.setValue("name", name);
			user.setValue("admin", false);

//			if (appServerRequest) {
//
//				Thread replicateChangeThread = new Thread() {
//					public void run() {
//
//						try {
//							
//							for (String dbAddress : dbServerAddresList) {
//
//								DBOperations db = getDbOperations(dbAddress);
//
//								db.insertUser(false, id, email, token, name, dbServerHostAddresses);
//
//							}
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				};
//
//				replicateChangeThread.start();
//			}
			replicateData(FunctionCallType.INSERT_USER, dbServerHostAddresses, new Object[] {id, email, token, name});

			return user;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}

	}

	@Override
	public User getUser(long id) throws RemoteException, SQLException {

		PreparedStatement stmt = connection.prepareStatement("SELECT * from user WHERE id = ?");

		stmt.setLong(1, id);
		ResultSet result = stmt.executeQuery();

		if (!result.next())
			return null;

		User user = new User();
		user.setValue("id", result.getLong(1));
		user.setValue("email", result.getString(2));
		user.setValue("token", result.getString(3));
		user.setValue("name", result.getString(4));
		user.setValue("admin", result.getBoolean(5));
		
		return user;

	}

	@Override
	public boolean updateUser(long id, String token, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException {

		try {

			PreparedStatement stmt = connection.prepareStatement("UPDATE user SET token = ? WHERE id = ?");

			stmt.setString(1, token);
			stmt.setLong(2, id);
			stmt.executeUpdate();

//			if (appServerRequest) {
//				
//				Thread replicateChangeThread = new Thread() {
//					public void run() {
//
//						try {
//							for (String dbAddress : dbServerAddresList) {
//								
//								DBOperations db = getDbOperations(dbAddress);
//								
//								db.updateUser(false, id, token, dbServerHostAddresses);
//								System.out.println("updateUser called for DB Server");
//
//							}
//						} catch (RemoteException | SQLException | NotBoundException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				};
//				
//				replicateChangeThread.start();
//			}

			System.out.println("User updated with success on DB");
			
			replicateData(FunctionCallType.UPDATE_USER, dbServerHostAddresses, new Object[] {id, token});

			return true;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return false;
		}

	}

	@Override
	public ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException {

		PreparedStatement stmt = connection.prepareStatement("SELECT * from treasure");

		ResultSet result = stmt.executeQuery();
		ArrayList<Treasure> treasures = new ArrayList<>();

		while (result.next()) {

			Treasure treasure = new Treasure();
			treasure.setValue("id", result.getInt(1));
			treasure.setValue("latitude", result.getDouble(2));
			treasure.setValue("longitude", result.getDouble(3));
			treasure.setValue("description", result.getString(4));
			treasures.add(treasure);

		}

		return treasures;

	}

	@Override
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasuresWithFoundInfo(long userId) throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection.prepareStatement(
			"SELECT * FROM (" +
				"SELECT id, latitude, longitude, description, 1 as found, challenge, challengeSolution " + 
				"FROM treasure " + 
				"WHERE (?, treasure.id) " + 
				"IN (select userId, treasureId from user_treasure) " + 
				
				"UNION " + 
				
				"SELECT id, latitude, longitude, description, 0 as found, challenge, challengeSolution " + 
				"FROM treasure " + 
				"WHERE (?, treasure.id) " + 
				"NOT IN (select userId, treasureId from user_treasure) " +
			");"
        );
		
		System.out.println("UserId: " + userId);
		
		stmt.setLong(1, userId);
		stmt.setLong(2, userId);
		ResultSet resultSet = stmt.executeQuery();
				
		ArrayList<Treasure> treasures = new ArrayList<>();
		ArrayList<Treasure> foundTreasures = new ArrayList<>();
		
		while(resultSet.next()) {
			
			Treasure treasure = new Treasure();
			treasure.setValue("id", resultSet.getInt(1));
			treasure.setValue("latitude", resultSet.getDouble(2));
			treasure.setValue("longitude", resultSet.getDouble(3));
			treasure.setValue("description", resultSet.getString(4));
						
			if(resultSet.getBoolean(5) == true) {
				
				treasure.setValue("challenge", resultSet.getString(6));
				treasure.setValue("answer", resultSet.getString(7));
				
				foundTreasures.add(treasure);
				
			}
				
			else {
				
				treasure.setValue("challenge", "");
				treasure.setValue("answer", "");
				treasures.add(treasure);
			}
				
		}
		
		Pair<ArrayList<Treasure>,ArrayList<Treasure>> result = new Pair<>(treasures,foundTreasures);					
		return result;
		
	}
	

	@Override
	public Treasure getTreasure(int treasureId) throws RemoteException, SQLException {
				
		PreparedStatement stmt = connection.prepareStatement("SELECT challenge, challengeSolution, id FROM treasure WHERE id = ?");
		stmt.setInt(1, treasureId);
		ResultSet result = stmt.executeQuery();
		
		if (!result.next())
			return null;
		
		Treasure treasure = new Treasure();
		treasure.setValue("challenge", result.getString(1));
		treasure.setValue("answer", result.getString(2));
		treasure.setValue("id", result.getInt(3));
		
		return treasure;
		
	}

	@Override
	public boolean insertFoundTreasure(int treasureId, long userId, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection
				.prepareStatement("INSERT INTO user_treasure (userId, treasureId) VALUES (?, ?)");
		
		stmt.setLong(1, userId);
		stmt.setInt(2, treasureId);
		
		stmt.executeUpdate();
			
		replicateData(FunctionCallType.INSERT_FOUND_TREASURE, dbServerHostAddresses, new Object[] {treasureId, userId});
		return true;
	}
	
	@Override
	public boolean insertTreasure(Double latitude, Double longitude, long userCreatorId, String description, String challenge, String challengeSolution, ArrayList<String> dbServerHostAddresses) throws SQLException {
		
		PreparedStatement stmt = connection
				.prepareStatement("INSERT INTO treasure (latitude, longitude, userCreatorId, description, challenge, challengeSolution) VALUES (?, ?, ?, ?, ?, ?)");
		
		stmt.setDouble(1, latitude);
		stmt.setDouble(2, longitude);
		stmt.setLong(3, userCreatorId);
		stmt.setString(4, description);
		stmt.setString(5, challenge);
		stmt.setString(6, challengeSolution);
		
		stmt.executeUpdate();

		replicateData(FunctionCallType.INSERT_TREASURE, dbServerHostAddresses, 
						new Object[] {
							latitude,
							longitude, 
							userCreatorId,
							description,
							challenge,
							challengeSolution
						});

		
		return true;
	}

//	@Override
//    public void newDBServer(String dbServerAddress) throws RemoteException {
//        System.out.println("New DB Server");
//        
//        this.dbServerAddresList.add(dbServerAddress);
//        
//    }
//
//
//	@Override
//	public boolean removeDBServer(String address) {
//	
//		return dbServerAddresList.remove(address);
//	}
	/*
	private DBOperations getDbOperations(String dbServerAddress) throws RemoteException, NotBoundException {
		
		Registry registry = LocateRegistry.getRegistry(
        		dbServerAddress, Registry.REGISTRY_PORT,
				new SslRMIClientSocketFactory());
		
		return (DBOperations) registry.lookup(DB_SERVER_OBJECT_NAME);

	}*/

	private enum FunctionCallType {
		INSERT_USER,
		UPDATE_USER,
		INSERT_FOUND_TREASURE,
		INSERT_TREASURE
	}

	public void replicateData(FunctionCallType functionName, ArrayList<String> dbServerHostAddresses, Object[] args) {
		if(dbServerHostAddresses == null)
			return;
		
		class ReplicateInsertFoundTreasure implements Runnable{

			@Override
			public void run() {
				
				for (int i = 0; i < dbServerHostAddresses.size(); i++) {
					if (!dbServerHostAddresses.get(i).equals(localAddress)) {
						System.out.println("replicating data for " + dbServerHostAddresses.get(i));
						Registry registry;
						try {
							registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,
									new SslRMIClientSocketFactory());
						
							DBOperations remoteObj = (DBOperations) registry.lookup(DB_SERVER_OBJECT_NAME);
							
							switch (functionName) {
							case INSERT_USER:
								
								long userInfoId = (long) args[0];
								String userInfoEmail = (String) args[1];
								String token = (String) args[2];
								String userInfoName = (String) args[3];
								System.out.println("INSERT_USER replicate");
								System.out.println(userInfoId);
								System.out.println(userInfoEmail);
								System.out.println(token);
								System.out.println(userInfoName);

								remoteObj.insertUser(userInfoId, userInfoEmail,  token, userInfoName, null);

								break;
							case UPDATE_USER:
								
								long id = (long) args[0];
								String token2 = (String) args[1];
								System.out.println("UPDATE_USER replicate");
								remoteObj.updateUser(id, token2, null);

								break;
							
							case INSERT_FOUND_TREASURE:
								
								int treasureId2 = (int) args[0];
								long userId = (long) args[1];
								System.out.println("INSERT_FOUND_TREASURE replicate");
								remoteObj.insertFoundTreasure(treasureId2, userId, null);

								break;

							case INSERT_TREASURE:
								System.out.println("INSERT_TREASURE replicate");
								remoteObj.insertTreasure((Double)args[0], (Double)args[1], (long)args[2], 
															(String)args[3], (String)args[4], (String)args[5], null);

								break;

							default:
								break;
							}
							

						} catch (RemoteException | SQLException e) {
//							ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
							e.printStackTrace();

						} catch (NotBoundException e) {
							e.printStackTrace();
						}
						
					}
				}
			}
		}
		
		new Thread(new ReplicateInsertFoundTreasure()).start();

	}

}
