// Peter Idestam-Almquist, 2018-02-26.
// Fredrik Larsson

// [Do necessary modifications of this file.]

package w03;

// [You are welcome to add some import statements.]

public class Program {
	final static int NUM_WEBPAGES = 40;
	private static WebPage[] webPages = new WebPage[NUM_WEBPAGES];
	// [You are welcome to add some variables.]

	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void initialize() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			webPages[i] = new WebPage("http://www.site.se/page" + i + ".html");
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
		for (int i = 0; i < NUM_WEBPAGES; i++)
			webPages[i].download();
		for (int i = 0; i < NUM_WEBPAGES; i++)
			webPages[i].analyze();
		for (int i = 0; i < NUM_WEBPAGES; i++)
			webPages[i].categorize();
		
		// Stop timing.
		long stop = System.nanoTime();

		// Present the result.
		presentResult();
		
		// Present the execution time.
		System.out.println("Execution time (seconds): " + (stop-start)/1.0E9);
	}
}
