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
	public static String[] availableChunks, chunkOwnedArray;
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
	Functions f;

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
				Object[] rPayload=f.xPayloadClient(devId, chunkOwnedArray, out, in);
				chunkId = (String) rPayload[0];
				connTo=(String) rPayload[1];			//connected device's Id
				availableChunks=(String[]) rPayload[2];
				filename = (String) rPayload[3];
				fileSize = (long) rPayload[4];
				
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




	
	// Main method
	public static void main(String args[])
	{	
		Client client = new Client();
		client.f = new Functions();

		if(args.length==1||args.length==2){
			client.devId = args[0];
			if(args.length==2)client.sleep = args[1];
			client.setIdentity(client.devId);
			client.row=client.f.configReader(devId);
			chunkOwnedArray=client.f.chunkOwned(client.folderName);
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