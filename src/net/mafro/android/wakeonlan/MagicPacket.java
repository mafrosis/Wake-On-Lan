package net.mafro.android.wakeonlan;

import android.util.Log;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.lang.IllegalArgumentException;

public class MagicPacket
{
	private static final String TAG = "MagicPacket";
	
	public static final String BROADCAST = "192.168.1.255";
	public static final int PORT = 9;
	public static final char SEPARATOR = ':';
	
	public static String send(String mac, String ip) throws UnknownHostException, SocketException, IOException, IllegalArgumentException
	{
		return send(mac, ip, PORT);
	}

	public static String send(String mac, String ip, int port) throws UnknownHostException, SocketException, IOException, IllegalArgumentException
	{
		String[] hex = validateMac(mac);
		byte[] macBytes = convertToBytes(hex);
		byte[] bytes = new byte[102];

		//fill first 6 bytes
		for (int i = 0; i < 6; i++) {
			bytes[i] = (byte) 0xff;
		}
		//fill remaining bytes with target MAC
		for (int i = 6; i < bytes.length; i += macBytes.length) {
			System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
		}

		//create socket to IP
		InetAddress address = InetAddress.getByName(ip);
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		
		return hex[0]+SEPARATOR+hex[1]+SEPARATOR+hex[2]+SEPARATOR+hex[3]+SEPARATOR+hex[4]+SEPARATOR+hex[5];
	}
	
	private static String[] validateMac(String mac) throws IllegalArgumentException
	{
		String[] hex;
		
		if(mac.length() == 17) {
			hex = mac.split("(\\:|\\-)");
			if(hex.length != 6) {
				throw new IllegalArgumentException("Invalid MAC address.");
			}
		}else if(mac.length() == 12) {
			char[] chars;
			hex = new String[6];
			
			for(int i=0; i<6; i++) {
				chars = new char[2];
				mac.getChars((i*2), (i*2)+2, chars, 0);
				hex[i] = new String(chars);
			}
		}else{
			throw new IllegalArgumentException("Invalid MAC address.");
		}
		return hex;
	}

	private static byte[] convertToBytes(String[] hex) throws IllegalArgumentException
	{
		byte[] bytes = new byte[6];
		
		try {
			for(int i=0; i<6; i++) {
				bytes[i] = (byte) Integer.parseInt(hex[i], 16);
			}
			
		}catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hex digit in MAC address.");
		}
		return bytes;
	}

	public static void main(String[] args) {
		if (args.length != 2) {
            System.out.println("Usage: java WakeOnLan <broadcast-ip> <mac-address>");
            System.out.println("Example: java WakeOnLan 192.168.0.255 00:0D:61:08:22:4A");
            System.out.println("Example: java WakeOnLan 192.168.0.255 00-0D-61-08-22-4A");
            System.exit(1);
        }
        
        String ipStr = args[0];
        String macStr = args[1];

		try	{
			MagicPacket.send(ipStr, macStr);
		}
        catch (Exception e) {
            System.out.println("Failed to send Wake-on-LAN packet: + e");
            System.exit(1);
        }

	}

}