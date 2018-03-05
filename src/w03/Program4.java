// Peter Idestam-Almquist, 2018-02-26.
// Fredrik Larsson - frla9839

// [Do necessary modifications of this file.]

package w03;

import java.util.concurrent.ConcurrentHashMap;

// [You are welcome to add some import statements.]

public class Program4 {
	final static int NUM_WEBPAGES = 40;
	private static WebPage[] webPages = new WebPage[NUM_WEBPAGES];
	private static ConcurrentHashMap<Integer, WebPage> cWebPages = new ConcurrentHashMap<>();
	// [You are welcome to add some variables.]

	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void initialize() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			webPages[i] = new WebPage("http://www.site.se/page" + i + ".html");
			cWebPages.put(i, webPages[i]);
		}
	}
	
	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void presentResult() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			System.out.println(webPages[i]);
		}
	}
	
	public static void main(String[] args) {
		// Initialize the list of webpages.
		initialize();
		
		// Start timing.
		long start = System.nanoTime();

		// [Do modify this sequential part of the program.]
		cWebPages.forEachValue(1, wp -> {
			wp.download();
			wp.analyze();
			wp.categorize();
		});
		
		// Stop timing.
		long stop = System.nanoTime();

		// Present the result.
		presentResult();
		
		// Present the execution time.
		System.out.println("Execution time (seconds): " + (stop-start)/1.0E9);
	}
}
