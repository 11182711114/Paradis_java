package w01;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Factorizer {

	private long product;
	private int maxThreads;
	private Work[] workPool;
	// private Thread[] threadPool;

	public Factorizer(long product, int threads) {
		this.product = product;
		this.maxThreads = threads;
		workPool = new Work[maxThreads];
		// threadPool = new Thread[maxThreads];
	}

	public void start() {
		long startTime = System.nanoTime();
		CountDownLatch done = new CountDownLatch(1);
		CountDownLatch notFound = new CountDownLatch(maxThreads);
		CountDownLatch[] list = { done, notFound };
		SeveralLatchWaiters slw = new SeveralLatchWaiters(list);
		for (int i = 0; i < maxThreads; i++) {
			Work work = new Work(2 + i, product, maxThreads, done, notFound);
			Thread thread = new Thread(work);
			// threadPool[i] = thread;
			workPool[i] = work;
			thread.start();
		}
		try {
			slw.await(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long doneTime = System.nanoTime();
		long factor1 = 0L, factor2 = 0L;
		for (Work work : workPool) {
			if (work.factor1 != 0L) {
				factor1 = work.factor1;
				factor2 = work.factor2;
			}
			work.shutdown();
		}
		if (isPrime(product)) {
			System.out.println("No factorization possible");
		} else if (isPrime(factor1)) {
			if (isPrime(factor2)) {
				System.out.println(factor1 + " * " + factor2 + " = " + product);
			} else
				System.out.println("Wrong shit " + factor1 + " * " + factor2 + " = " + product);
		}

		System.out.println("Time: " + (doneTime - startTime) / 1E6 + "ms");

	}

	public static void main(String[] args) {
		long product = Long.parseLong(args[0]);
		int threads = Integer.parseInt(args[1]);

		Factorizer factor = new Factorizer(product, threads);
		factor.start();
	}

	static boolean isPrime(long number) {
		boolean result = true;
		for (long denominator = 2; denominator < Math.sqrt(number); denominator++) {
			if (number % denominator == 0)
				result = false;
		}
		return result;
	}

//	static boolean isPrime(long number) {
//		// will contain true or false values for the first 10,000 integers
//		boolean[] primes = new boolean[(int) number+1];
//
//		Arrays.fill(primes, true); // assume all integers are prime.
//		primes[0] = primes[1] = false;
//		for (int i = 2; i < primes.length; i++) {
//			if (primes[i]) {
//				for (int j = 2; i * j < primes.length; j++) {
//					primes[i * j] = false;
//				}
//			}
//		}
//		return primes[(int) number]; // simple, huh?
//	}

}

class SeveralLatchWaiters {
	CountDownLatch[] latches;

	public SeveralLatchWaiters(CountDownLatch[] list) {
		latches = list;
	}

	public void await(int timeout) throws InterruptedException {
		
		while (true) {
			for (CountDownLatch countDownLatch : latches) {
				countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
			if (countDownLatch.getCount() == 0) {
				return;
			}
			}
		}
	}
}

class Work implements Runnable {

	long min, max, factor1, factor2, product, step;
	CountDownLatch done;
	CountDownLatch notFound;
	volatile boolean working;

	public Work(long min, long product, int step, CountDownLatch done, CountDownLatch notFound) {
		this.min = min;
		this.max = product;
		this.product = product;
		this.step = step;
		this.done = done;
		this.notFound = notFound;
	}

	public void run() {
		working = true;
		long number = min;
		while (number <= max && working) {
			if (product % number == 0 && Factorizer.isPrime(number)) {
				factor1 = number;
				factor2 = product / factor1;
				done.countDown();
				return;
			}
			number = number + step;
		}
		notFound.countDown();
	}

	public void shutdown() {
		working = false;
	}
}