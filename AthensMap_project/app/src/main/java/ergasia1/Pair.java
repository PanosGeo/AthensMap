package ergasia1;

import java.io.Serializable;

public class Pair implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5583646493600018722L;
	private String left;
	//private transient JSONArray right;
	private String right;
	
	public Pair(String left, String right) {
	  this.left = left;
	  this.right = right;
	}
	
	public String getLeft() { return left; }
	public String getRight() { return right; }
	
//	private void writeObject(ObjectOutputStream oos) throws IOException {
//        oos.defaultWriteObject();
//        oos.writeObject(this.right.toString());
//    }

//    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException, JSONException {
//        ois.defaultReadObject();
//        this.right = new JSONArray((String)ois.readObject());
//    }
}
