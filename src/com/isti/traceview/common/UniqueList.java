package com.isti.traceview.common;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List which can contain only unique elements
 * 
 * @author Max Kokoulin
 */
public class UniqueList<E> extends ArrayList<E> {
	public boolean add(E e) {
		if (!super.contains(e)) {
			return super.add(e);
		} else {
			return false;
		}
	}

	public void add(int index, E element) {
		if (!super.contains(element)) {
			super.add(index, element);
		}
	}

	public boolean addAll(Collection<? extends E> c) {
		for (E o: c) {
			if (super.contains(o))
				return false;
		}
		return super.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		for (E o: c) {
			if (super.contains(o))
				return false;
		}
		return super.addAll(index, c);
	}
}
