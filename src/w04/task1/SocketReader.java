package w04.task1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import log.Log;

/**
 * @author Fredrik
 *
 * @param <T>
 */
//TODO: Should this be parameterized for extending Seriablizable or just handling Serializables?
public class SocketReader<T extends Serializable> implements Runnable {
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
