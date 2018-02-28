package w02;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Testing {
	public static void main(String[] args) {
		String NUM_THREADS = "" + 	12;
		String NUM_ACCOUNTS = "" + 	10_00;
		String FACTOR = "" + 		10_000;
		String TIMEOUT = "" + 		60;
		int iterations = 1;
		String Ops = format((long) ((Integer.parseInt(FACTOR) * Integer.parseInt(NUM_ACCOUNTS)*5)*iterations));
		System.out.format("Threads: %s%nAccounts: %s%nFactor: %s%nTotal Operations: %s%nTimeout: %s%n%n", NUM_THREADS, NUM_ACCOUNTS, FACTOR, Ops, TIMEOUT);

		String[] testArgs = {NUM_THREADS, NUM_ACCOUNTS, FACTOR, TIMEOUT};	

		System.out.println("Stamped");
		long start = System.nanoTime();
		for (int i = 0; i < iterations; i++) {	
			w02.stamped.Program.main(testArgs);	
		} 
		long done = System.nanoTime();
		System.out.println("Batch of " + iterations + " iterations done in: " + (done-start) / 1E6 + "ms");

//		System.out.println("Intrinsic locks");
//		long startSync = System.nanoTime();
//		for (int i = 0; i < iterations; i++) {	
//			w02.sync.Program.main(testArgs);
//		} 
//		long doneSync = System.nanoTime();
//		System.out.println("Batch of " + iterations + " iterations done in: " + (doneSync-startSync) / 1E6 + "ms");
		
	}
	
	
	/// Below from https://stackoverflow.com/a/30661479
	private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
	static {
	  suffixes.put(1_000L, "k");
	  suffixes.put(1_000_000L, "M");
	  suffixes.put(1_000_000_000L, "G");
	  suffixes.put(1_000_000_000_000L, "T");
	  suffixes.put(1_000_000_000_000_000L, "P");
	  suffixes.put(1_000_000_000_000_000_000L, "E");
	}

	public static String format(long value) {
	  //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
	  if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
	  if (value < 0) return "-" + format(-value);
	  if (value < 1000) return Long.toString(value); //deal with easy case

	  Entry<Long, String> e = suffixes.floorEntry(value);
	  Long divideBy = e.getKey();
	  String suffix = e.getValue();

	  long truncated = value / (divideBy / 10); //the number part of the output times 10
	  boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
	  return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
	}
}
