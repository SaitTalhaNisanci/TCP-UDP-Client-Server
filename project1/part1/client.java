package client;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * 
 * @author Talha
 * This program communicates with a server on attu2.cs.washington.edu
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
 */
public class client {
    static byte[] sendData = new byte[1024];//The data to be sent to the server.
    static byte[] receiveData = new byte[1024];//The data to be received by the server.
    static byte[] sendData2 = new byte[1024];//The data to be sent to the server.
    static DatagramSocket clientSocket;
    static InetAddress IPAddress;
    static DatagramPacket receivePacket;
    static InputStream is;
    static Socket clientSocketTCP =null;
    static OutputStream out;
	public static void stepA(){
	    sendData[0]=0;sendData[1]=0;sendData[2]=0;sendData[3]=12; //Payload length
	    sendData[4]=0;sendData[5]=0;sendData[6]=0;sendData[7]=0; //Secret code
	    sendData[8]=0;sendData[9]=1;sendData[10]=0;sendData[11]=(byte)197; //Step number and last 3 digits of student id(2 byte for each)
	    sendData[12]='h';sendData[13]='e';sendData[14]='l';sendData[15]='l';//Message to be sent
	    sendData[16]='o';sendData[17]=' ';sendData[18]='w';sendData[19]='o';//Message to be sent
	    sendData[20]='r';sendData[21]='l'; sendData[22]='d';sendData[23]='\0';//Message to be sent
	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 12235); //build the package to send to the server from port 12235
	    try {
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//Send the package
	    receivePacket = new DatagramPacket(receiveData, receiveData.length); //Build the package to receive from the server
	    try {
			clientSocket.receive(receivePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//Receive from the server.
	}
	public static void stepB(int numValue,int udp_portValue) throws IOException{
	    int packet_id= 0;
	    ByteBuffer dbuf = ByteBuffer.allocate(4);
	    byte[] bytes=dbuf.array();
	    for(int i = 0;i<numValue;i++,packet_id++){
	    	dbuf = ByteBuffer.allocate(4);
	    	dbuf.putInt(packet_id);
	    	bytes = dbuf.array();
	    	for(int k= 0 ;k <4;k++){
	    		sendData2[12+k]=bytes[k];
	    	}
	    	for(int k =16; k <20;k++){
	    		sendData2[k]=0;
	    	}
	    	DatagramPacket sendPacket2 = new DatagramPacket(sendData2, sendData2.length,IPAddress,udp_portValue);
		    try {
				clientSocket.setSoTimeout(800);
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	while(true){
	    		try{
	    		clientSocket.send(sendPacket2);
	    		}catch(SocketException e){
	    			System.out.println("Exception "+e);
	    		}
	    		try{
			    	
				    receivePacket = new DatagramPacket(receiveData, receiveData.length);
				    clientSocket.receive(receivePacket);
				    break;
	    		}catch(SocketTimeoutException e){
	    			System.out.println("Timeout Reached"+e);
	    		}

	    	}

		    
	    }
	    clientSocket.receive(receivePacket);
	}
	public static void stepC(int tcp_port) throws IOException{

	    try{
		    clientSocketTCP= new Socket("attu2.cs.washington.edu",tcp_port); //Open a tcp connection to the server using tcp_port
	    }catch (IOException e){
	    	System.out.println(e);
	    }

	    is =clientSocketTCP.getInputStream();
	}
	public static void stepD(int num2,byte[] header,byte[] sendData3) throws IOException{
	    out = clientSocketTCP.getOutputStream();
	    for(int i = 0 ; i <num2;i++){
	    	out.write(header);
	    	out.write(sendData3);
	    }
	    is.read(receiveData);
	}
	public abstract class Upw{
		
	}
	public static void main(String args[]) throws IOException{
	
		clientSocket = new DatagramSocket(); //UDP connection with server.
		IPAddress = InetAddress.getByName("attu2.cs.washington.edu");//Server's IpAddress based on the host name.
		stepA();
		//This part is to convert the receivedData-------------------------
	    byte num[] = Arrays.copyOfRange(receiveData, 12, 16);
	    byte len[] = Arrays.copyOfRange(receiveData, 16, 20);
	    byte udp_port[] = Arrays.copyOfRange(receiveData, 20, 24);
	    byte secretA[] =Arrays.copyOfRange(receiveData, 24, 28);
	    ByteBuffer wrapped = ByteBuffer.wrap(num);
	    int numValue =wrapped.getInt();
	    wrapped = ByteBuffer.wrap(len);
	    int lenValue = wrapped.getInt();
	    wrapped = ByteBuffer.wrap(udp_port);
	    int udp_portValue = wrapped.getInt();
	    wrapped =ByteBuffer.wrap(secretA);
	    int secretAValue = wrapped.getInt();
	    //---------------------------------------------
	    System.out.println("secret A is: "+ secretAValue); 
	    //Step A completed.
	    ByteBuffer dbuf = ByteBuffer.allocate(4);
	    dbuf.putInt(lenValue+4);
	    byte[] bytes=dbuf.array();
	    for(int i =0;i<4;i++) sendData2[i]=bytes[i];//Payload length
	    for(int i =4;i<8;i++) sendData2[i]=secretA[i-4];//Secret
	    sendData2[8]=0;sendData2[9]=1;sendData2[10]=0;sendData2[11]=(byte)197;//Step number and last 3 digits of student id
	    stepB(numValue,udp_portValue);
    	//Step B completed.
    	clientSocket.close();
    	//This part is to convert the receivedData-------------------------
	    byte tcp_port_byte[] = Arrays.copyOfRange(receiveData, 12, 16); //Port number in byte form
	    byte secretB_byte[]= Arrays.copyOfRange(receiveData, 16, 20);  //Secret b in byte form
	    wrapped = ByteBuffer.wrap(tcp_port_byte); 
	    int tcp_port =wrapped.getInt(); //port number as integer
	    wrapped = ByteBuffer.wrap(secretB_byte); 
	    int secretB = wrapped.getInt();//secretB as integer
	    System.out.println("Secret B is "+ secretB);
	    stepC(tcp_port);
	    is.read(receiveData);
	  //This part is to convert the receivedData-------------------------
	    byte num2_byte[] = Arrays.copyOfRange(receiveData, 12, 16);
	    byte len2_byte[] = Arrays.copyOfRange(receiveData, 16, 20);
	    byte secretC_byte[] =Arrays.copyOfRange(receiveData, 20, 24);
	    byte charC_byte[]=Arrays.copyOfRange(receiveData, 24, 28);
	    wrapped = ByteBuffer.wrap(num2_byte);
	    int padZero= 0;
	    if(len2_byte[3]%4!=0){
	    	//padZero will store how many to add to len2 so that it is divisible by 4.
	    	padZero+=4-len2_byte[3]%4; //Length of the package must be divisible by 4.
	    }
	    int num2 =wrapped.getInt();
	    wrapped = ByteBuffer.wrap(len2_byte);
	    int len2 = wrapped.getInt();
	    wrapped =ByteBuffer.wrap(secretC_byte);
	    int secretC = wrapped.getInt();
	    System.out.println("Secret C is "+ secretC);
	    byte charC = charC_byte[0];
	    //-------------------------------------------------
	    //Stage C completed.
	    byte[] sendData3 = new byte[len2+padZero];
	    byte[] header = new byte[12];
	    for(int i = 0; i<4;i++) header[i]=len2_byte[i]; //Payload length
	    for(int i = 4; i<8;i++) header[i]=secretC_byte[i-4]; //SecretC
	    for(int i = 8;i<12;i++) header[i]=sendData2[i];//Step number and last 3 digits of student id
	    for(int i= 0;i<len2;i++) sendData3[i]=charC; //Fill with char c
	    for(int i =len2;i<len2+padZero;i++) sendData3[i]=0; //Pad with zeroes until it is divisible by 4.
	    stepD(num2, header,sendData3);
	    byte secretD_byte[] = Arrays.copyOfRange(receiveData, 12, 16); //Extract secretD from the received message.
	    wrapped = ByteBuffer.wrap(secretD_byte);
	    int secretD = wrapped.getInt();
	    System.out.println("Secret D is " + secretD);
	    //Step D completed.
	}
}
