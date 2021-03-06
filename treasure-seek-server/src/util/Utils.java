package util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import org.json.JSONException;

import communications.ReplyMessage;


public class Utils {

	public static class Pair<K, V> implements Serializable {

		private static final long serialVersionUID = -4589969236014340084L;
		public K key;
		public V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.value);
		}

		@Override
		public boolean equals(Object obj) {
			@SuppressWarnings("unchecked")
			Pair<K, V> pair2 = (Pair<K, V>) obj;

			return this.key.equals(pair2.key) && this.value.equals(pair2.value);
		}

	}

	private static final int TIME_OUT = 4000;

	public static void setSecurityProperties(boolean debugEclipse) {
		
		String keyStorePath = debugEclipse ? "security/keys/keystore" : "../security/keys/keystore";
		String trustStorePath = debugEclipse ? "security/certificates/truststore" : "../security/certificates/truststore";
		
		String password = "123456";
		System.setProperty("javax.net.ssl.keyStore", keyStorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", password);

		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", password);

	}
	
	public static String bindParamenter(String[] args, String prefix, String alternative, String usage) {

		int index = Arrays.asList(args).indexOf(prefix);
		if(index != -1) {
			try {
				if(args[index + 1].charAt(0) == '-') {
					System.out.println(usage);
					System.exit(1);
				}
				return args[index + 1];				
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(usage);
				System.exit(1);
			}
		}
		else if(alternative == null){
			System.out.println(usage);
			System.exit(1);
		}
		
		return alternative;
	}
	
	public static ArrayList<String> bindMultiParamenter(String[] args, String prefix, ArrayList<String> alternative, String usage) {
		int index = Arrays.asList(args).indexOf(prefix);
		
		if(index != -1) {
			ArrayList<String> result = new ArrayList<>();
			try {
				int i = 1;
				while(args[index + i].charAt(0) != '-') {
					result.add(args[index + i]);
					i++;
					
				}
				return result;				

			} catch (ArrayIndexOutOfBoundsException e) {
				if(result.size() == 0) {
					System.out.println(usage);
					System.exit(1);
				}
				else {
					return result;
				}
			}
		}
		else if(alternative == null){
			System.out.println(usage);
			System.exit(1);
		}
		
		return alternative;
	}

	public static String squaredFrame(Object object) {
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(outStream);
		
		String text = object.toString();
		out.print("\n +");
		for (int i = 0; i < text.length() + 2; i++) {
			out.print("-");
		}
		out.println("+");
		
		out.println(" | " + text + " |");
		
		out.print(" +");
		for (int i = 0; i < text.length() + 2; i++) {
			out.print("-");
		}
		out.println("+");
		
		out.close();
		
		return outStream.toString();
	}

    public static byte[] readFile(File file) throws IOException{

        byte[] buffer = new byte[(int) file.length()];
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            if (stream.read(buffer) == -1) {
                throw new IOException("EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
        return buffer;
    }
    
    public static ReplyMessage sendMessage(String message, Socket socket) throws IOException, ParseMessageException, JSONException {
		
		PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
        ReplyMessage response = null;
        
        pw.println(message);
        socket.setSoTimeout(TIME_OUT);
        
        if(scanner.hasNextLine())
            response = ReplyMessage.parseServerMessage(scanner.nextLine());
        
        scanner.close();
        pw.close();
        return response;
		
	}
}
