package paradis.assignment2.sync;

import paradis.assignment2.sync.Bank;

public class ReadBalance implements Runnable {
	private Bank bank;
	private int accountId;
	private int balance = 0;

	public ReadBalance(int accountId, Bank bank) {
		this.accountId = accountId;
		this.bank = bank;
	}
	
	
	@Override
	public void run() {
		balance = bank.getAccountBalance(accountId);
	}
	
	public int getBalance() {
		return balance;
	}
	
}
