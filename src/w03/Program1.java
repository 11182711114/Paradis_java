// Peter Idestam-Almquist, 2018-02-26.
// Fredrik Larsson

// [Do necessary modifications of this file.]

package w03;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

// [You are welcome to add some import statements.]

public class Program1 {
	final static int NUM_WEBPAGES = 40;
	private static WebPage[] webPages = new WebPage[NUM_WEBPAGES];
	// [You are welcome to add some variables.]
	private static int threads = Runtime.getRuntime().availableProcessors();
	private static ExecutorService tPool = Executors.newFixedThreadPool(threads);
	static BlockingQueue<WebPage> wpgs = new LinkedBlockingQueue<WebPage>();

	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void initialize() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			webPages[i] = new WebPage("http://www.site.se/page" + i + ".html");
			try {
				wpgs.put(webPages[i]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		// This is kind of cheating in a consumer producer way but since the actual work
		// is bounded this should be the most efficient way given;
		BlockingQueue<WebPage> downloaderDone = new LinkedBlockingQueue<>();
		BlockingQueue<WebPage> analyzerDone = new LinkedBlockingQueue<>();
		BlockingQueue<WebPage> categorizerDone = new LinkedBlockingQueue<>();
		IntStream.range(0, threads).forEach(i -> {
			Downloader dwlder = new Downloader(wpgs, downloaderDone);
			tPool.execute(dwlder);
		});
		IntStream.range(0, threads).forEach(i -> {
			Analyzer analyzer = new Analyzer(downloaderDone, analyzerDone);
			tPool.execute(analyzer);
		});
		IntStream.range(0, threads).forEach(i -> {
			Categorizer categorizer = new Categorizer(analyzerDone, categorizerDone);
			tPool.execute(categorizer);
		});
		tPool.shutdown();
		try {
			tPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Stop timing.
		long stop = System.nanoTime();

		// Present the result.
		presentResult();

		// Present the execution time.
		System.out.println("Execution time (seconds): " + (stop - start) / 1.0E9);
	}

}

class Downloader implements Runnable {
	BlockingQueue<WebPage> input;
	BlockingQueue<WebPage> output;

	public Downloader(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
		this.input = input;
		this.output = output;
	}

	@Override
	public void run() {
		while (!input.isEmpty()) {
			try {
				WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
				if (page == null)
					break;
				page.download();
				output.put(page);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class Analyzer implements Runnable {
	BlockingQueue<WebPage> input;
	BlockingQueue<WebPage> output;

	public Analyzer(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
		this.input = input;
		this.output = output;
	}

	@Override
	public void run() {
		while (!input.isEmpty()) {
			try {
				WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
				if (page != null) {
					page.analyze();
					output.offer(page);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class Categorizer implements Runnable {
	BlockingQueue<WebPage> input;
	BlockingQueue<WebPage> output;

	public Categorizer(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
		this.input = input;
		this.output = output;
	}

	@Override
	public void run() {
		while (!input.isEmpty()) {
			try {
				WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
				if (page != null) {
					page.categorize();
					output.offer(page);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}