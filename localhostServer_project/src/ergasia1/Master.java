
package ergasia1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Master extends Thread {
		ObjectInputStream in;
		ObjectOutputStream out;
		
		ServerSocket providerSocket;
		ServerSocket reducerSocket;
		Socket connection = null;
		String query;
		private static Object sharedLock = new Object(); // Any object's instance can be used as a lock.
	

		String[] mappers = new String[4];
		String _reducer;
		ArrayList<Pair> master_cache;
		Integer oldest_cached_index; 
		
		Master(String mappers0,String mappers1,String mappers2,String mappers3,String red){
			this.mappers[0] = mappers0;
			this.mappers[1] = mappers1;
			this.mappers[2] = mappers2;
			this.mappers[3] = mappers3;
			_reducer=red;
			master_cache = new ArrayList<Pair>(100);
			oldest_cached_index=0;
		}

		public static void main(String args[]) {
			new Master("172.16.1.66","172.16.1.66","172.16.1.56","172.16.1.56","172.16.1.65").start();
		}
		
		Master(Socket connection,String[] mappers, String red,ArrayList<Pair> cache,Integer index){
			this.mappers[0] = mappers[0];
			this.mappers[1] = mappers[1];
			this.mappers[2] = mappers[2];
			this.mappers[3] = mappers[3];
			_reducer=red;
			this.connection = connection;
			this.master_cache = cache;
			this.oldest_cached_index = index;

			try {
				out= new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void openServer() {
			try {
				providerSocket = new ServerSocket(1100);
				System.out.println("Master server open on port 1100");
				
				while(true){
					
					connection = providerSocket.accept();
					
					
					new Master(connection,mappers,_reducer, this.master_cache,this.oldest_cached_index).start();
					
					
				}

			} catch (IOException ioException) {
				ioException.printStackTrace();
			} finally {
				try {
					providerSocket.close();
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
		
		
		public void run(){
			
			if(this.connection == null){
				openServer();
			} else {
				try {
					
					query = (String)in.readObject();
					//System.out.println("exists:"+master_cache.get(0).getLeft() + "query is" + query);
					Pair result;
					
					result = checkCache(query);
					
					if(result==null){
						System.out.println("Not found in master cache");
						String to_mapper = findMapper(getQueryHash(query));
						
						sendToMapper(query,to_mapper.substring(0, 4),to_mapper.substring(4));
						
						
						result = collectDataFromReducer();
						
						writeToMasterCache(result);
					} else {
						System.out.println("Found in master cache");
					}
					out.writeObject(result);
					out.flush();
					in.close();
					out.close();
											
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					try {
						connection.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}
		}
		
		public Pair checkCache(String query){
			ArrayList<Pair> filtered =  master_cache.stream()
			        .filter(p -> p.getLeft().equals(query))
			        .collect(Collectors.toCollection(ArrayList<Pair>::new));
			System.out.println("query is" + query);
			if(filtered.isEmpty()){
				return null;
			} else {
				return filtered.get(0);
			}
		}
		
		public void writeToMasterCache(Pair obj){
			if(master_cache.size()<100){
				master_cache.add(obj);
			} else {
				master_cache.remove(oldest_cached_index);
				master_cache.add(oldest_cached_index, obj);
				oldest_cached_index++;
				if(oldest_cached_index>99){
					oldest_cached_index=0;
				}
			}
		}
		
		public String  findMapper(long queryHash){
		
				ArrayList<Long> mappers_hashes = new ArrayList<Long>();
				for(int i = 0; i < 4;i++){
					int port_f = i + 1;
					String port = port_f + "000";
					mappers_hashes.add(getHashKey(mappers[i],port));
				}
				Collections.sort(mappers_hashes);
				if(queryHash<=mappers_hashes.get(0)){
					return 1000 + mappers[0];
				}else if(queryHash<=mappers_hashes.get(1)){
					return 2000 + mappers[1];
				}else if(queryHash<=mappers_hashes.get(2)){
					return 3000 + mappers[2];
				}else if(queryHash<=mappers_hashes.get(3)){
					return 4000 + mappers[3];
				}else {
					return findMapper(queryHash%mappers_hashes.get(3));
				}
		}
		
		public long getHashKey(String ip,String port){
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

		public long getQueryHash(String query){
				try {
					MessageDigest md;
					md = MessageDigest.getInstance("MD5");
					byte[] hashkey;
					if(query.contains("),(")){
						hashkey = md.digest((query.substring(2,query.indexOf(')'))+query.substring(query.indexOf('(',2)+1,query.length()-2)).getBytes(StandardCharsets.UTF_8));
					} else {
						hashkey = md.digest((query.substring(1,query.indexOf(','))+query.substring(query.indexOf(',')+1,query.length()-1)).getBytes(StandardCharsets.UTF_8));
					}
					ByteBuffer bb = ByteBuffer.wrap(hashkey);
					return bb.getLong();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;
		}
		
		public void sendToMapper(String message,String to_port, String ip){
			Socket requestSocket = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			String result = null ;
			try {
				System.out.println("For '" + message + "' connecting to mapper with port: " + Integer.parseInt(to_port));
				requestSocket = new Socket(ip,Integer.parseInt(to_port));
				
				out= new ObjectOutputStream(requestSocket.getOutputStream());
				in = new ObjectInputStream(requestSocket.getInputStream());
			
			
				out.writeObject(message);
				out.flush();
				

				while(!((String)in.readObject()).equals("sent to reducer")){
					continue;
				}
				
				in.close();
				out.close();
				requestSocket.close();

			} catch (UnknownHostException unknownHost) {
				System.err.println("You are trying to connect to an unknown host!");
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
		
		public Pair collectDataFromReducer(){
			synchronized (sharedLock) {
				Socket connection = null;
				Pair result = null;
				ServerSocket reducerSocket;
				try {
					reducerSocket = new ServerSocket(8524);
						
						while(true){
							connection = reducerSocket.accept();
							
							ObjectOutputStream out= new ObjectOutputStream(connection.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
						
							result = (Pair)in.readObject();

							in.close();
							out.close();
							break;
						}	
						connection.close();
						reducerSocket.close();

				} catch (IOException ioException) {
					ioException.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} 
				return result;	
			}
			
		}

}