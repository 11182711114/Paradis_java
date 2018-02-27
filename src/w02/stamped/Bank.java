// Peter Idestam-Almquist, 2018-02-21.

package w02.stamped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

// TODO: Make this class thread-safe and as performant as possible.
class Bank {
	
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
			Account account = awl.getAccount();
			account.setBalance(account.getBalance() + operation.getAmount());
		} finally {
			lock.unlock(stamp);
		}
	}
		
	// TODO: If you are not aiming for grade VG you should remove this method.
	void runTransaction(Transaction transaction) {
		List<Integer> accountIds = transaction.getAccountIds();
		List<Operation> operations = transaction.getOperations();
		Map<Integer, Integer> rollbacks = new HashMap<>();		
		
//		Not eq anymore, doesnt handle rollbacks
//		Map<Integer, Long> stamps = accountIds.stream().collect(Collectors.toMap(id -> id, id -> { return accounts.get(id).getLock().writeLock(); }));
//		Roughly 10% faster 
		Map<Integer, Long> stamps = new HashMap<Integer, Long>( (int) (accountIds.size() * 1.25), 0.75f ); 
		for (Integer integer : accountIds) {
			AccountAndLockWrapper alw = accounts.get(integer);
			long stamp = alw.getLock().writeLock();
			stamps.put(integer, stamp);
			
			// Remember before balance for rollback
			Account acc = alw.getAccount();
			rollbacks.put(acc.getId(), acc.getBalance());
		}
		
		try {
			for (Operation operation : operations) {
				AccountAndLockWrapper alw = accounts.get(operation.getAccountId());				
				Account acc = alw.getAccount();
				acc.setBalance(acc.getBalance() + operation.getAmount());
			}
		} catch (Exception e) {
			// Rollback all if anything goes wrong
			for (Map.Entry<Integer, Integer> entry : rollbacks.entrySet()) {
				accounts.get(entry.getKey())
					.getAccount().setBalance(entry.getValue());
			}			
		} finally {
			// Unlock all
			stamps.forEach((id, stamp) -> { accounts.get(id).getLock().unlock(stamp); });
		}
	}
	
	// Not used for anything except printing
	int getAccountBalance(int accountId) {
		AccountAndLockWrapper alw = accounts.get(accountId);
		StampedLock lock = alw.getLock();
		
		long stamp = lock.tryOptimisticRead();
		Account account = alw.getAccount();
		int toReturn = account.getBalance();
		
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
