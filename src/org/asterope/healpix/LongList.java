//
// Licenced under GPLv2, see licence.txt
// (c)  Jan Kotek,
//

package org.asterope.healpix;


import java.io.Serializable;
import java.util.*;


/**
* Growable long[] array
*/
public class LongList implements Serializable {

    private static final long serialVersionUID = -2794240565359961009L;

    protected int size = 0;
    protected long[] data = new long[32];


    public LongList(LongRangeSet longRangeSet) {
        addAll(longRangeSet.longIterator());
    }

    /**
     * add all values from given array
     * @param vals long array to add
     */
    public void addAll(long[] vals){
        for(long v :vals) add(v);
    }

    public void addAll(LongRangeSet set) {
        addAll(set.longIterator());
    }

    public void addAll(LongList set) {
        addAll(set.longIterator());
    }

    /**
     * add all values from given iterator
     * @param iter LongIterator
     */
    public void addAll(LongIterator iter) {
        while(iter.hasNext())
            add(iter.next());
    }


    /**
     * add all values from given range (inclusive)
     * @param first
     * @param last
     */
    public void addRange(long first, long last){
        for(long v = first;v<=last ; v++)
            add(v); //TODO sorted access can be optimized
    }


    public void add(long v) {
        add(size, v);
    }

    public LongList() {
    }

    public LongList(LongList set) {
        addAll(set.longIterator());
    }

    public LongList(Collection<Long> s) {
        for(Long l :s)
            add(l);
    }



    public LongList(long[] set) {
        addAll(set);
    }


	public void clear() {
        size = 0;
        data = new long[32];
    }

    public LongIterator longIterator() {
        return new LongIterator(){

            int pos = 0;
            public boolean hasNext() {
                return pos<size;
            }

            public long next(){
                if(pos>=size) throw new NoSuchElementException();
                pos++;
                return data[pos-1];
            }
        };
    }


    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(long v) {
        for (int i = 0; i < size; i++)
            if (data[i] == v)
                return true;
        return false;
    }

    public int indexOf(long c) {
        for (int i = 0; i < size; i++)
            if (data[i] == c)
                return i;
        return -1;
    }


    public void remove(long v) {
        int index = indexOf(v);
        if (index != -1) {
            removeElementAt(index);

        }
    }

    public void add(int index, long v) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException();
        ensureCapacity(size+1);
        //  Move data
        int block = size-index;
        if (block > 0)
            System.arraycopy(data, index, data, index+1, block);
        data[index] = v;
        size++;
    }

    public long get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        return data[index];
    }

    public long set(int index, long v) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        long result = data[index];
        data[index] = v;
        return result;
    }

    public long removeElementAt(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        long result = data[index];
        //  Move data
	    int block = size-(index+1);
        if (block > 0)
            System.arraycopy(data, index+1, data, index, block);
        size--;
        return result;
    }

    /**
     *  Ensures that this list has at least a specified capacity.
     *  The actual capacity is calculated from the growth factor
     *  or growth chunk specified to the constructor.
     *
     *  @param      capacity
     *              the minimum capacity of this list.
     *
     *  @return     the new capacity of this list.
     *
     */
    public int ensureCapacity(int capacity) {
        if (capacity > data.length) {
            capacity = Math.max(capacity, data.length * 2);
            long[] newdata = new long[capacity];
            System.arraycopy(data, 0, newdata, 0, size);
            data = newdata;
        }
        return capacity;
    }

    /** @return this array list, but sorted*/
    public LongList sort(){
        long[] data = toArray();
        Arrays.sort(data);
        return new LongList(data);
    }


    public TreeSet<Long> toTreeSet() {
        TreeSet<Long> ret = new TreeSet<Long>();
        LongIterator iter = longIterator();
        while(iter.hasNext())
            ret.add(iter.next());
        return ret;
    }


    /**
     * @return  array of elements in collection
     */
    public long[] toArray(){
        long[] ret = new long[size()];
        LongIterator iter = longIterator();
        for(int i=0;iter.hasNext();i++)
            ret[i] = iter.next();
        return ret;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('[');
        LongIterator i = longIterator();
        while (i.hasNext()) {
            if (s.length() > 1)
                s.append(',');
            s.append(i.next());
        }
        s.append(']');
        return s.toString();
    }

    public Iterator<Long> iterator() {
        return new Iterator<Long>(){

            final LongIterator iter =longIterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Long next() {
                return iter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


}
