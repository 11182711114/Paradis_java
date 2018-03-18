package w04.task1;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import log.Log;

public class SocketWriter<T extends Serializable> implements Runnable {
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
