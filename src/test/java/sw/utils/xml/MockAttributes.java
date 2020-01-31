package sw.utils.xml;

import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;

/**
 * Helper class.
 *
 * @author $Author: $ 
 * @version $Revision: $
 */
public class MockAttributes implements Attributes
{
	private final List<String> attrs;

	/**
	 * Constructor.
	 * 
	 * @param aList The list of attribute definitions
	 */
	public MockAttributes(String[] aList)
	{
		this.attrs = Arrays.asList(aList);
	}

	@Override
    public int getLength()
    {
    	return this.attrs.size();
    }

	@Override
    public String getURI(int aIndex)
    {
    	final String qname = this.getQName(aIndex);
    	if ( qname != null )
    	{
	    	final int i = qname.indexOf(':');
	    	return (i < 0) ? "" : qname.substring(0, i-1);
    	}
    	return null;
    }

	@Override
    public String getLocalName(int aIndex)
    {
    	final String qname = this.getQName(aIndex);
    	if ( qname != null )
    	{
	    	final int i = qname.indexOf(':');
	    	return (i < 0) ? qname : qname.substring(i);
    	}
    	return null;
    }

	@Override
    public String getQName(int aIndex)
    {
    	if ( (aIndex < 0) || (aIndex > this.getLength()) )
    	{
    		return null;
    	}

    	final String def = this.attrs.get(aIndex);
    	final int i = def.indexOf('=');
    	return def.substring(0, i);
    }

	@Override
    public String getType(int aIndex)
    {
    	return null;
    }

	@Override
    public String getValue(int aIndex)
    {
    	if ( (aIndex < 0) || (aIndex > this.getLength()) )
    	{
    		return null;
    	}

    	final String def = this.attrs.get(aIndex);
    	final int i = def.indexOf('=');
    	return def.substring(i+1);
    }

	@Override
    public int getIndex(String aUri, String aLocalName)
    {
    	return this.getIndex(aUri + ":" + aLocalName);
    }

	@Override
    public int getIndex(String aQualifiedName)
    {
    	for ( int i=0;  i < this.attrs.size();  i++)
    	{
    		final String def = this.attrs.get(i);
    		if ( def.startsWith(aQualifiedName+"=") )
    		{
    			return i;
    		}
    	}
     	return -1;
    }

	@Override
    public String getType(String aUri, String aLocalName)
    {
    	return this.getType(aUri + ":" + aLocalName);
    }

	@Override
    public String getType(String aQualifiedName)
    {
    	return null;        	
    }

	@Override
    public String getValue(String aUri, String aLocalName)
    {
    	return this.getValue(aUri + ":" + aLocalName);
    }

	@Override
    public String getValue(String aQualifiedName)
    {
    	final int i = this.getIndex(aQualifiedName);
		return (i > -1) ? this.getValue(i) : null;
    }

}
