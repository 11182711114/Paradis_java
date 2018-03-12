package w04.task2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CopyOnWriteArrayList<T> {	
	AtomicReference<T>[] arr;
	
	private int size = 0;
	
	public CopyOnWriteArrayList() {
		arr = (AtomicReference<T>[]) new AtomicReference[10];
	}
	
	private void expandArray() {
		System.out.println("Expanding array");
		AtomicReference<T>[] newArr = new AtomicReference[size*2];
		for (int i = 0; i < arr.length; i++) {
			newArr[i] = arr[i];
		}
		arr = newArr;
	}
	
	boolean add(T t) {
		if (size+1 >= arr.length)
			expandArray();
		arr[size] = new AtomicReference<T>(t);
		size++; 
		
		return true;	
	}
	
	boolean remove(Object obj) {
		AtomicReference<T>[] newArr = new AtomicReference[arr.length];
		
		boolean removed = false;
		int newArrPos = 0;
		for (int i = 0; i < arr.length; i++) {
			AtomicReference<T> atomicReference = arr[i];
			if (atomicReference == null) 
				continue;
			if (obj.equals(atomicReference.get())) {
				removed = true;
			} else {
				newArr[newArrPos] = arr[i];
				newArrPos++;
			}
			
		}		
		
		arr = newArr;
		
		if (removed)
			size--;
		
		return removed;
	}
	
	void forEach(Consumer<T> action) {
		AtomicReference<T>[] arrTmpRfs = arr;
		for (AtomicReference<T> atomicReference : arrTmpRfs) {
			if (atomicReference == null)
				continue;
			action.accept(atomicReference.get());
		}
		
	}
	
	public static void main(String[] args) {
		CopyOnWriteArrayList<String> cp = new CopyOnWriteArrayList<String>();
//		cp.add("Test");
//		cp.add("Testis");
//		String s = "Testigare";
//		cp.add(s);
//		cp.add("Testigast");
//		
//		cp.forEach(System.out::println);
//		
//		cp.remove(s);
//
//		cp.forEach(System.out::println);
		
		final int steps = 10;
		long timeToDie = (System.currentTimeMillis() + 10000L);
		for(int i = 0; i <= 100; i = i+steps) {
			final int test = i;
			new Thread(new Runnable() {
				int start = new Integer(test);
				@Override
				public void run() {
					for (int j = start; j < start+steps; j++) {
						cp.add(""+j);
					}
					
				}
			}).start();
		}
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		Comparator<AtomicReference<String>> c = new Comparable<String> {
			
//		}
//		Arrays.sort(cp.arr, c);
//		cp.forEach(System.out::println);
	}
	
}
