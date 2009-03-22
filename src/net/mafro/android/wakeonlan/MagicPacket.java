package net.mafro.android.wakeonlan;

import java.io.*;
import java.net.*;

public class MagicPacket
{
	public static final int PORT = 9;
	
	public static void send(String ip, String mac) {
		send(ip, mac, PORT);
	}

	public static void send(String ip, String mac, int port) {
		try {
			byte[] macBytes = getMacBytes(mac);
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

			System.out.println("Wake-on-LAN packet sent.");

		}catch (Exception e) {
			System.out.println("Failed to send Wake-on-LAN packet: + e");
			System.exit(1);
		}
	}

	private static byte[] getMacBytes(String mac) throws IllegalArgumentException {
		byte[] bytes = new byte[6];
		String[] hex = mac.split("(\\:|\\-)");
		if(hex.length != 6) {
			throw new IllegalArgumentException("Invalid MAC address.");
		}
		try {
			for(int i = 0; i < 6; i++) {
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