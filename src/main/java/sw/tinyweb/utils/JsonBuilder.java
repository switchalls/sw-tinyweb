package sw.tinyweb.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Helper for creating JSON structures.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class JsonBuilder
{
	/** Default date format. */
	public static final String ISO8641_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	private DateFormat dateFormat;
	private final StringBuilder json = new StringBuilder();
	private boolean requiresComma;
	
	/** Default constructor. */
	public JsonBuilder()
	{
		this.json.append("{");	
		this.requiresComma = false;
		this.setDateFormat(ISO8641_DATE_FORMAT);
	}

	/**
	 * Define the date format to be used.
	 * 
	 * @param aFormat The {@link SimpleDateFormat} format
	 * 
	 * @see #setDateFormat(String, String)
	 */
	public void setDateFormat(String aFormat)
	{
		this.setDateFormat( aFormat, "UTC" );
	}
	
	/**
	 * Define the date format to be used.
	 * 
	 * @param aFormat The {@link SimpleDateFormat} format
	 * @param aTimeZone The {@link TimeZone}
	 * 
	 * @see #setProperty(String, Date)
	 */
	public void setDateFormat(String aFormat, String aTimeZone)
	{
		this.dateFormat = new SimpleDateFormat(ISO8641_DATE_FORMAT);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone(aTimeZone));
	}

	/**
     * Append the stated text verbatim.
     * 
     * @param aText The text to be added
     * @return the builder
     */
	public JsonBuilder append(String aText)
	{
		this.json.append(aText);
		return this;
	}
	
	/**
	 * Append the stated property value.
	 * 
	 * @param aName The property name
	 * @param aValue The property value
     * @return the builder
     * 
     * @see #appendPropertyValue(String, String)
	 */
	public JsonBuilder setProperty(String aName, String aValue)
	{
		final String s = aValue.replaceAll("'", "\\'");
		return this.appendPropertyValue( aName, "'"+s+"'");
	}

	/**
	 * Append the stated property value.
	 * 
	 * @param aName The property name
	 * @param aValue The property value
     * @return the builder
     * 
     * @see #setDateFormat(String)
     * @see #appendPropertyValue(String, String)
	 */
	public JsonBuilder setProperty(String aName, Date aValue)
	{
		final String s = this.dateFormat.format(aValue);
		return this.setProperty( aName, s);
	}

	/**
	 * Append the stated property value.
	 * 
	 * @param aName The property name
	 * @param aValue The property value
     * @return the builder
     * 
     * @see #appendPropertyValue(String, String)
	 */
	public JsonBuilder setProperty(String aName, long aValue)
	{
		return this.appendPropertyValue( aName, Long.toString(aValue));
	}

	/**
	 * Append the stated property value.
	 * 
	 * @param aName The property name
	 * @param aValue The property value
     * @return the builder
     * 
     * @see #appendPropertyValue(String, String)
	 */
	public JsonBuilder setProperty(String aName, double aValue)
	{
		return this.appendPropertyValue( aName, Double.toString(aValue));
	}
	
	/**
	 * Start a new record.
	 * 
	 * @param aName The property name
     * @return the builder
	 */
	public JsonBuilder openRecord(String aName)
	{
		if (this.requiresComma)
		{
			this.json.append(",");
		}
		
		this.json.append("'");
		this.json.append(aName);
		this.json.append("': {");

		this.requiresComma = false;
		
		return this;
	}
	
	/**
	 * Close the current record.
	 * 
	 * @return the builder
	 */
	public JsonBuilder closeRecord()
	{
		this.json.append("}");
		
		this.requiresComma = true;
		
		return this;
	}
	
	@Override
    public String toString()
    {
        return this.json.toString() + "}";
    }

	/**
	 * Append the stated property value verbatim.
	 * 
	 * @param aName The property name
	 * @param aValue The property value
     * @return the builder
	 */
	protected JsonBuilder appendPropertyValue(String aName, String aValue)
	{
		if (this.requiresComma)
		{
			this.json.append(",");
		}
		
		this.json.append("'");
		this.json.append(aName);
		this.json.append("':");
		this.json.append(aValue);
		
		this.requiresComma = true;
		
		return this;
	}

}