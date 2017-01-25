import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * 
 * @author Talha
 * This a server side program.
 * The communication's protocol is defined as follows:
 * The server will run on the host on attu2.cs.washington.edu, listening for incoming packets on UDP port 12235. 
 * The server expects to receive and will only send:
 * Payload that has a header (see below)
 * Data in network-byte order (big-endian order)
 * 4-byte integers that are unsigned (uint32_t), or 2-byte integers that are unsigned (uint16_t)
 * Characters that are 1-byte long (Note: in Java a char is 2-byte long)
 * Strings that are a sequence of characters ending with the character '\0'
 * Packets that are aligned on a 4-byte boundary (that is, a packets must be padded until its length is divisible by 4)
 * The server will close any open sockets to the client and/or fail to respond to the client if:
 * unexpected number of buffers have been received
 * unexpected payload, or length of packet or length of packet payload has been received
 * The server does not receive any packets from the client for 3 seconds
 *
 */
public class server {
	static ArrayList<Integer> clients =new ArrayList<Integer>(); //This list can be used to store information about connected clients.
	static DatagramSocket serverSocket1; //Clients will connect to this socket to start the process.
	

	public class EchoThread extends Thread {
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
	    protected DatagramSocket socket; //UDP socket.
		protected InetAddress IPAddress = null ;
		protected int port= 0;
		int TIMEOUT = 3000;// 3 seconds
		//Initialize
	    public EchoThread(DatagramSocket clientSocket,InetAddress IPAddress,int port,byte[] receiveData) {
	        this.socket = clientSocket;
	        this.IPAddress=IPAddress;
	        this.port=port;
	        this.receiveData=receiveData;
	    }

	    public void run() {
			byte[] stageA = new byte[24];
			//StageA holds what is expected to receive in stageA.
			stageA[0]=0;stageA[1]=0;stageA[2]=0;stageA[3]=12; //Payload length
			stageA[4]=0;stageA[5]=0;stageA[6]=0;stageA[7]=0;
			stageA[8]=0;stageA[9]=1;stageA[10]=0;stageA[11]=(byte)197;
			stageA[12]='h';stageA[13]='e';stageA[14]='l';stageA[15]='l';
			stageA[16]='o';stageA[17]=' ';stageA[18]='w';stageA[19]='o';
			stageA[20]='r';stageA[21]='l'; stageA[22]='d';stageA[23]='\0';//Message to be sent
			int num=0;int udp_port=0;int len=0;int secretA=0;
			Random r = new Random();
			
	    	boolean isStageACorrect= true;//If stageA array is matched with the received array this stays true.
			//Check if the received array is correct.
	    	for(int i = 0; i <24;i++){
				if(receiveData[i]!=stageA[i]) isStageACorrect=false;
			}
	    	//If there is an incorrect value in the received array then stop the connection.
			if(!isStageACorrect){
				this.stop();
			}
			num = r.nextInt(60)+1;
			len = r.nextInt(60)+1;
			udp_port =r.nextInt(40000)+2000;
			secretA = r.nextInt(80)+1;
			ByteBuffer dbuf = ByteBuffer.allocate(4);
			byte[] bytes=dbuf.array();
			dbuf = ByteBuffer.allocate(4);
			dbuf.putInt(udp_port);
			bytes = dbuf.array();
			sendData[0]=0;sendData[1]=0;sendData[2]=0;sendData[3]=16; //Payload length
			sendData[4]=0;sendData[5]=0;sendData[6]=0;sendData[7]=0; //Secret code
			sendData[8]=0;sendData[9]=2;sendData[10]=0;sendData[11]=(byte)197; //Step number and last 3 digits of student id(2 byte for each)
			sendData[12]=0;sendData[13]=0;sendData[14]=0;sendData[15]=(byte)num;//Message to be sent
			sendData[16]=0;sendData[17]=0;sendData[18]=0;sendData[19]=(byte)len;
			for(int i=20;i<24;i++) sendData[i]=bytes[i-20]; //upd port
			sendData[24]=0;sendData[25]=0; sendData[26]=0;sendData[27]=(byte)(secretA);//Message to be sent
			DatagramPacket sendPacket =
					new DatagramPacket(sendData, sendData.length, IPAddress, port);
			try {
				socket.send(sendPacket); //Send packet to the client
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				socket= new DatagramSocket(udp_port); //open new udp port
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int packet_id =0;
			boolean isStageBOkay =true;
			for(int i=12;i<1024;i++) sendData[i]=0;


			for(int step= 0;  step <num;){
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				try {
					socket.setSoTimeout(TIMEOUT);
					socket.receive(receivePacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Client hasn't replied in "+ TIMEOUT+" seconds");
					e.printStackTrace();
				}
				int canAck = r.nextInt(2);
				if(canAck==0)continue;
				step++;
				byte[] expectedPayload2 =new byte[12];
				for(int i = 0; i < 3; i++) expectedPayload2[i]=0;
				expectedPayload2[3]=(byte)(len+4);
				for(int i = 4;i<7;i++) expectedPayload2[i]=0;
				expectedPayload2[7]=(byte)secretA;
				expectedPayload2[8]=0;expectedPayload2[9]=1; expectedPayload2[10]=0;expectedPayload2[11]=(byte)197;

				for(int i = 0 ;i <12;i++){

					if(expectedPayload2[i] != receiveData[i]){
						isStageBOkay=false;
						break;
					}
				}
				byte packet_id_byte[] = Arrays.copyOfRange(receiveData, 12, 16);
				ByteBuffer wrapped = ByteBuffer.wrap(packet_id_byte);
				int received_packet_id =wrapped.getInt();
				if(received_packet_id!=packet_id){
					isStageBOkay=false;
					break;
				}
				byte rest_byte[] =Arrays.copyOfRange(receiveData, 16, 16+len);
				for(int i = 0;i<len;i++){
					if(rest_byte[i]!=0){
						isStageBOkay=false;
						break;
					}
				}
				packet_id++;
				sendData[4]=0;sendData[5]=0;sendData[6]=0;sendData[7]=(byte)(secretA);
				for(int i=12;i<16;i++) sendData[i]=packet_id_byte[i-12]; 
				sendPacket =
						new DatagramPacket(sendData, sendData.length, IPAddress, port);
				try {
					socket.send(sendPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
			int secretB = r.nextInt(150)+1;
			int tcp_port = r.nextInt(30000)+2000;
			dbuf = ByteBuffer.allocate(4);
			bytes=dbuf.array();
			dbuf = ByteBuffer.allocate(4);
			dbuf.putInt(tcp_port);
			bytes = dbuf.array();
			sendData[3]=8;
			for(int i = 12;i<16;i++) sendData[i]=bytes[i-12];
			for(int i = 16;i<19;i++) sendData[i]=0;
			sendData[19]=(byte) secretB;

			if(isStageBOkay){
				sendPacket =
						new DatagramPacket(sendData, sendData.length, IPAddress, port);
				try {
					socket.send(sendPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				this.stop();
			}
			ServerSocket socket2=null;
			try {
				socket2 = new ServerSocket(tcp_port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Socket connectionSocket =null;
			try {
				
				connectionSocket = socket2.accept();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("Client hasn't replied in "+ TIMEOUT+" seconds");
				e1.printStackTrace();
			}
			DataOutputStream outToClient = null;
			try {
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int num2 = r.nextInt(100)+1;
			int len2 =r.nextInt(100)+1;
			
			int secretC = r.nextInt(100)+1;
			byte charC = (byte)(r.nextInt(100)+1);
			sendData[0]=0;sendData[1]=0;sendData[2]=0;sendData[3]=16; //Payload length
			sendData[4]=0;sendData[5]=0;sendData[6]=0;sendData[7]=(byte)secretB; //Secret code
			sendData[8]=0;sendData[9]=2;sendData[10]=0;sendData[11]=(byte)197; //Step number and last 3 digits of student id(2 byte for each)
			sendData[12]=0;sendData[13]=0;sendData[14]=0;sendData[15]=(byte)num2;//Message to be sent
			sendData[16]=0;sendData[17]=0;sendData[18]=0;sendData[19]=(byte)len2;
			sendData[20]=0;sendData[21]=0;sendData[22]=0;sendData[23]=(byte)secretC;
			for(int i = 24;i<28;i++)sendData[i]=charC;
			try {
				outToClient.write(sendData);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int padZero =(len2%4==0)? 0:4-len2%4;
			byte[] expectedD = new byte[12+len2+padZero];
			expectedD[0]= 0;expectedD[1]=0;expectedD[2]=0;expectedD[3]=(byte)(len2);
			expectedD[4]=0;expectedD[5]=0;expectedD[6]=0;expectedD[7]=(byte)secretC;
			expectedD[8]=0;expectedD[9]=1;expectedD[10]=0;expectedD[11]=(byte)197;
			for(int i = 12;i<12+len2;i++) expectedD[i]=charC;
			for(int i = 12+len2;i<12+len2+padZero;i++) expectedD[i]=0;
			DataInputStream dIn = null;
			try {
				dIn = new DataInputStream(connectionSocket.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean isDOkay=true;
			int total=0;
			int expectedBytes=num2*(12+len2+padZero);
			int index = 0;
			while(total<expectedBytes){
					byte cur = 0;
					try {
						
						cur = dIn.readByte();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(cur!=expectedD[index]){
						isDOkay=false;
						break;
					}
					if(!isDOkay)break;
					index++;
					index%=(12+len2+padZero);
					total++;
					
			}
			int secretD = r.nextInt(100)+1;
			sendData[0]=0;sendData[1]=0;sendData[2]=0;sendData[3]=4;
			sendData[4]=0;sendData[5]=0;sendData[6]=0;sendData[7]=(byte)secretC;
			sendData[8]=0;sendData[9]=2;sendData[10]=0;sendData[11]=(byte)197;
			sendData[12]=0;sendData[13]=0;sendData[14]=0;sendData[15]=(byte)secretD;
			for(int i =16;i<1024;i++) sendData[i]=0;
			if(isDOkay){
				try {
					outToClient.write(sendData);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}else {
				
			}
			this.stop();
			 
	    }
	}
	public static void main(String args[]) throws IOException{
		server x = new server();
       	serverSocket1= new DatagramSocket(12235);
        while (true) {
        	byte[] receiveData = new byte[1024];
        	InetAddress IPAddress=null;
        	int port =0;
            try {
 
            	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    			serverSocket1.receive(receivePacket);
    			IPAddress = receivePacket.getAddress() ;
    			port= receivePacket.getPort();
            } catch (IOException e) {
                System.out.println("I/O errror: " + e);
            }
            // new thread for a client
            x.new EchoThread(serverSocket1,IPAddress,port,receiveData).start();
        }
		 
		
	}
}
