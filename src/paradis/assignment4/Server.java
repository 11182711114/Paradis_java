// Fredrik Larsson frla9839
package paradis.assignment4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

 // Note: this is modifier from a previous assignment in another course and has some things that do not apply to this assignment, e.g. logger
 // 		However the logger does not write and removing references to it is quite extensive
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
	private boolean logWrite = false; // change this to true if you want the logger to actually write
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
		Log.startLog(logFile, logAppend, logWrite); 
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
			} catch (IOException | InterruptedException e) {
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

class SocketWriter<T extends Serializable> implements Runnable {
//	private static final int DEFAULT_INTERVAL = 1000 / 1000;
	
	private OutputStream output;
	
	private BlockingQueue<T> outputBuffer;
	
	private boolean running;
	Log log;
	
	public SocketWriter(OutputStream out) {
		this.output = out;
		log = Log.getLogger(this.getClass().getSimpleName()+ "@" +Thread.currentThread().getName());
		outputBuffer = new LinkedBlockingQueue<T>();
	}
	
	@Override
	public void run() {
		log.debug("Writer starting");
		running = true;
//		ObjectOutputStream writer = null;
//		try {
//			writer = new ObjectOutputStream(output);
//		} catch (IOException e1) {
//			log.exception(e1);
//		}
		
		PrintWriter pw = new PrintWriter(output, true);
		
		while(running) {
			try {
				log.debug("Checking for data to write");
				T toWrite = outputBuffer.take();
				log.debug("New data to write");
//				if (toWrite != null)
//					writer.writeObject(toWrite);
				pw.println(toWrite);
				
			} catch (InterruptedException e) {
				log.exception(e);
			} 
//			catch (IOException e) {
//				log.exception(e);
//			}
		}
	}
	
	public boolean add(T s) {
		log.trace("Adding to writerBuffer: " + s);
		return outputBuffer.offer(s);
	}

	public void stop() {
		running = false;
	}
}


class SocketReader<T extends Serializable> implements Runnable {
//	private static final int DEFAULT_INTERVAL = 1000 / 1000;
	
	private InputStream input;
	
	private BlockingQueue<T> inputBuffer;
	
	private boolean running;
	Log log;
	
	public SocketReader(InputStream input) {
		this.input = input;
		log = Log.getLogger(this.getClass().getSimpleName()+ "@" +Thread.currentThread().getName());
		inputBuffer = new LinkedBlockingQueue<T>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		log.debug("Reader starting");
		running = true;		
//		ObjectInputStream reader = null;
//		try {
//			reader = new ObjectInputStream(input);
//		} catch (IOException e1) {
//			log.exception(e1);
//		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		while(running) {
			try {
				log.trace("Waiting for data to read");
				Object read = reader.readLine();
				if (read == null) // can be null if the thread is interrupted while waiting
					continue;
				
//				log.trace("Data to read found");
//				if (read instanceof Serializable) {
//					log.trace("Data is Serializable");
//					inputBuffer.put((T) read); //FIXME This is bad? Should be bad
//				}
				inputBuffer.put((T) read);
				
				
				
			} catch (IOException e) {
				log.exception(e);
			} catch (InterruptedException e) {
				log.exception(e);
			}
		}
	}
	
	/** Bridge to {@link BlockingQueue#poll()}
	 * @return {@link T} - if available
	 */
	public T poll() {
		return inputBuffer.poll();
	}
	
	/** Checks if there are objects in the buffer
	 * @return true if there are objects, false otherwise
	 */
	public boolean available() {
		return !inputBuffer.isEmpty();
	}

	/** Stops the SocketReader
	 * 
	 */
	public void stop() {
		running = false;
	}

	public T peek() {
		return inputBuffer.peek();
	}
	
}


class Connection implements Runnable {
	private static final int DEFAULT_INTERVAL = 1000 / 10;

	private ClientInfo client;
	private Socket socket;
	private SocketWriter<String> writer;
	private Thread writerThread;
	private SocketReader<String> reader;
	private Thread readerThread;
	private Server server;

	private Queue<String> out;
	
	private boolean running;

	private Log log;

	public Connection(Socket socket, Server server) {
		this.socket = socket;
		this.server = server;
		this.out = new LinkedList<String>();
		this.client = ClientInfo.create(socket.getInetAddress(), socket.getPort());
		this.log = Log.getLogger(this.getClass().getSimpleName() + ":" + client.toString());
	}

	public void start() throws IOException {
		this.writer = new SocketWriter<String>(socket.getOutputStream());
		this.reader = new SocketReader<String>(socket.getInputStream());
		writerThread = new Thread(writer);
		readerThread = new Thread(reader);
		writerThread.start();
		readerThread.start();
		
	}
	
	public boolean send(String msg) {
		log.trace("Sending: " + msg);
		return writer.add(msg);
	}

	private void sendToRest(String msg) {
		List<Connection> connections = server.getConnections();
		connections.forEach(conn -> {
			if (conn != this)
				conn.send(msg);
		});
	}

	@Override
	public void run() {
		try {
			start();
		} catch (IOException e1) {
			log.exception(e1);
		}
		log.debug("Connection starting");
		running = true;
		long lastRun = System.currentTimeMillis();
		while(running) {
			try {
				lastRun = Throttler.waitIfNecessary(lastRun, DEFAULT_INTERVAL);
			} catch (InterruptedException e) {
				log.exception(e);
			}
			if (socket.isClosed()) {
				running = false;
				return;
			}
			read();			
			write();
		}
		reader.stop();
		writer.stop();
		readerThread.interrupt();
		writerThread.interrupt();
	}
	
	private void read() {
		while (reader.available()) {
			if (client.name == null)
				client.name = reader.peek();
			sendToRest(client + ": " + reader.poll());
		}
	}

	private void write() {
		while(!out.isEmpty())
			writer.add(out.poll());
	}

	public void stop() {
		running = false;
	}

}

class ClientInfo {
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

class Throttler {
	
	public static long waitIfNecessary(long lastRun, int intervalInMs) throws InterruptedException {
		// Lets not use 100% cpu
		long timeDiff = System.currentTimeMillis() - lastRun;
		if (timeDiff < intervalInMs)
			Thread.sleep(intervalInMs - timeDiff);
		return System.currentTimeMillis();
	}
	
	
}



/*
 * 
 * ##############################
 * ##############################
 * #####					#####
 * #####	  WARNING		#####
 * #####  HERE BE DRAGONS	#####
 * #####    ignore below	#####
 * #####					#####
 * ##############################
 * ##############################
 * 
 */

class Log {

	private LogWriter lw;
	private String context;

	private Log(String context) {
		this.context = context;
		lw = LogWriter.getInstance();
	}
	
	public static void startLog(File logFile, boolean append, boolean write) {
		LogWriter.setLogFile(logFile);
		LogWriter.setAppend(append);
		LogWriter.setWrite(write);
		LogWriter lw = LogWriter.getInstance();
		if (lw != null)
			new Thread(lw).start();
	}
	
	public static Log getLogger(String who) {
		return new Log(who);
	}

	public void debug(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.DEBUG, context);
		addToLog(lls);
	}

	public void error(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.ERROR, context);
		addToLog(lls);
	}

	public void info(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.INFO, context);
		addToLog(lls);
	}

	public void trace(String toLog) {
		LogLineStorage lls = LogLineStorage.create(toLog, LogLevel.TRACE, context);
		addToLog(lls);
	}

	public void exception(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		LogLineStorage lls = LogLineStorage.create(exceptionAsString, LogLevel.EXCEPTION, context);
		addToLog(lls);
	}

	private void addToLog(LogLineStorage lls) {
		if (lw != null)
			lw.add(lls);
	}
	
	public void stop() {
		lw.stop();
	}
}

enum LogLevel {
	// Severity lowest down to highest
	ERROR, INFO, DEBUG, EXCEPTION, TRACE
}

class LogLineStorage implements Comparable<LogLineStorage> {
	private String toLog;
	private LogLevel lvl;
	private long time;
	private String who;
	private String threadName;

	public LogLineStorage(String toLog, LogLevel lvl, long time, String who, String threadName) {
		this.toLog = toLog;
		this.lvl = lvl;
		this.time = time;
		this.who = who;
		this.threadName = threadName;
	}

	public LogLineStorage(String exceptionAsString, LogLevel stacktrace, long time) {
		this.toLog = exceptionAsString;
		this.lvl = stacktrace;
		this.time = time;
	}
	
	public static LogLineStorage create(String toLog, LogLevel lvl, String context) {
		return new LogLineStorage(toLog, lvl, System.currentTimeMillis(), context, Thread.currentThread().getName());
	}

	public String toWrite() {
		DateFormat df = new SimpleDateFormat("y-M-d HH:mm:ss.SSS");
		String timeOutput = df.format(time);
		return "[" + timeOutput + "] " + lvl + "\t" + who + "@" + threadName +" :: " + toLog;
	}

	@Override
	public int compareTo(LogLineStorage lls) {
		long timeDiff = this.time = lls.time;
		if (timeDiff == 0)
			return 0;
		if (timeDiff > 0)
			return 1;
		return -1;
	}
}

class LogWriter implements Runnable {

	private static File logFileStatic;
	private static boolean append = true;
	private static boolean write = true;

	private static class LogWriterHolder {
		private static final LogWriter INSTANCE = new LogWriter(logFileStatic, append);
	}

	public static LogWriter getInstance() {
		if (logFileStatic == null)
			return null;
		return LogWriterHolder.INSTANCE;
	}

	public static void setLogFile(File f) {
		logFileStatic = f;
	}

	public static void setAppend(boolean app) {
		append = app;
	}
	
	public static void setWrite(boolean write) {
		LogWriter.write = write;
	}

	private File logFile;
	private boolean active;

	private LinkedBlockingQueue<LogLineStorage> writeBuffer = new LinkedBlockingQueue<>();

	public LogWriter(File logFileLocation, boolean append) {
		logFile = logFileLocation;
	}

	@Override
	public void run() {
		active = true;
		if (!append) {
			if (logFile.exists()) {
				logFile.delete();
			}
		}
		while (active && write) {
			try {
				LogLineStorage lls = writeBuffer.poll(100, TimeUnit.MILLISECONDS);
				if (lls != null) {
					String toWrite = lls.toWrite();
					FileUtil.writeToFile(toWrite, logFile);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while (active && !write) {
			try {
				writeBuffer.poll(100, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void add(LogLineStorage lls) {
		writeBuffer.add(lls);
	}

	public void stop() {
		active = false;
	}
}

class FileUtil {
	public static void writeToFile(String toWrite, File toWriteIn) throws IOException {
		if (!toWriteIn.exists()) {
			new File(toWriteIn.getAbsolutePath().substring(0, toWriteIn.getAbsolutePath().lastIndexOf(File.separator)))
					.mkdirs();
		}

		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(toWriteIn, true)));
		pw.println(toWrite);
		pw.flush();
		pw.close();
	}

	public static String[] readFromFile(File f) throws FileNotFoundException {
		List<String> output = new ArrayList<>();
		Scanner sc;

		sc = new Scanner(new BufferedReader(new FileReader(f)));

		while (sc.hasNextLine())
			output.add(sc.nextLine());

		sc.close();

		String[] tmpOut = shiftArray(output.toArray(new String[0]));
		tmpOut[0] = f.getName();

		return tmpOut;
	}

	private static String[] shiftArray(String[] s) {
		String[] tmp = new String[s.length + 1];
		for (int i = (s.length - 1); i > -1; i--) {
			tmp[i + 1] = s[i];
		}
		return tmp;
	}

	public static void writeToFile(String[] rt, File toWriteIn) throws IOException {
		if (!toWriteIn.exists()) {
			new File(toWriteIn.getAbsolutePath().substring(0, toWriteIn.getAbsolutePath().lastIndexOf(File.separator)))
					.mkdirs();
		}

		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(toWriteIn, true)));
		for (String s : rt) {
			pw.println(s);
		}
		pw.flush();
		pw.close();
	}
}
