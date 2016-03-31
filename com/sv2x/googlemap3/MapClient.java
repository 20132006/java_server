package com.sv2x.googlemap3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MapClient
{

	public static final int PORT = 8001;

	public static void main(String[] args) {

		String text = "hello2";
		DatagramPacket dp;
		DatagramSocket ds = null;

		try {
			InetAddress serverAddr = InetAddress.getByName("192.168.1.100");
			ds = new DatagramSocket();
			dp = new DatagramPacket(text.getBytes(), text.length(), serverAddr, PORT);
			ds.send(dp);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		ServerSocket serverSocket = null;
		
		try {
			System.out.println("Opening Server Socket...");
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		TxThread txThread = new TxThread();
		txThread.start();
		 
		System.out.println("Waiting Client...");
		while(true){
			Socket skt_client;
			try {
				skt_client = serverSocket.accept();
				//System.out.println("Client Accepted!");
				
				ClientThread clientThread = new ClientThread(skt_client);
				clientThread.start();
				
				ClientList.addClient(clientThread);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	*/
	}

}
