// Peter Idestam-Almquist, 2018-02-21.

package paradis.assignment2.stamped;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Program {
	// Static variables.
	private static int NUM_THREADS = 12;
	private static int NUM_ACCOUNTS = 6;
	private static int FACTOR = 100000;
	private static int TIMEOUT = 60; // Seconds;
	private static int NUM_TRANSACTIONS = NUM_ACCOUNTS * FACTOR;
	private static Integer[] accountIds = new Integer[NUM_ACCOUNTS];
	private static Operation[] withdrawals = new Operation[NUM_ACCOUNTS];
	private static Operation[] deposits = new Operation[NUM_ACCOUNTS];
	private static Bank bank = new Bank();

	// Static methods.

	private static void initiate() {
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + (NUM_THREADS - 1));
		for (int i = 0; i < NUM_ACCOUNTS; i++) {
			accountIds[i] = bank.newAccount(1000);
		}

		for (int i = 0; i < NUM_ACCOUNTS; i++) {
			withdrawals[i] = new Operation(bank, accountIds[i], -100);
			;
		}

		for (int i = 0; i < NUM_ACCOUNTS; i++) {
			deposits[i] = new Operation(bank, accountIds[i], +100);
			;
		}
	}

	// You may use this test to test thread-safety for operations.
	private static void runTestOperations() {
		 ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
//		ExecutorService executor = new ScheduledThreadPoolExecutor(NUM_THREADS);
		// ExecutorService executor = new ForkJoinPool(NUM_THREADS);

		ReadBalance[] readers = new ReadBalance[NUM_TRANSACTIONS * 2];
		Operation[] operations = new Operation[NUM_TRANSACTIONS * 2];
		for (int i = 0; i < NUM_TRANSACTIONS; i++) {
			operations[i * 2] = withdrawals[i % NUM_ACCOUNTS];
			operations[(i * 2) + 1] = deposits[(i + 1) % NUM_ACCOUNTS];

			readers[i * 2] = new ReadBalance(i % NUM_ACCOUNTS, bank);
			readers[(i * 2) + 1] = new ReadBalance(i % NUM_ACCOUNTS, bank);
		}

		try {
			long time = System.nanoTime();
			IntStream.range(0, operations.length).forEach(i -> {
				executor.execute(operations[i]);
//				executor.execute(readers[i]);
			});
			long loopDone = System.nanoTime();
			Future<Long> done = executor.submit(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					return System.nanoTime();
				}
			});
			executor.shutdown();
			boolean completed = executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
			if (!completed)
				System.out.println("Operation failed to complete something");
			time = System.nanoTime() - time;

			System.out.println("Test operations finished.");
			System.out.format("Delay [ms]: %.0f%n", (done.get() - loopDone)/1E6);
			System.out.println("Completed: " + completed);
			System.out.println("Time [ms]: " + time / 1000000);

			for (int i = 0; i < NUM_ACCOUNTS; i++) {
				int balance = bank.getAccountBalance(accountIds[i]);
				if (balance != 1000)
					System.out.println("Operation Mismatch:: Account: " + accountIds[i] + ";\tBalance: " + balance);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	// You may use this test to test thread-safety for transactions.
	private static void runTestTransactions() {
		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
		// ExecutorService executor = new ForkJoinPool(NUM_THREADS);

		ReadBalance[] readers = new ReadBalance[NUM_TRANSACTIONS];
		Transaction[] transactions = new Transaction[NUM_TRANSACTIONS];
		for (int i = 0; i < NUM_TRANSACTIONS; i++) {
			transactions[i] = new Transaction(bank);
			transactions[i].add(withdrawals[i % NUM_ACCOUNTS]);
			transactions[i].add(deposits[(i + 1) % NUM_ACCOUNTS]);

			readers[i] = new ReadBalance(i % NUM_ACCOUNTS, bank);
		}

		try {
			long time = System.nanoTime();
			IntStream.range(0, transactions.length).forEach(i -> {
				executor.execute(transactions[i]);
//				executor.execute(readers[i]);
			});
			long loopDone = System.nanoTime();
			Future<Long> done = executor.submit(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					return System.nanoTime();
				}

			});
			executor.shutdown();
			boolean completed = executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
			if (!completed)
				System.out.println("Transaction failed to complete something");
			time = System.nanoTime() - time;

			System.out.println("\nTest transactions finished.");
			System.out.format("Delay [ms]: %.0f%n", (done.get() - loopDone)/1E6);
			System.out.println("Completed: " + completed);
			System.out.println("Time [ms]: " + time / 1000000);

			for (int i = 0; i < NUM_ACCOUNTS; i++) {
				int balance = bank.getAccountBalance(accountIds[i]);
				if (balance != 1000)
					System.out.println("Transaction Mismatch:: Account: " + accountIds[i] + ";\tBalance: " + balance);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	// Entry point.
	public static void run(String[] args) {
		if (args.length > 0) {
			NUM_THREADS = Integer.parseInt(args[0]);
			NUM_ACCOUNTS = Integer.parseInt(args[1]);
			FACTOR = Integer.parseInt(args[2]);
			TIMEOUT = Integer.parseInt(args[3]);
			NUM_TRANSACTIONS = NUM_ACCOUNTS * FACTOR;
			accountIds = new Integer[NUM_ACCOUNTS];
			withdrawals = new Operation[NUM_ACCOUNTS];
			deposits = new Operation[NUM_ACCOUNTS];
		}
		// System.out.println("StampedLock");
		initiate();
		runTestOperations();
		runTestTransactions();
	}
}
