// Fredrik Larsson frla9839
package w04.task2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public class CopyOnWriteArrayList<E> implements Collection<E> {
	
	public static void main(String[] args) {
		int iters = 50000;
		
		CopyOnWriteArrayList<Integer> arr = new CopyOnWriteArrayList<>();
		java.util.concurrent.CopyOnWriteArrayList<Integer> ref = new java.util.concurrent.CopyOnWriteArrayList<>();
		ArrayList<Integer> list = new ArrayList<>();
		
		long start1 = System.nanoTime();
		IntStream.range(0, iters).parallel().forEach(arr::add);
		long end1 = System.nanoTime();
		IntStream.range(0, iters).parallel().forEach(ref::add);
		long end2 = System.nanoTime();
		IntStream.range(0, iters).parallel().forEach(i -> {
			synchronized(list) {
				list.add(i);
			}
		});
		long end3 = System.nanoTime();

		System.out.println("Arr time [ms]: " + ((end1 - start1) /1E6));
		System.out.println("Ref time [ms]: " + ((end2 - end1) /1E6));
		System.out.println("List time [ms]: " + ((end3 - end2) /1E6));
		System.out.println();
		
		System.out.println("Before removed");
		System.out.println("Arr sum: " + arr.stream().mapToInt(i -> i).sum());
		System.out.println("Ref sum: " + ref.stream().mapToInt(i -> i).sum());
		System.out.println("List sum: " + list.stream().mapToInt(i -> i).sum());
		System.out.println();
		
		int max = iters/2;
		int min = iters/4;
		IntStream.range(0, ThreadLocalRandom.current().nextInt(min,max)).parallel().forEach(i -> {
			boolean arrRemoved = arr.remove(i);
			boolean refRemoved = ref.remove((Object) i);
			boolean listRemoved;
			synchronized(list) {
				listRemoved = list.remove((Object) i);
			}
			if (arrRemoved != refRemoved) {
				int index = arr.indexOf(i);
				System.out.println("Mismatch(" + System.nanoTime() + "): " + i + " ArrRemoved: " + arrRemoved + " Ref: " + refRemoved + " List: "+ listRemoved + "\n\tArr contains? " + arr.contains(i) + ", index: " + index);
			}
		});

		System.out.println();
		System.out.println("After removed");
		System.out.println("Arr sum: " + arr.stream().mapToInt(i -> i).sum());
		System.out.println("Ref sum: " + ref.stream().mapToInt(i -> i).sum());
		System.out.println("List sum: " + list.stream().mapToInt(i -> i).sum());
	}
	
	
	private AtomicReference<E[]> array;
	
	public CopyOnWriteArrayList() {
		array = new AtomicReference<>((E[]) new Object[0]);
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
			while (origArrayPos < tmpOrigArray.length && !changed) {
				if (!tmpOrigArray[origArrayPos].equals(o)) {
					if (newArrayPos == tmpNewArray.length)
						break;
					tmpNewArray[newArrayPos] = tmpOrigArray[origArrayPos];
					newArrayPos++;
				} else {
					changed = true;
				}
				origArrayPos++;
			}
			
			
			if (!changed)
				tmpNewArray = tmpOrigArray;
			else
				System.arraycopy(tmpOrigArray, origArrayPos, tmpNewArray, newArrayPos, tmpOrigArray.length-origArrayPos);
			
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

	public int indexOf(Object o) {
		E[] tmp = array.get();
		int j = -1;
		for (int i = 0; i < tmp.length; i++) {
			if (tmp[i].equals(o)) {
				j = i;
				break;
			}
		}
		return j;
	}

	@Override
	public boolean isEmpty() {
		return array.get().length > 0;
	}

	@Override
	public Iterator<E> iterator() {
		E[] tmp = array.get();
		return Arrays.stream(tmp).iterator();
	}

	@Override
	public void clear() {
		array.set((E[]) new Object[0]);
	}
	
	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
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

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
}
