package w02;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Testing {
	public static void main(String[] args) {
		String NUM_THREADS = "" + 	12;
		String NUM_ACCOUNTS = "" + 	1000;
		String FACTOR = "" + 		10000;
		String TIMEOUT = "" + 		60;
		String Ops = format((long) (Integer.parseInt(FACTOR) * Integer.parseInt(NUM_ACCOUNTS)*3));
		System.out.format("Testing:%nThreads: %s%nAccounts: %s%nFactor: %s%nTotal Operations: %s%nTimeout: %s%n%n", NUM_THREADS, NUM_ACCOUNTS, FACTOR, Ops, TIMEOUT);
		String[] testArgs = {NUM_THREADS, NUM_ACCOUNTS, FACTOR, TIMEOUT}; 
		w02.stamped.Program.main(testArgs);
		System.out.println("\n");
		w02.sync.Program.main(testArgs);
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
