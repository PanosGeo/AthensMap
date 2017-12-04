package ergasia1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Mapper extends Thread {
	ObjectInputStream in;
	ObjectOutputStream out;
	String ip = "172.16.1.56";
	String reducer_ip = "172.16.1.65";
	ServerSocket providerSocket = null;
	Socket connection = null;
	int port;

    String jsonResponse;
    
    HashMap<String, String> cache;
    

	public static void main(String args[]) {
		Mapper upper_left = new Mapper(3000);
		upper_left.start();
		Mapper upper_right = new Mapper(4000);
		upper_right.start();
	}
    
	Mapper(int port){
		this.port = port;
		this.cache = new HashMap<String, String>();
	}
	
	Mapper(int port,Socket connection,HashMap<String, String> cache){
		this.port = port;
		this.connection = connection;
		jsonResponse="";
		this.cache = cache;
	}
	
	public long getHashKey(){
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			byte[] hashkey = md.digest((ip+port).getBytes(StandardCharsets.UTF_8));
			ByteBuffer bb = ByteBuffer.wrap(hashkey);
			return bb.getLong();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
		
	}
	
	public void openServer(){
		try {
			providerSocket = new ServerSocket(port);
			System.out.println("Mapper with port " + port + " server open");

			while (true) {
				connection = providerSocket.accept();
				
				new Mapper(this.port,connection,this.cache).start();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void run(){
		if(this.connection ==null){
			openServer();
		} else {
			try {
					ObjectOutputStream out= new ObjectOutputStream(connection.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					
					String key = (String) in.readObject();
					//Pair result = getDirectionsAPIData(key);
					
					if(cache.containsKey(key)){
						System.out.println("getting data from cache for query: " + key); 
						
							sendDataToReducer(new Pair(key,cache.get(key)));
						
					}
					else{
						System.out.println("getting data from API for query: " + key); 
						Pair googleResult = getDirectionsAPIData(key);
						cache.put(key, googleResult.getRight().toString());
						sendDataToReducer(googleResult);
					}
					out.writeObject("sent to reducer");
					out.flush();
										
					in.close();
					out.close();
						
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
			
		}
	}
	
		
	public void sendDataToReducer(Pair result){
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			
			requestSocket = new Socket(reducer_ip,9999);
			
			out= new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
		
		
			out.writeObject(result);
			out.flush();
			
			
			in.close();
			out.close();
			requestSocket.close();

		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} 
		
	}
	

	public String makeHttpRequest(URL url) {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
            try {
				urlConnection = (HttpURLConnection) url.openConnection();
	            urlConnection.connect();
	            inputStream = urlConnection.getInputStream();
	            jsonResponse = readFromStream(inputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return jsonResponse;
    }
	
	public String readFromStream(InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
			try {
				line = reader.readLine();
	            while (line != null) {
	                output.append(line);
	                line = reader.readLine();
	            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return output.toString();
    }
	
	public Pair getDirectionsAPIData(String query){
		String url_maps = "https://maps.googleapis.com/maps/api/directions/json?";
		if(query.contains("),(")){
			url_maps += "&origin=" + query.substring(2,query.indexOf(')'));
			url_maps += "&destination=" + query.substring(query.indexOf('(',2)+1,query.length()-2);
			
		} else {

			url_maps += "&origin=" + query.substring(1,query.indexOf(','));
			url_maps += "&destination=" + query.substring(query.indexOf(',')+1,query.length()-1);
		}
		url_maps +=  "&mode=walking&key=AIzaSyBz8-FmAY0h233BBtM6s6AO792a7Ti2Lpc";
		System.out.println(url_maps);
		try {
			return printLocation(makeHttpRequest(new URL(url_maps)),query);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Pair printLocation(String jason,String query){
			try {
				JSONObject json = new JSONObject(jason);
				String points = "";
				if(json.getString("status").equals("OK")){
					JSONArray jRoutes = json.getJSONArray("routes");
				    JSONObject obj = jRoutes.getJSONObject(0);
					JSONObject polyline = obj.getJSONObject("overview_polyline");
					points = polyline.getString("points");

					System.out.println(points);
					return new Pair(query,points);
				}	
				else {
					return new Pair(query,"error:no directions available");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
			
		}
}
