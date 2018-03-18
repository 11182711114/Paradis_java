// Fredrik Larsson frla9839
package w04;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public class CopyOnWriteArrayList<E> implements Collection<E>, Iterable<E>{
	
	public static void main(String[] args) {
		
		CopyOnWriteArrayList<Integer> arr = new CopyOnWriteArrayList<>();
		java.util.concurrent.CopyOnWriteArrayList<Integer> ref = new java.util.concurrent.CopyOnWriteArrayList<>();
		
		long start1 = System.nanoTime();
		IntStream.range(0, 2000).parallel().forEach(arr::add);
		long end1 = System.nanoTime();
		IntStream.range(0, 2000).parallel().forEach(ref::add);
		long end2 = System.nanoTime();

		System.out.println("Arr time [ms]: " + ((end1 - start1) /1E6));
		System.out.println("Ref time [ms]: " + ((end2 - end1) /1E6));
		System.out.println();
		
		System.out.println("Before removed");
		System.out.println("Arr sum: " + arr.stream().mapToInt(i -> i).sum());
		System.out.println("Ref sum: " + ref.stream().mapToInt(i -> i).sum());
		System.out.println();
		
//		IntStream.range(0, 1001).parallel().forEach(i -> {
//			Integer remove = ThreadLocalRandom.current().nextInt(0, 1001);
//			boolean arrRemoved = arr.remove(remove);
//			boolean refRemoved = ref.remove(remove);
//			if (arrRemoved != refRemoved) {
//				int index = arr.indexOf(remove);
//				System.out.println("Mismatch(" + System.nanoTime() + "): " + remove + " ArrRemoved: " + arrRemoved + " Ref: " + refRemoved + "\n\tArr contains? " + arr.contains(remove) + ", index: " + index);
//			}
//		});
		
		ForkJoinPool.commonPool().awaitQuiescence(500, TimeUnit.SECONDS);
		
//		ref.sort((i, j) -> {
//			return i.compareTo(j);			
//		});
//		arr.sort();
		
		IntStream.range(0, 1001).parallel().forEach(i -> {
			boolean arrRemoved = arr.remove(i);
			boolean refRemoved = ref.remove((Object) i);
			if (arrRemoved != refRemoved) {
				int index = arr.indexOf(i);
				System.out.println("Mismatch(" + System.nanoTime() + "): " + i + " ArrRemoved: " + arrRemoved + " Ref: " + refRemoved + "\n\tArr contains? " + arr.contains(i) + ", index: " + index);
			}
		});

		System.out.println();
		System.out.println("After removed");
		System.out.println("Arr sum: " + arr.stream().mapToInt(i -> i).sum());
		System.out.println("Ref sum: " + ref.stream().mapToInt(i -> i).sum());
	}
	
	
	private AtomicReference<E[]> array;
	
	public CopyOnWriteArrayList(int initialCapacity) {
		array = new AtomicReference<>((E[]) new Object[initialCapacity]);
	}
	
	public CopyOnWriteArrayList() {
		this(0);
	}
	
	@Override
	public boolean add(E e) {
		E[] tmpOrigArray;
		E[] tmp;
		do {
			tmpOrigArray = array.get();
			tmp = (E[]) new Object[tmpOrigArray.length+1];
			
			System.arraycopy(tmpOrigArray, 0, tmp, 0, tmpOrigArray.length);
			tmp[tmp.length-1] = e;
		
		} while (!array.compareAndSet(tmpOrigArray, tmp));
			
		return true;
	}

	@Override
	public boolean remove(Object o) {
		boolean changed;
		E[] tmpOrigArray;
		E[] tmpNewArray;
		
		do {
			changed = false;
			tmpOrigArray = array.get();
			if (tmpOrigArray.length == 0)
				return false;
			
			tmpNewArray = (E[]) new Object[tmpOrigArray.length-1];

			
			int origArrayPos = 0;
			int newArrayPos = 0;
			while (origArrayPos < tmpOrigArray.length && newArrayPos < tmpNewArray.length) {
				if (!tmpOrigArray[origArrayPos].equals(o)) {
					tmpNewArray[newArrayPos] = tmpOrigArray[origArrayPos];
					newArrayPos++;
				} else {
					changed = true;
				}
				origArrayPos++;
			}
			
			if (!changed)
				tmpNewArray = tmpOrigArray;
			
		} while (!array.compareAndSet(tmpOrigArray, tmpNewArray));
		
		return changed;
	}
	
	@Override
	public void forEach(Consumer<? super E> action) {
		E[] arr = array.get();
		for(E e : arr) {
			action.accept(e);
		}
	}

	public void sort() {
		E[] orig;
		E[] tmp;
		do {
			orig = array.get();
			tmp = (E[]) new Object[orig.length];
			System.arraycopy(orig, 0, tmp, 0, orig.length);
			
			Arrays.sort(tmp);
		} while(!array.compareAndSet(orig, tmp));
	}
	
	@Override
	public int size() {
		return array.get().length;
	}

	@Override
	public Object[] toArray() {
		return array.get();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		E[] tmp = array.get();
		if (a.length >= tmp.length) {
			IntStream.range(0,tmp.length).forEach(i -> {
				a[i] = (T) tmp[i]; 
			});
		}
		return a;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public int indexOf(Object o) {
		E[] tmp = array.get();
		int i;
		for (i = 0; i < tmp.length; i++) {
			if (tmp[i].equals(o))
				break;
		}
		return i;
	}
	
	@Override
	public boolean contains(Object o) {
		E[] tmp = array.get();
		
		return Arrays.stream(tmp).anyMatch(elem -> elem.equals(o));
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<E> iterator() {
		E[] tmp = array.get();
		return Arrays.stream(tmp).iterator();
	}
}
