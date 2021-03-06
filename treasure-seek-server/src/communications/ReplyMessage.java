package communications;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;

import util.ParseMessageException;

public class ReplyMessage {

	public static enum ReplyMessageStatus {
		
		OK("OK"),
		UNAUTHORIZED("UNAUTHORIZED"),
		RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
		BAD_REQUEST("BAD_REQUEST");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("OK","UNAUTHORIZED","RESOURCE_NOT_FOUND","BAD_REQUEST"));
		
		public String description;
		
		ReplyMessageStatus(String description) {
			this.description = description;
		}
		
		static ReplyMessageStatus type(String text) throws ParseMessageException {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return ReplyMessageStatus.OK;
				case 1:
					return ReplyMessageStatus.UNAUTHORIZED;
				case 2:
					return ReplyMessageStatus.RESOURCE_NOT_FOUND;
				case 3:
					return ReplyMessageStatus.BAD_REQUEST;
				default:
					throw new ParseMessageException("Invalid Protocol Action");
			
			}
					
		}
				
	}

	
	public static String buildResponseMessage(ReplyMessageStatus status) {
		
		return status.description;
	
	}
	
	public static String buildResponseMessage(ReplyMessageStatus status, JSONArray jsonBody) throws JSONException {
				
		return status.description + " " + jsonBody.toString();
		
	}
	
	public static ReplyMessageStatus parseResponse(String raw) throws ParseMessageException {
		
		return ReplyMessageStatus.type(raw);
		
	}
	
	private ReplyMessageStatus status;
    private JSONArray body;

    public ReplyMessage(ReplyMessageStatus status) {
        this.status = status;
    }

    private void setBody(JSONArray body) {
        this.body = body;
    }


    public ReplyMessageStatus getStatus() {
        return status;
    }

    public JSONArray getBody() {
        return body;
    }

    public static ReplyMessage parseServerMessage(String raw) throws ParseMessageException, JSONException {

        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(raw.getBytes()))));

        if(!scanner.hasNext()) {
        	
        		scanner.close();
        	 	throw new ParseMessageException("No message status");
        }
           

        ReplyMessage message = new ReplyMessage(ReplyMessageStatus.type(scanner.next()));

        if(scanner.hasNextLine())
            message.setBody(new JSONArray(scanner.nextLine()));

        scanner.close();

        return message;

    }

    @Override
    public String toString() {

        String res = "";

        res += "\nMessage Header:";
        res += ("\n\tMessage Type: " + this.getStatus());

        res += "\nMessage Body: \n";

        if(this.body == null)
            res += "\tNo body";
        else
            res += "\t" + this.body.toString();

        return res;

    }
	
	
	
}
