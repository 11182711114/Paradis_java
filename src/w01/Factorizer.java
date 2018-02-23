package w01;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Fredrik Larsson frla9839
 */
public class Factorizer {

	private long product;
	private int maxThreads;
	private Work[] workPool;
	// private Thread[] threadPool;

	public Factorizer(long product, int threads) {
		this.product = product;
		this.maxThreads = threads;
		if (threads > product)
			this.maxThreads = (int) product;
		workPool = new Work[maxThreads];
		// threadPool = new Thread[maxThreads];
	}

	public void start() {
		long startTime = System.nanoTime();
		
		// Thread management
		CountDownLatch done = new CountDownLatch(1);
		CountDownLatch notFound = new CountDownLatch(maxThreads);
		CountDownLatch[] list = { done, notFound };
		SeveralLatchWaiter slw = new SeveralLatchWaiter(list);
		
		// Start threads
		for (int i = 0; i < maxThreads; i++) {
			Work work = new Work(2 + i, product, maxThreads, done, notFound);
			Thread thread = new Thread(work);
			// threadPool[i] = thread;
			workPool[i] = work;
			thread.start();
		}
		
		// Wait for the threads to finish, either by one finding a solution or all threads failing
		try {
			slw.await(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Get the result
		// Note: if the threads find multiple incorrect solutions 
		//			only the last is output
		long factor1 = 0L, factor2 = 0L;
		for (Work work : workPool) {
			if (work.getFactor1() != 0L) { // If factor1 != 0 then so is factor2
				factor1 = work.getFactor1();
				factor2 = work.getFactor2();
			}
			work.shutdown();
		}
		

		// This is unnecessary, since factor1 is checked to be a prime inside the threads and product is only checked if factor2 is not a prime
//		CountDownLatch primeCheckLatch = new CountDownLatch(2);
//		PrimeCheck[] primePool = new PrimeCheck[3];
//		primePool[0] = new PrimeCheck(product, primeCheckLatch);
//		primePool[1] = new PrimeCheck(factor1, primeCheckLatch);
//		primePool[2] = new PrimeCheck(factor2, primeCheckLatch);
//		for (PrimeCheck primeCheck : primePool) {
//			new Thread(primeCheck).start();
//		}
//		try {
//			primeCheckLatch.await();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		boolean isProductPrime = primePool[0].isPrime();
//		boolean isFactor1Prime = primePool[1].isPrime();
		boolean isFactor2Prime = new PrimeCheck(factor2).checkPrime();
		
		// Output
			// Result output
		if (isFactor2Prime) // Correct result
			System.out.format("%d * %d = %d%n", factor1, factor2, product);
		else if (new PrimeCheck(product).checkPrime()) // Check if the product is a prime only when the primary goal has failed, i.e. factor1 or factor2 is not a prime
			System.out.println("No factorization possible");
		else // factor2 is not a prime and the product is not a prime
			System.out.format("Found factor2 is not a prime: %d * %d = %d%n", factor1, factor2, product);
		
			//	Time output
		long doneTime = System.nanoTime();
		System.out.format("Time: %.2fms", (doneTime - startTime) / 1E6);

	}

	public static void main(String[] args) {
		if (args.length > 2 || args.length < 2) {
			System.out.println("Wrong number arguments, quitting");
			System.exit(0);
		}
		long product = Long.parseLong(args[0]);
		int threads = Integer.parseInt(args[1]);

		Factorizer factor = new Factorizer(product, threads);
		factor.start();
	}
}

class SeveralLatchWaiter {
	CountDownLatch[] latches;

	public SeveralLatchWaiter(CountDownLatch[] list) {
		latches = list;
	}

	/** Blocks until one of several {@link CountDownLatch}'s resolve. 
	 * @param timeout - Time to wait for the latch until switching to the next one
	 * @throws InterruptedException
	 */
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

class PrimeCheck {
	private long number;
	
	public PrimeCheck(long number) {
		this.number = number;
	}
	
	/** isPrime, from the prime examples.
	 * @param number - the number to check
	 * @return {@code true} if <b>number</b> is a prime </br>{@code false} if not
	 * @author Peter Idestam-Almquist
	 */
	public boolean checkPrime() {
		if (number <= 1)
			return false;
		boolean result = true;
		for (long denominator = 2; denominator < Math.sqrt(number); denominator++) {
			if (number % denominator == 0)
				result = false;
		}
		return result;
	}

}


class Work implements Runnable {

	private long min, max, factor1, factor2, product, step;

	CountDownLatch done;
	CountDownLatch notFound;
	volatile boolean working;

	public Work(long min, long product, int step, CountDownLatch done, CountDownLatch notFound) {
		this.min = min;
		this.max = (long) Math.sqrt(product);
		this.product = product;
		this.step = step;
		this.done = done;
		this.notFound = notFound;
	}

	public void run() {
		working = true;
		long number = min;
		while (number <= max && working) {
			if (product % number == 0 && new PrimeCheck(number).checkPrime()) {
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
	
	public long getFactor1() {
		return factor1;
	}

	public void setFactor1(long factor1) {
		this.factor1 = factor1;
	}

	public long getFactor2() {
		return factor2;
	}

	public void setFactor2(long factor2) {
		this.factor2 = factor2;
	}

	public boolean isWorking() {
		return working;
	}

	public void setWorking(boolean working) {
		this.working = working;
	}
}