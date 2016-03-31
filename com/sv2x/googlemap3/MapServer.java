package com.sv2x.googlemap3;
import com.oracle.javafx.jmx.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
class User {
	User() {
		userId = "";
		userName = "";
		ownSpaceId = false;
		spaceId = "-1";
		addr = null;
		port = 0;
		connectionActive = true;
		mLastUpdateTime = "0";
	}
	String userId;          // unique user id
	String userName;         // additional user name
	InetAddress addr;        // ipv4 addr
	int port;              // port number
	boolean connectionActive;  // connectiivty, 1 = active, 0 = inactive
	public boolean ownSpaceId; // a owner of a space?
	//public int spaceId;        // space where the user belongs to
	String spaceId;             // space where the user belongs to
	// location information
	String mLatitude;
	String mLongitude;
	String mLastUpdateTime;
	void send(final DatagramSocket skt, String msg ) {
		if(addr == null || port == 0) return;
		final DatagramPacket pkt;
		pkt = new DatagramPacket(msg.getBytes(), msg.length(), addr, port);
		new Thread(new Runnable() {
			public void run() {
				try {
					skt.send(pkt);
				} catch (IOException e) {
					e.printStackTrace( );
				}
			}
		}).start();
	}
};
class txLocation implements Runnable {
	private volatile boolean stopRequested;
	private DatagramSocket tSocket;
	List<User> userInSpace;
	int txInterval;
	String spaceOwner;
	String spaceId;
	CharSequence msg;
	Long currentTime;
	public txLocation( DatagramSocket skt, int interval, List<User> u, String o, String id ) {
		this.tSocket = skt;
		this.txInterval = interval;
		this.userInSpace = u;
		this.spaceOwner = o;
		this.spaceId = id;
		stopRequested = false;
	}
	public void requestStop() { stopRequested = true; }
	public void run() {
		while( stopRequested == false ) {
			try {
				Thread.sleep( txInterval * 1000 ); // ms
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
			// hide a user who have no response for 30 sec
			currentTime = System.currentTimeMillis()/1000;
			for(User user : userInSpace) {
				if( user.connectionActive == true && currentTime - Long.parseLong(user.mLastUpdateTime) > 30 ) {  // if too old
					user.connectionActive = false;
				}
			}
			// space owner comes first
			if( userInSpace.size() > 0 ) {
				msg = "Location Update;";
				for(User user : userInSpace) {
					if( user.userId.equals(spaceOwner) && user.connectionActive ) {
						msg = msg + user.userId + ";" + user.mLatitude + ";"
								+ user.mLongitude + ";" + user.mLastUpdateTime + ";";
					}
				}
				for(User user : userInSpace) {
					if( user.userId.equals(spaceOwner) )
						continue;
					if( user.connectionActive ) {
						msg = msg + user.userId + ";" + user.mLatitude + ";"
								+ user.mLongitude + ";" + user.mLastUpdateTime + ";";
					}
				}
				System.out.println( "> " + spaceId + ": " + msg);
			}
			for(User user : userInSpace) {
				user.send( tSocket, msg.toString() );
			}
		}
	}
}
/*
class cleanUser implements Runnable {
   int cleanInterval;
   List<User> userList;
   boolean stopRequested;
   long currentTime;
   public cleanUser( int interval, List<User> u ) {
      this.cleanInterval = interval;
      this.userList = u;
      stopRequested = false;
   }
   public void requestStop() { stopRequested = true; }
   public void run() {
      while( stopRequested == false ) {
         try {
            Thread.sleep( cleanInterval * 1000 );  // ms
         } catch ( InterruptedException e ) {
            e.printStackTrace();
         }
         currentTime = System.currentTimeMillis();
         for(User user : userList) {
            if( currentTime - Long.parseLong(user.mLastUpdateTime) > 30*1000 ) {   // if too old
               userList.remove(user);
            }
         }
      }
   }
}
*/
/***********************************************************************************************************************************/
/***********************************************Send Leader's Locations*************************************************************/
/***********************************************************************************************************************************/
class Space
{
	String spaceId;
	String owner;
	private List<User> memberList = new ArrayList<User>();
	txLocation txThread;
	Boolean Actual_locations;
	Space( String id, DatagramSocket txSocket, int txInterval, Boolean location_type ) {
		spaceId = id;
		txThread = new txLocation( txSocket, txInterval, this.memberList, owner, spaceId );
		new Thread( txThread ).start();
		Actual_locations = location_type;
	}
	public String getSpaceId() { return spaceId; }
	public void addMember(User u) { memberList.add(u); }
	public void removeMember(User u) {
		memberList.remove(u);
		if( memberList.size() == 0) txThread.requestStop();
	}
	public List<User> getMemberList()
	{
		return memberList;
	}
	public void updateLeaderLocations()
	{
	}
	public User findMember(String name) {
		for(User user : memberList) {
			if(user.userId.equals(name)) {
				return user;
			}
		}
		return null;
	}
	public int N_Users()
	{
		return memberList.size();
	}
	public void printMembers() {
		String text;
		text = "> Space " + spaceId + " >> ";
		//for(int i=0; i<memberList.size(); i++) {
		// text += memberList.get(i) + ", ";
		//}
		for(User user : memberList) {
			text += user.userId + ", ";
		}
		System.out.println(text);
	}
};
class Spaces {
	List<Space> spaceList = new ArrayList<Space>();
	void addSpace( Space s ) {
		spaceList.add(s);
	}
	void removeSpace( Space s ) {
		spaceList.remove(s);
	}
	public Space findSpace( String id ) {
		for(Space s : spaceList) {
			if(s.spaceId.equals(id)) {
				return s;
			}
		}
		return null;
	}
};
class Users {
	List<User> userList;
	//cleanUser cleanThread;
	Users() {
		userList = new ArrayList<User>();
		//cleanThread = new cleanUser( 30, this.userList );
		//new Thread( cleanThread ).start();
	}
	void addUser(User u) {
		userList.add(u);
	}
	void removeUser(User u) {
		userList.remove(u);
	}
	public User findUser(String id) {
		for(User u : userList) {
			if(u.userId.equals(id)) {
				return u;
			}
		}
		return null;
	}
};
class Receive implements Runnable {
	static final String NEW_USER = "New User";
	static final String CREATE_SPACE_MATCHED = "Create Space Matched";
	static final String CREATE_SPACE_ACTUAL = "Create Space Actual";
	static final String JOIN_SPACE = "Join Space";
	static final String SEND_INVITATION = "Send Invitation";
	static final String LEAVE_SPACE = "Leave Space";
	static final String LOCATION = "Location";
	static final String UPDATE_LEADER_LOCATIONS = "Leader's Locations";
	static final String REMOVE_USER = "Remove User";
	static final String REMOVE_SPACE = "Remove Space";
	DatagramSocket rSocket = null;
	DatagramPacket rPacket = null;
	Users uList;
	Spaces sList;
	byte[] rMessage = new byte[2000];
	public Receive (DatagramSocket sck, Users u, Spaces s) {
		this.rSocket = sck;
		this.uList = u;
		this.sList = s;
	}
	public void run() {
		//while ( isRunning ) {
		while ( true ) {
			rPacket = new DatagramPacket(rMessage, rMessage.length);
			try {
				rSocket.receive( rPacket );
				handlePacket( rPacket );
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void handlePacket( DatagramPacket pkt ) {
		String msg;
		int index;
		User clientUser;
		String clientId;
		String clientName;
		String spaceId;
		String msgType;
		String requestedSpaceId;
		User tempUser;
		Space tempSpace;
		// cjoo: debug - the entire incoming message
		msg = new String( rMessage, 0, pkt.getLength() );
		//System.out.println( msg );
		// cjoo: debug - uknown message
		index = msg.indexOf(";");
		if( index <= 0 ) { System.out.println( "Unknown message" ); return;
		}
		else {
			clientId = msg.substring( 0, index );
			msg = msg.substring( index+1, msg.length() );
			clientUser = uList.findUser(clientId);          /*************added***************/
		}
		//System.out.println( clientId );
		// cjoo: get message type
		index = msg.indexOf(";");
		if( index <= 0 ) { System.out.println( "Unknown message type" ); return;
		} else {
			msgType = msg.substring( 0, index );
			msg = msg.substring( index+1, msg.length() );
		}
		//System.out.println( msgType );
		// cjoo: handle the message
		if( msgType.equals( NEW_USER ))
		{
			// add the user in the list
			addUser( clientId, pkt );
		}
		else if ( msgType.equals( CREATE_SPACE_MATCHED ) || msgType.equals( CREATE_SPACE_ACTUAL ))
		{
			Boolean location_type = false;
			if ( msgType.equals( CREATE_SPACE_ACTUAL ) )
			{
				location_type = true;
			}
			// creat new space
			index = msg.indexOf(";");
			if( index <= 0 )
			{
				System.out.println( "Unknown create space message format" ); return;
			} else
			{
				requestedSpaceId = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			////////////////////////////////////////////////////////////////
			// We need to check,
			// 1. the requester belongs to a space as an owner (remove the space)
			// 2. the owner belongs to a space as non-owner (leave the space)
			////////////////////////////////////////////////////////////////
			tempSpace = sList.findSpace( requestedSpaceId );
			//if (tempSpace.)
			if( tempSpace != null )
			{
				System.out.println( "Existing space name: " + requestedSpaceId );
				if( tempSpace.owner.equals( clientId ) ) {
					// remove the space
					sList.removeSpace( tempSpace );
				} else {
					// somehow reject the space name.. (how?)
					//
					return;
				}
			}
			tempUser = uList.findUser( clientId );
			if( tempUser != null ) {
				tempSpace = sList.findSpace( tempUser.spaceId );
				if( tempSpace != null ) {
					tempSpace.removeMember( tempUser );
				}
			}
			Space s = new Space( requestedSpaceId, rSocket, 7, location_type );
			s.owner = clientId;
			s.Actual_locations = location_type;
			sList.addSpace(s);
			System.out.println( "Space " + requestedSpaceId + " has been created");
			joinSpace( clientId, requestedSpaceId );
			// Space( int id, DatagramSocket txSocket, int txInterval ) {
		}
		else if ( msgType.equals( JOIN_SPACE ))
		{
			// join the space
			index = msg.indexOf(";");
			if( index <= 0 ) {
				System.out.println( "Unknown join message format" );
				return;
			} else {
				spaceId = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			System.out.println( spaceId );
			joinSpace( clientId, spaceId );
			User user = uList.findUser( clientId );
			CharSequence response;
			response = JOIN_SPACE + " OK;" + user.spaceId;
			user.send( rSocket, response.toString() );
		}
		else if ( msgType.equals( LEAVE_SPACE ))
		{
			// leave the space
			index = msg.indexOf(";");
			if( index <= 0 )
			{
				System.out.println( "Unknown join message format" );
				return;
			} else
			{
				spaceId = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			System.out.println( spaceId );
			leaveSpace( clientId, spaceId );
			User user = uList.findUser( clientId );
			CharSequence response;
			response = JOIN_SPACE + " OK;" + user.spaceId;
			user.send( rSocket, response.toString() );
		}
		else if ( msgType.equals( LOCATION ))
		{
			// update location
			String latitude;
			String longitude;
			String time;
			index = msg.indexOf(";");
			if( index <= 0 ) { System.out.println( "Delimit error" ); return;
			} else {
				latitude = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			index = msg.indexOf(";");
			if( index <= 0 ) { System.out.println( "Delimit error" ); return;
			} else {
				longitude = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			index = msg.indexOf(";");
			if( index <= 0 ) { System.out.println( "Delimit error" ); return;
			} else {
				time = msg.substring( 0, index );
				msg = msg.substring( index+1, msg.length() );
			}
			updateLocation( clientId, latitude, longitude, time );
		}
		else if ( msgType.equals( UPDATE_LEADER_LOCATIONS )) {
			// update location
			String spaceID;
			spaceID = clientUser.spaceId;
			Space client_space = sList.findSpace(spaceID);
			List<User> space_users = client_space.getMemberList();
			for(User user : space_users) {
				if( user.userId.equals(clientId) ) {
					continue;
				}
				else {
					if (client_space.Actual_locations){
						String add_back="";
						for (int i=0;i<30;i++)
						{
							add_back+="**********";
						}
						user.send( rSocket ,  UPDATE_LEADER_LOCATIONS + ";" + user.userId + ";finish;" + msg + add_back );
					}
					else
					{
						try
						{
							msg = makeOsrmQuery(msg);
							String add_back="";
							for (int i=0;i<30;i++)
							{
								add_back+="**********";
							}
							while (msg.length()>0)
							{
								String part_msg="";
								if (msg.length()>300)
								{
									part_msg = msg.substring(0,300);
									part_msg += add_back;
									user.send( rSocket ,  UPDATE_LEADER_LOCATIONS + ";" + user.userId + ";to be continue;" + part_msg );
									msg = msg.substring(300);
								}
								else if (msg.length() <= 300)
								{
									user.send( rSocket ,  UPDATE_LEADER_LOCATIONS + ";" + user.userId + ";finish;" + msg+add_back );
									msg="";
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		else if (msgType.equals(REMOVE_USER))
		{

		}
		else if (msgType.equals(REMOVE_SPACE))
		{

		}
		else {
			System.out.println( "Unknown message type" );
		}
	}
	void addUser( String clientId, DatagramPacket pkt ) {
		User user;
		user = uList.findUser( clientId );
		if ( user != null ) {
			System.out.println( "User exists" );
			user.addr = pkt.getAddress();
			user.port = pkt.getPort();
			user.connectionActive = true;
			//if ( user.connectionActive == false ) {
			//user.connectionActive = true;
			// maybe update last access time
			//}
		} else {
			user = new User();
			user.userId = clientId;
			user.addr = pkt.getAddress();
			user.port = pkt.getPort();
			user.connectionActive = true;
			// update last access time
			uList.addUser(user);
		}
		CharSequence response;
		response = NEW_USER + " OK;" + user.userId;
		user.send( rSocket, response.toString() );
		return;
	}
	void joinSpace( String clientId, String spaceId ) {
		User user;
		Space space;
		user = uList.findUser( clientId );
		if( user == null ) {
			System.out.println( "User does not exist" );
			return;
		}
		space = sList.findSpace( spaceId );
		if( space == null) {
			System.out.println( "Space does not exist" );
			return;
		}
		if( space.findMember( clientId ) == null ) {
			space.addMember(user);
		}
		user.spaceId = spaceId;
		return;
	}
	void leaveSpace( String clientId, String spaceId ) {
		User user;
		Space space;
		user = uList.findUser( clientId );
		if( user == null ) {
			System.out.println( "User does not exist" );
			return;
		}
		space = sList.findSpace( spaceId );
		if( space == null) {
			System.out.println( "Space does not exist" );
			return;
		}
		if( space.findMember( clientId ) != null ) {
			space.removeMember(user);
		}
		user.spaceId = "";
		return;
	}
	void updateLocation( String clientId, String lat, String log, String time ) {
		User user;
		CharSequence response = "";
		user = uList.findUser( clientId );
		if ( user == null ) {
			System.out.println( "User does not exist" );
			return;
		}
		user.mLatitude = lat;
		user.mLongitude = log;
		user.mLastUpdateTime = time;
		user.connectionActive = true;

		Date date = new Date(Long.parseLong(time));
		response = clientId + " has location update: " +
				lat + " / " + log + " / " + date;
		System.out.println( response );
	}
	String makeOsrmQuery(String url_query) throws IOException {
		System.out.println(url_query);
		String latitude;
		String longitude;
		String time;
		String Leader_ID;
		String msg;
		int index;
		msg = url_query;
		url_query = "http://10.20.17.173:5000/match?";
		index = msg.indexOf(";");
		if (index <= 0) {
			System.out.println("Delimit error");
			return "Unknow message type";
		} else {
			Leader_ID = msg.substring(0, index);
			msg = msg.substring(index + 1, msg.length());
		}
		while (msg.length() > 0) {
			index = msg.indexOf(";");
			if (index <= 0) {
				System.out.println("Delimit error");
				return "Unknow message type";
			} else {
				latitude = msg.substring(0, index);
				msg = msg.substring(index + 1, msg.length());
			}
			index = msg.indexOf(";");
			if (index <= 0) {
				System.out.println("Delimit error");
				return "Delimit error";
			} else {
				longitude = msg.substring(0, index);
				msg = msg.substring(index + 1, msg.length());
			}
			index = msg.indexOf(";");
			if (index <= 0) {
				System.out.println("Delimit error");
				return "Delimit error";
			} else {
				time = msg.substring(0, index);
				msg = msg.substring(index + 1, msg.length());
			}
			url_query = url_query + "loc=" + latitude + "," + longitude + "&t=" + time + "&";
		}
		url_query = url_query + "instructions=true&compression=false";
		URL oracle = new URL(url_query);
		URLConnection yc = oracle.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				yc.getInputStream()));
		String inputLine;
		String JsonO = "";
		while ((inputLine = in.readLine()) != null) {
			JsonO+=inputLine;
			System.out.println(inputLine);
		}
		return Leader_ID + ";" + JsonO;
	}
}
public class MapServer
{
	// cjoo: The following String constants are used to denote the message type.
	public static final int PORT = 8006;
	public static void main(String[] args) {
		int index;
		String text;
		String type;
		InetAddress clientInetAddr;
		int clientPort;
		String clientId;
		String spaceId;
		Spaces allSpaceList = new Spaces();
		Users allUserList = new Users();
		int nSpace = 0;
		Space space;
		User user;
		byte[] recvMsg = new byte[1000];
		String sendMsg;
		DatagramPacket dp = null;
		DatagramSocket ds = null;
		try {
			ds = new DatagramSocket(PORT);
			new Thread(new Receive( ds, allUserList, allSpaceList )).start();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}