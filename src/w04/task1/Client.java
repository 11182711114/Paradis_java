package w04.task1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import log.Log;
import util.Throttler;

public class Client {
	private static final int DEFAULT_INTERVAL = 1000 / 10;
	
	private static final int DEFAULT_PORT = 2000;
	private static final String DEFAULT_HOST = "127.0.0.1";
	
	private static final int DEFAULT_SERVER_PORT = 7645;
	private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
	
	private static final String DEFAULT_LOG_FILE = "logs/w02u2_1_1/client.log";
	private static final boolean DEFAULT_LOG_APPEND = true;

	
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	
	private String serverHost = DEFAULT_SERVER_HOST;
	private int serverPort = DEFAULT_SERVER_PORT;
	
	private Socket socket;
	private SocketReader<String> reader;
	private SocketWriter<String> writer;
	private Thread readerThread;
	private Thread writerThread;
	
	
	private File logFile = new File(DEFAULT_LOG_FILE);
	private boolean logAppend = DEFAULT_LOG_APPEND;
	private Log log;
	
	private Queue<String> consoleInput;
	
	private boolean running;
	
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
		
		this.consoleInput = new LinkedList<String>();
	}
	public Client(String host) {
		this(host, DEFAULT_PORT);
	}
	public Client() {
		this(DEFAULT_HOST);
	}
	
	public boolean initialize() {
		Log.startLog(logFile, logAppend, false);
		log = Log.getLogger(this.getClass().getSimpleName());
		
		socket = connect(serverHost, serverPort, host, port);
		if (socket == null)
			return false;
	
		return true;
	}
	
	private Socket connect(String serverHost, int serverPort, String host, int port) {
		Socket socket = null ;
		try {
			socket = new Socket(serverHost, serverPort, InetAddress.getByName(host), port);
			System.out.println("Connected to server: " + serverHost + ":" + serverPort + " from local " + host + ":" + port);
		} catch (IOException e) {
			log.exception(e);
			log.error("Failed to create Socket with remote: " + serverHost + ":" + serverPort + " local: "+ host + ":" + port);
			log.stop();
			System.out.println("Could not connect to server, shutting down");
		}
		return socket;
	}
	
	public void start() {
		log.info("Starting client: " + host + ":" + port);
		try {
			reader = new SocketReader<String>(socket.getInputStream());
			writer = new SocketWriter<String>(socket.getOutputStream());
			log.debug("Starting reader thread");
			readerThread = new Thread(reader);
			log.debug("Starting writer thread");
			writerThread = new Thread(writer);
			readerThread.start();
			writerThread.start();
		} catch (IOException e) {
			log.exception(e);
		}
		running = true;
		run();
		stop();
	}
	
	public void stop() {
		writer.stop();
		reader.stop();
		writerThread.interrupt();
		readerThread.interrupt();
	}
	
	
	public void run() {
		try ( 
			PrintWriter outConsole = new PrintWriter(System.out, true);
			BufferedReader inConsole = new BufferedReader(new InputStreamReader(System.in));
		) {
			long lastRun = System.currentTimeMillis();
			while(running) {
				lastRun = Throttler.waitIfNecessary(lastRun, DEFAULT_INTERVAL);
				
				if (socket.isClosed())
					running = false;
				
				readFromConsole(inConsole);
				sendToSever();
				printFromServer(outConsole);
			}
		} catch (IOException e) {
			log.exception(e);
			log.debug("Client failed to establish read or write to socket or console");
		} catch (InterruptedException e) {
			log.exception(e);
		}
	}
	
	private void printFromServer(PrintWriter outConsole) {
		while(reader.available()) {
			String line = reader.poll();
			outConsole.println(line);
		}
	}
	
	private void sendToSever() {		
		while(!consoleInput.isEmpty()) {
			writer.add(consoleInput.poll());
		}
	}
	
	public void readFromConsole(BufferedReader inConsole) {
		try {
			while(inConsole.ready()) {
				String in = inConsole.readLine();
				log.trace("Reading from console: " + in);
				consoleInput.offer(in);
			}
		} catch (IOException e) {
			log.exception(e);
		}
	}
	
	public static void main(String[] args) {
		Client client = null;
		if (args.length == 2)
			client = new Client(args[0], Integer.parseInt(args[1]));
		else if (args.length == 1) 
			client = new Client(args[0]);
		else 
			client = new Client();
		
		if (client.initialize())
			client.start();
	
		
	}
	

}
