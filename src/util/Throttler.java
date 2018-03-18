package util;

public class Throttler {
	
	public static long waitIfNecessary(long lastRun, int intervalInMs) throws InterruptedException {
		// Lets not use 100% cpu
		long timeDiff = System.currentTimeMillis() - lastRun;
		if (timeDiff < intervalInMs)
			Thread.sleep(intervalInMs - timeDiff);
		return System.currentTimeMillis();
	}
	
	
}
