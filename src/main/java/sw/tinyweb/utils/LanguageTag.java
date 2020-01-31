package sw.tinyweb.utils;

import java.util.Locale;

/**
 * RFC 1766 language tag with quality level.
 * 
 * @author $Author: $
 * @version $Revision: $
 * 
 * @see LocaleProvider
 */
public class LanguageTag
{
	private static final LocaleProvider LOCALE_PROVIDER = new LocaleProvider();
	
	private final String language;
	private double qualityLevel = 1.0;

	/**
	 * Constructor.
	 * 
	 * @param aLanguage The RFC 1766 language tag
	 */
	public LanguageTag(String aLanguage)
	{
		this( aLanguage, 1.0 );
	}
	
	/**
	 * Constructor.
	 * 
	 * @param aLanguage The RFC 1766 language tag
	 * @param aQualityLevel The quality level
	 * @throws IllegalArgumentException when the value is rejected
	 */
	public LanguageTag(String aLanguage, Double aQualityLevel)
	throws IllegalArgumentException
	{
		this.language = aLanguage;
		this.setQualityLevel( aQualityLevel );
	}

	/** @return the RFC 1766 language tag */
	public String getLanguage()
	{
		return this.language;
	}
	
	/** @return the Java locale */
	public Locale getLocale()
	{
		return LOCALE_PROVIDER.fromString( this.language );
	}
	
	/** @return the quality level */
	public double getQualityLevel()
	{
		return this.qualityLevel;
	}
	
	/**
	 * Change the quality level.
	 * 
	 * @param aQuality The new value
	 * @throws IllegalArgumentException when the value is rejected
	 */
	public void setQualityLevel(double aQuality)
	{
		if ( aQuality < 0 || aQuality > 1.0 )
		{
			throw new IllegalArgumentException( "Inavlid language tag quality level: "+aQuality );
		}
		this.qualityLevel = aQuality;
	}
	
}
