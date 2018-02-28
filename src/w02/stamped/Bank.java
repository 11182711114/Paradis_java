package w02.stamped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * @author Peter Idestam-Almquist
 * @author Heavily modified by Fredrik Larsson frla9839
 *
 */

/* 
 * I belive that this entire problem is a bit bad for testing performance, 
 * the NQ(N = Accounts, Q = transaction/operation) value of the program does not justify it being multithreaded, 
 * as shown by running the original not threadsafe program sequencially is faster than doing it multithreaded (With errors)
 * and the overhead cost of multithreading will always outweight the benefits as the transactions/operations are very light-weight.
 */
public class Bank {
	
	/** Simple wrapper bundling an {@link Account} with a {@link StampedLock}
	 * @author Fredrik
	 */
	class AccountAndLockWrapper {
		Account acc;
		StampedLock accLock;
		
		public AccountAndLockWrapper(Account acc) {
			this.acc = acc;
			accLock = new StampedLock();
		}
		public Account getAccount() { return acc; }
		public StampedLock getLock() { return accLock; }
	}
	
	// Instance variables.
	private int accountCounter = 0;
	
		// While not covered in the standard test, HashMap is not thread-safe and can break if accounts are added while transaction/operation work is running,
		// ConcurrentHashMap does striped locking and thus does not heavily impact performance unless all read/writes happen in the same stripe.
	private Map<Integer, AccountAndLockWrapper> accounts = new ConcurrentHashMap<>();
	
	// Instance methods.
	
	int newAccount(int balance) {
		int accountId = accountCounter++;
		Account account = new Account(accountId, balance);
		AccountAndLockWrapper alw = new AccountAndLockWrapper(account);
		accounts.put(accountId, alw);
		return accountId;
	}
	
	void runOperation(Operation operation) {
		AccountAndLockWrapper awl = accounts.get(operation.getAccountId());
		StampedLock lock = awl.getLock();
		long stamp = lock.writeLock();
		try {
			doActualOperation(operation, awl.getAccount());
		} finally {
			lock.unlock(stamp);
		}
	}
	 
	/** Actual operation, the input account should be locked from the caller
	 * @param operation the operation
	 * @param account the account
	 */
	private void doActualOperation(Operation operation, Account account) {
		account.setBalance(account.getBalance() + operation.getAmount());		
	}
		
	/** Tries to lock all of the accounts in accountIds storing the stamps in stamps, records the balance in the accounts.
	 * @param accountIds - The accounts to lock
	 * @param rollbacks - The {@link Map} to store the balance in
	 * @param stamps - The {@link Map} to store the lock stamps in
	 * @param timeoutms - Time out period of trying to lock the accounts(individual)
	 * @param sleepFloor - Minimum amount of time to sleep if timed out waiting for a lock
	 * @param sleepCeil - Max amount of time to sleep if timed out waiting for a lock
	 */
	private void lockAll(List<Integer> accountIds, Map<Integer, Integer> rollbacks, Map<Integer, Long> stamps, int timeoutms, int sleepFloor, int sleepCeil) {
		boolean failedLock = false;
		do {
			// If we failed last run
			if (failedLock) {
				try {
					int sleepTime = ThreadLocalRandom.current().nextInt(sleepFloor, sleepCeil);
					System.out.println(Thread.currentThread().getName() + "::" + Thread.currentThread().getId()+" Failed aquiring atleast one lock, sleeping for "+ sleepTime +"ms before trying again");
					Thread.sleep(sleepTime); // Sleep for 100-500ms
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} finally {
					rollbacks.clear();
					stamps.clear();
				}
			}
			failedLock = false;
			
			// Aquiring locks
			for (Integer integer : accountIds) {
				AccountAndLockWrapper alw = accounts.get(integer);
				long stamp = 0L;
				try { 
					stamp = alw.getLock().tryWriteLock(timeoutms, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Did we get a lock or timeout?
				if (alw.getLock().validate(stamp)) {
					stamps.put(integer, stamp);
				
					// Remember before balance for rollback
					Account acc = alw.getAccount();
					rollbacks.put(acc.getId(), acc.getBalance());
				} else {
					stamps.forEach((id, unlockStamp) -> { accounts.get(id).getLock().unlock(unlockStamp); });
					failedLock = true;
					break;
				}
			}
		} while(failedLock);
	}
	
	/** Performs the given operations, if exception is raised from the operations the accounts are rolled back to previous state according to rollbacks.
	 * 	Finally, unlocks all the accounts in stamps
	 * @param operations - The operations to execute
	 * @param rollbacks - Account.id => Before balance
	 * @param stamps - Account.id => Lock stamp
	 */
	private void doTransactionOperation(List<Operation> operations, Map<Integer, Integer> rollbacks, Map<Integer, Long> stamps) {
		// Operation stuff
		try {
			for (Operation operation : operations) {
				AccountAndLockWrapper alw = accounts.get(operation.getAccountId());
				doActualOperation(operation, alw.getAccount());
			}
		} catch (IllegalArgumentException e) { // Since this example is a bit simple, there are no apperant exceptions that are ever raised.
			// Rollback all if anything goes wrong, the account are already locked
			System.out.println("Something went wrong, rolling back");
			rollbacks.forEach((id, value) -> { accounts.get(id).getAccount().setBalance(value); });
		} finally {
			// Unlock all
			stamps.forEach((id, stamp) -> { accounts.get(id).getLock().unlock(stamp); });
		}
		
	}
	
	/** Runs all the operations in a transaction, if one fails all are rolled back to the original state.
	 * @param transaction - The transaction to run
	 */
	// Should throw TimeoutException if we cannot lock the account in time to avoid deadlocks but that requires changes to the other files which are not handed in.
	void runTransaction(Transaction transaction) {
		List<Integer> accountIds = transaction.getAccountIds();
		List<Operation> operations = transaction.getOperations();
		
		Map<Integer, Integer> rollbacks = new HashMap<>();	
		Map<Integer, Long> stamps = new HashMap<Integer, Long>();
		
		lockAll(accountIds, rollbacks, stamps, 50, 50, 101);
		doTransactionOperation(operations, rollbacks, stamps);
	}
	
	/** Returns the balance of the account with the {@code accoundId}
	 * @param accountId - The account to get the balance of
	 * @return {@code int} - the balance of the account
	 */
	public int getAccountBalance(int accountId) {
		AccountAndLockWrapper alw = accounts.get(accountId);
		StampedLock lock = alw.getLock();
		
		long stamp = lock.tryOptimisticRead();
		Account account = alw.getAccount();
		int toReturn = account.getBalance();
		
		// optimistic read failed
		if (!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				toReturn = account.getBalance();
			} finally {
				lock.unlock(stamp);
			}
		}
		
		return toReturn;
	}
}
