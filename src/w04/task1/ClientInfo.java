package w04.task1;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientInfo {
	String name;
	InetAddress ip;
	int port;
	
	private ClientInfo(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public static ClientInfo create(InetAddress ip, int port) {
		return new ClientInfo(ip, port);
	}
	
	public static ClientInfo create(String ip, int port) throws UnknownHostException {
		return new ClientInfo(InetAddress.getByName(ip), port);
	}
	
	public String toString() {
//		return ip.getHostAddress() + ":" + port;
		return name;
	}
	
	
}