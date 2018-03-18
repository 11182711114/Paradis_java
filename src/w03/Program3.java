// Peter Idestam-Almquist, 2018-02-26.
// Fredrik Larsson - frla9839

// [Do necessary modifications of this file.]

package w03;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

// [You are welcome to add some import statements.]

public class Program3 {
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
		Arrays.stream(webPages).forEach(wp -> {
			CompletableFuture.supplyAsync(() -> {
				wp.download();
				return wp;
			}).thenApply(wpA -> {
				wpA.analyze();
				return wpA;
			}).thenAccept(wpC -> {
				wpC.categorize();
			});
		});
		ForkJoinPool.commonPool().awaitQuiescence(5000, TimeUnit.MILLISECONDS);

		// Stop timing.
		long stop = System.nanoTime();

		// Present the result.
		presentResult();

		// Present the execution time.
		System.out.println("Execution time (seconds): " + (stop - start) / 1.0E9);
	}
}
