package ergasia1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONArray;

public class Reducer extends Thread {

	ObjectInputStream in;
	ObjectOutputStream out;
	
	ServerSocket providerSocket = null;
	Socket connection = null;
	int port;

	String ip = "172.16.1.65";
	String ip_master = "172.16.1.67";
	
    String jsonResponse;
	
    public static void main(String args[]) {
		Reducer reduc = new Reducer(9999);
		reduc.start();
	}
    
    Reducer(int port){
		this.port = port;
	}
	
    Reducer(int port,Socket connection){
		this.port = port;
		this.connection = connection;
		jsonResponse="";
	}
    
    public void openServer(){
		try {
			providerSocket = new ServerSocket(port);
			System.out.println("Reducer with port " + port + " server open");

			while (true) {
				connection = providerSocket.accept();
				
				new Reducer(this.port,connection).start();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void run(){
		if(this.connection == null){
			openServer();
		} else {
			try {
					ObjectOutputStream out= new ObjectOutputStream(connection.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					
					Pair result = (Pair)in.readObject();
					
					sendResultsToMaster(result);
					
					in.close();
					out.close();
						
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public synchronized void sendResultsToMaster(Pair response){
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			
			requestSocket=new Socket(InetAddress.getByName(ip_master), 8524);
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
			

			out.writeObject(response);
			out.flush();
				
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}  
//		finally {
//			try {
//				in.close();	out.close();
//				requestSocket.close();
//			} catch (IOException ioException) {
//				ioException.printStackTrace();
//			}
//		}
	}

}
