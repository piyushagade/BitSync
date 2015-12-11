import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	public static String devId;
	String[] availableChunks, chunkOwnedArray;
	String intTo, filename, connTo;
	Object[] objectMessage;
	public static int sPort;    //This as a server will be listening on this port number
	int cPort;    //Port through which messages are exchanged
	public static int[] sPortArray = {18121,18122,18123,18124,18125};
	int[] cPortArray = {18221,18222,18223,18224,18225};
	String[] folderNameArray = {"Client2","Client3","Client4","Client5","Client6"};
	String folderName;
	final static int noDev=5;
	static long fileSize;
	String sleep="0";

	//************Peer-to-peer*******//
	ServerSocket serverSocket;
	Socket sSocket;
	public static String[] row;

	public void Client() {}

	public void setIdentity(String devId){
		sPort = sPortArray[Integer.parseInt(devId)-2];
		cPort = cPortArray[Integer.parseInt(devId)-2];
		folderName = folderNameArray[Integer.parseInt(devId)-2];
	}

	void run()
	{
		String chunkId;
		try{
			//create a socket to connect to the server
			requestSocket = new Socket("localhost", 12160);
			System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream

			out = new ObjectOutputStream(requestSocket.getOutputStream());

			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			int k=0;

			do{
				// Sleep
				try {
					Thread.sleep(Integer.valueOf(sleep));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				chunkId=xPayload();

				if((Integer.parseInt(chunkId)-Integer.parseInt(devId)+2)%noDev==0&&Integer.parseInt(chunkId)!=-1){

					if(k==0){
						System.out.println("Receiving files from Server; Please wait.");
						k=1;
					}

					// Write file
					String currentDir=System.getProperty("user.dir");
					//fileDest = currentDir+"/src/"+folderName+"/"+filename+".chunk"+chunkId;		// Change Client Folder identifier
					String fileDest = currentDir+"/"+folderName+"/"+filename+".chunk"+chunkId;		// Change Client Folder identifier


					byte[] myByteArray = new byte[102400];
					InputStream is = requestSocket.getInputStream();

					int bytesRead = is.read(myByteArray, 0, myByteArray.length);
					int   current = bytesRead;
					//					System.out.println(fileSize);

					if((fileSize-65536>0)&&Integer.parseInt(chunkId)!=-1){ // if last chunk is smaller than 65536 Bytes
						do {
							bytesRead = is.read(myByteArray, current, myByteArray.length - current);
							if (bytesRead >= 0) current += bytesRead;
							//} while (bytesRead > -1);
						} while (current != (int)fileSize);
					}

					FileOutputStream fos = new FileOutputStream(fileDest);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					bos.write(myByteArray, 0, (int)fileSize);
					bos.flush();
				}

				if(Integer.valueOf(chunkId)==-1){
					System.out.println("All chunks have be received from Server.");
				}

			}while(Integer.valueOf(chunkId)!=-1);
		}
		catch (ConnectException e) {
			System.err.println("Initiate a server first.");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				if(in!=null) in.close();
				if(out!=null) out.close();
				if(requestSocket!=null) {
					requestSocket.close();
				}
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}
	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
			System.out.println("Send message: " + msg);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}


	// Exchange chunkId & chunkList & devId
	String xPayload(){
		try { 

			// Write Object
			Object[] payload={devId, chunkOwnedArray};
			out.writeObject(payload);

			// Read Object
			objectMessage = (Object[]) (in).readObject();
			connTo=(String) objectMessage[1];			//connected device's Id
			availableChunks=(String[]) objectMessage[2];
			filename = (String) objectMessage[3];
			fileSize = (long) objectMessage[4];

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (ConnectException e) {
			System.err.println("Control Connection refused. Initiate a server first.");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		return (String) objectMessage[0];			//return chunkId
	}

	// List chunks in possession
	public void chunkOwned(){
		//Look for chunks
		String currentDir=System.getProperty("user.dir");
		//		File folder = new File(currentDir+"/src/"+folderName);
		File folder = new File(currentDir+"/"+folderName);
		File[] listOfFiles = folder.listFiles();
		int fileCount=0;

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()&&listOfFiles[i].getName().toLowerCase().contains("chunk")) {
				fileCount=fileCount+1;

			}
		}
		String[] chunkOwnedArray1= new String[fileCount];
		fileCount=0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()&&listOfFiles[i].getName().toLowerCase().contains("chunk")) {
				String chunkName=listOfFiles[i].getName().toString();
				String chunkIdOwned=chunkName.substring(chunkName.lastIndexOf('.') + 6);
				chunkOwnedArray1[fileCount]=chunkIdOwned;
				fileCount++;
			}
		}
		chunkOwnedArray=chunkOwnedArray1;
	}

	//Config reader
	public String[] configReader(){
		BufferedReader br = null;
		String line = null;
		String[] row = {"-1","-1","-1"};

		try {
			String currentDir=System.getProperty("user.dir");
			br = new BufferedReader(new FileReader(currentDir+"/config.txt"));
			while ((line = br.readLine()) != null) {
				String[] data = line.split(",");
				if(data[0].equals(devId))
					row=data;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return row;
	}

	// Main method
	public static void main(String args[])
	{	
		Client client = new Client();

		if(args.length==1||args.length==2){
			client.devId = args[0];
			if(args.length==2)client.sleep = args[1];
			client.setIdentity(client.devId);
			client.row=client.configReader();
			client.chunkOwned();
			client.run();
		}
		else{
			System.out.println("Please put appropriate arguments.");
			System.exit(1);
		}

		System.out.println("Client "+client.devId+" entered peer-to-peer sharing mode.");



			// As a client, downloads from
			new Thread(new PeerClient()).start();


			// As a server, uploads to
			new Thread(new PeerServer()).start();

			
		
	}
}

class PeerServer implements Runnable {
	Socket sSocket;
	ServerSocket serverSocket;

	@Override
	public void run() {
		System.out.println(Client.row[1]);
		System.out.println("PeerServer thread running.");
		try {
			serverSocket = new ServerSocket(Client.sPort, 10);
			System.out.println("Waiting for "+Client.row[1]+".");
			sSocket = serverSocket.accept();
			System.out.println("Acting as a server to "+Client.row[1]+".");

			while(true){
				try {
					ObjectOutputStream out = new ObjectOutputStream(sSocket.getOutputStream());
					String msg="Message from "+Client.devId;
					out.writeObject(msg);
					out.flush();
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	} catch (IOException e) {
		e.printStackTrace();
	}finally{
		try {
			sSocket.close();
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
}

class PeerClient implements Runnable {
	Socket cSocket;
	@Override
	public void run() {

		System.out.println(Client.row[2]);
		System.out.println("PeerClient thread running.");
		do{
			try {
				Thread.sleep(3000);	// Retry Timeout
				cSocket = new Socket("localhost", Client.sPortArray[Integer.valueOf(Client.row[2])-2]);
			
			
			System.out.println("Connection accepted by "+Client.row[2]+".");
			
			while(true){
				try {
					ObjectInputStream in = new ObjectInputStream(cSocket.getInputStream());
					String msg=(String) in.readObject();
					System.out.println(msg);
					Thread.sleep(5000);
				} catch (InterruptedException | ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (java.net.ConnectException e) {
			System.out.println("Connection refused. Retrying in 3s, please wait.");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				if(cSocket!=null)cSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		}while(cSocket==null);

	}
}

