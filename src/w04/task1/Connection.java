// Fredrik Larsson frla9839
package w04.task1;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import log.Log;
import util.Throttler;

public class Connection implements Runnable {
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
