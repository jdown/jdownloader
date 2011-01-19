package jd.updater;

import java.util.Iterator;
import java.util.LinkedList;

public class RandomIterator<T> implements Iterator<T> {

    public static void main(final String[] args) {
        for (final RandomIterator<Integer> it = new RandomIterator<Integer>(new Integer[] { 1, 2, 3, 4, 5 }); it.hasNext();) {
            System.out.println(it.next());
        }
    }

    private final LinkedList<T> unserved;
    private T                   current;
    private final T[]           list;

    private boolean             untouched;

    public RandomIterator(final T[] list) {

        unserved = new LinkedList<T>();
        this.list = list;

        reset();

    }

    public T curr() {
        if (untouched) {
            next();
        }
        return current;
    }

    @Override
    public boolean hasNext() {

        return unserved.size() > 0;
    }

    @Override
    public T next() {
        untouched = false;
        final int index = (int) (Math.random() * (unserved.size() + 0));
        current = unserved.remove(index);
        return current;
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not Implemented");

    }

    public void reset() {
        current = null;
        untouched = true;
        unserved.clear();
        for (final T element : list) {
            unserved.add(element);
        }
    }
}
