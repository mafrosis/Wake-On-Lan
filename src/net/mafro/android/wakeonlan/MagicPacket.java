package net.mafro.android.wakeonlan;

import android.util.Log;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.lang.IllegalArgumentException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


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
		byte[] macBytes = validateMac(mac);
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

	public static String cleanMac(String mac) throws IllegalArgumentException
	{
		byte[] hex = validateMac(mac);
		return hex[0]+SEPARATOR+hex[1]+SEPARATOR+hex[2]+SEPARATOR+hex[3]+SEPARATOR+hex[4]+SEPARATOR+hex[5];
	}
	
	private static byte[] validateMac(String mac) throws IllegalArgumentException
	{
		//error handle semi colons
		mac = mac.replace(";", ":");
		
		//regexp pattern match a valid MAC address
		Pattern pat = Pattern.compile("((([0-9a-fA-F]){2}[-:]){5}([0-9a-fA-F]){2})");
		Matcher m = pat.matcher(mac);

		if(m.find()) {
			String result = m.group();
			String hex[] = mac.split("(\\:|\\-)");
			
			byte[] bytes = new byte[6];
			for(int i=0; i<6; i++) {
				bytes[i] = (byte) Integer.parseInt(hex[i], 16);
			}
			return bytes;
		}else{
			throw new IllegalArgumentException("Invalid MAC address.");
		}
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