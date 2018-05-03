package communications;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.naming.InvalidNameException;

import util.ParseMessageException;
import util.Utils.Pair;

import org.json.*;

import model.Model;
import model.Model.ModelType;

public class Message {
	
	public static final String RESOURCE_PATH_SEPARATOR = "/"; 

	public static enum MessageType {
		
		CREATE("CREATE"),
		UPDATE("UPDATE"),
		DELETE("DELETE"),
		RETRIEVE("RETRIEVE");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("CREATE","UPDATE","DELETE","RETRIEVE"));
		
		public String description;
		
		MessageType(String description) {
			this.description = description;
		}
		
		static MessageType type(String text) throws ParseMessageException {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return MessageType.CREATE;
				case 1:
					return MessageType.UPDATE;
				case 2:
					return MessageType.DELETE;
				case 3:
					return MessageType.RETRIEVE;
				default:
					throw new ParseMessageException("Invalid Protocol Action");
			
			}
					
		}
				
	}
	
	public static class MessageHeader {
		
		private MessageType messageType;
		private ArrayList<Pair<Model.ModelType,Integer>> resourcePath;
		
		private MessageHeader(MessageType messageType, ArrayList<Pair<Model.ModelType, Integer>> resource) {
			this.messageType = messageType;
			this.resourcePath = resource;
		}
		
		public MessageType getMessageType() {
			return messageType;
		}
		public ArrayList<Pair<Model.ModelType, Integer>> getResource() {
			return resourcePath;
		}
		
	}
	
	private MessageHeader header;
	private JSONObject body;
		
	private Message(MessageHeader header) {
		this.header = header;
	}
	
	private Message(MessageHeader header, JSONObject body) {
		this(header);
		this.body = body;
	}
	
	public MessageHeader getHeader() {
		return header;
	}

	public JSONObject getBody() {
		return body;
	}
	
	/**
	 * Parse a raw message and builds a message object with everything needed 
	 * to identify resource and action to perform
	 *
	 * @param raw String 
	 * @return Message Object
	 * @throws ParseMessageException - Something wrong with message header
	 * @throws JSONException - Something wrong with message body
	 */

	public static Message parseMessage(byte[] raw) throws ParseMessageException, JSONException {
		
		Message message;
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(raw);
		Scanner messageScanner = new Scanner(inputStream);
				
		String messageTypeString = null;
		String resourcePathString = null;
		
		try {
			messageTypeString = messageScanner.next();
			resourcePathString = messageScanner.next();
			
		}
		
		catch(NoSuchElementException e) {
			
			messageScanner.close();
			throw new ParseMessageException("Missing message portions");
			
		}
		
		
		/**
		 * message must have at least 2 parts
		 */
					
		MessageType messageType = MessageType.type(messageTypeString);
		ArrayList<Pair<Model.ModelType,Integer>> pathToResource = parsePath(resourcePathString);
		MessageHeader header = new MessageHeader(messageType, pathToResource);
		
		if(messageType == MessageType.RETRIEVE || messageType == MessageType.DELETE) {
			message = new Message(header);
		}
		
		else {
			
			String jsonString = null;
			
			try {
				jsonString = messageScanner.nextLine();
				
			}
			
			catch(NoSuchElementException e) {
				messageScanner.close();
				throw new ParseMessageException("Missing message portions");
			}
			
			
			JSONObject body = new JSONObject(jsonString);
			message = new Message(header, body);
			
		}
			
		messageScanner.close();
		return message;
		
	}
	
	
	
	
	/**
	 * Parse path location of the message
	 * 
	 * @param path: String representing path location
	 * @return Array with pair resource/id
	 * @throws ParseMessageException
	 */
	
	private static ArrayList<Pair<Model.ModelType,Integer>> parsePath(String path) throws ParseMessageException {
		
		String[] pathPortions = path.split(RESOURCE_PATH_SEPARATOR);
		
		ArrayList<Pair<Model.ModelType,Integer>> result = new ArrayList<>();
		
		int numberPortions = pathPortions.length;
		ModelType modelLevel1 = null;
		ModelType modelLevel2 = null;
		
		try {
			modelLevel1 = ModelType.type(pathPortions[0]);
			modelLevel2 = ModelType.type(pathPortions[2]);
		}
		catch (InvalidNameException e) {
			throw new ParseMessageException("Invalid resource");
		}
		
		catch (ArrayIndexOutOfBoundsException e) {}
		
		
		switch(numberPortions) {
		
			case 1:
				
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, -1));
				break;
				
			case 2:
								
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				break;
				
			case 3:
				
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				result.add(new Pair<Model.ModelType, Integer>(modelLevel2, -1));
				break;
				
			case 4:
								
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				result.add(new Pair<Model.ModelType, Integer>(modelLevel2, Integer.parseInt(pathPortions[3])));
				break;
				
			default:
				throw new ParseMessageException("Invalid resource path: wrong number of levels");
			
		}
				
		return result;
		
		
	}


	
	
	
	
}