package ergasia1;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DummyClient {
    String  a, b;
    String [] pair = new String[2];
    String query;
    
    public DummyClient() {
    }


    public void sendRequest() {
        getLocation();
        createQuery();
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {

            requestSocket=new Socket(InetAddress.getByName("127.0.0.1"), 1100);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());


            out.writeObject(query);
            out.flush();

            Pair results = (Pair) in.readObject();

            if(!(results==null)){ System.out.println("Result>"+results.getRight().toString());}

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  finally {
            try {
                in.close();	out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }


    public void getLocation(){
        Scanner reader = new Scanner(System.in);
        System.out.print("Enter origin: ");
        pair[0] = getLocationInfo(reader.nextLine());
        System.out.print("Enter destination: ");
        pair[1] = getLocationInfo(reader.nextLine());
        reader.close();
    }

    public void createQuery(){
        if(pair[0].length()!=5){
            this.query = "(" + "(" + pair[0] + ")" + "," + "(" + pair[1] + ")" + ")";

        } else {
            this.query = "(" + pair[0] + "," + pair[1] + ")";

        }
    }


    public static String getLocationInfo(String line){
        if(line.length()!=5){
            return line;
        } else {
            return "GR-" + line;
        }
    }

    public static void main(String args[]) {
        new DummyClient().sendRequest();
    }
}
