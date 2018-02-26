// Peter Idestam-Almquist, 2018-02-21.

package w02;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

// TODO: Make this class thread-safe and as performant as possible.
class Bank {
	// Instance variables.
	private int accountCounter = 0;
	private Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	
	// Instance methods.
	
	int newAccount(int balance) {
		int accountId = accountCounter++;
		Account account = new Account(accountId, balance);
		accounts.put(accountId, account);
		return accountId;
	}
	
	void runOperation(Operation operation) {
		Account account = accounts.get(operation.getAccountId());
		synchronized(account) {
			account.setBalance(account.getBalance() + operation.getAmount());
		}
	}
		
	// TODO: If you are not aiming for grade VG you should remove this method.
	void runTransaction(Transaction transaction) {
		@SuppressWarnings("unused")
		List<Integer> accountIds = transaction.getAccountIds();
		List<Operation> operations = transaction.getOperations();
		
		for (Operation operation : operations) {
			Account acc = accounts.get(operation.getAccountId());
			synchronized(acc) {
				acc.setBalance(acc.getBalance() + operation.getAmount());
			}
		}
		
//		for (int i = 0; i < operations.size(); i++) { // Slower? At least uglier
//		Operation operation = operations.get(i);
//		Account acc = accounts.get(operation.getAccountId());
//		synchronized(acc) {
//			acc.setBalance(acc.getBalance() + operation.getAmount());
//		}
//	}
		
//		operations.forEach(opereration -> { // Even slower
//			Account acc = accounts.get(opereration.getAccountId());
//			synchronized(acc) {
//				acc.setBalance(acc.getBalance() + opereration.getAmount());
//			}
//		});
		

//		operations.parallelStream().forEach(operation -> { // Slowest
//			Account acc = accounts.get(operation.getAccountId());
//			synchronized(acc) {
//				acc.setBalance(acc.getBalance() + operation.getAmount());
//			}
//		});
	}
	
	// Not used for anything except printing
	int getAccountBalance(int accountId) {
		Account account = accounts.get(accountId);
		return account.getBalance();
	}
}
