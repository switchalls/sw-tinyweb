package sw.tinyweb.utils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Utility for converting a {@link Collection} to an enumeration.
 * 
 * @param <T> The collection type
 * @author $Author: $
 * @version $Revision: $
 */
public class IteratorEnumeration<T> implements Enumeration<T>
{
	private Iterator<T> iterator;

	/**
	 * Constructor.
	 * 
	 * @param aList The collection
	 */
	public IteratorEnumeration(Collection<T> aList)
	{
		this( aList.iterator() );
	}

	/**
	 * Constructor.
	 * 
	 * @param aIter The iterator
	 */
	public IteratorEnumeration(Iterator<T> aIter)
	{
		this.iterator = aIter;
	}

	@Override
	public boolean hasMoreElements()
	{
		return this.iterator.hasNext();
	}

	@Override
	public T nextElement()
	{
		return this.iterator.next();
	}
	
}
