package com.sv2x.googlemap3;

import java.net.DatagramSocket;
import java.net.SocketException;

public class MapServer
{
	// cjoo: The following String constants are used to denote the message type.
	public static final int PORT = 8002;
	public static void main(String[] args) {

		Spaces allSpaceList = new Spaces();
		Users allUserList = new Users();
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