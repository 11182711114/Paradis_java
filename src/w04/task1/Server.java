package w04.task1;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import log.Log;
import util.Throttler;

public class Server {
	private static final int DEFAULT_INTERVAL = 1000 / 10;
	
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 7645;
	private static final int DEFAULT_BACKLOG_LIMIT = 1024;

	private static final String DEFAULT_LOG_FILE = "logs/w02u2_1_1/server.log";
	private static final boolean DEFAULT_LOG_APPEND = true;
	
	private String host;
	private int port;
	private int backlogLimit;
	private ServerSocket listeningSocket;
	private List<Connection> connections;
	
	private File logFile = new File(DEFAULT_LOG_FILE);
	private boolean logAppend = DEFAULT_LOG_APPEND;
	private Log log;
	
	private boolean running;
	
	public Server(int port, int backlog, String ip) {
		this.host = ip;
		this.port = port;
		this.backlogLimit = backlog;
		this.connections = new ArrayList<>();
	}
	
	public Server() {
		this(DEFAULT_PORT, DEFAULT_BACKLOG_LIMIT, DEFAULT_IP);
	}
	
	public void initialize() {
		try {
			listeningSocket = new ServerSocket();
		} catch (IOException e) {
			log.exception(e);
		}
		Log.startLog(logFile, logAppend);
		log = Log.getLogger(this.getClass().getSimpleName());
	}
	
	public void shutdown() {
		log.debug("Shutting down");
		log.stop();
	}
	
	/** Starts the server
	 * @return true if the server was started(and shutdown), false if not
	 */
	public boolean start() {
		if (log == null)
			return false;
		
		try {
			listeningSocket.bind(new InetSocketAddress(host, port), backlogLimit);
//			InetAddress.getByName(host);
		} catch (IOException e) {
			log.exception(e);
			return false;
		}
		log.info("Starting server at " + listeningSocket.getInetAddress() + ":" + listeningSocket.getLocalPort());
		running = true;
		run();
		return true;
	}
	
	private void run() {
		long lastRun = System.currentTimeMillis();
		while(running) {
			Socket newSocket = null;
			try {
				lastRun = Throttler.waitIfNecessary(lastRun, DEFAULT_INTERVAL);
				
				log.debug("Listening on: " + listeningSocket.getInetAddress() + ":" + listeningSocket.getLocalPort());
				newSocket = listeningSocket.accept();
			} catch (IOException e) {
				log.exception(e);
			} catch (InterruptedException e) {
				log.exception(e);
			}
			if (newSocket != null) {
				log.debug("New connection: " + newSocket.getInetAddress() + ":" + newSocket.getPort());
				Connection conn = new Connection(newSocket, this);
				log.debug("Starting new Thread for connection");
				new Thread(conn).start();
				connections.add(conn);		
			} else {
				log.error("Failed to establish connection with incoming request");
			}
		}
	}
	
	public List<Connection> getConnections() {
		return connections;
	}
	
	
	public static void main(String[] args) {
		Server server = null;
		server = new Server();
		
		server.initialize();
		server.start();
		server.shutdown();
	}
}
