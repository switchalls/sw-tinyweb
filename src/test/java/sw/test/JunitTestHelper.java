package sw.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * Collection of useful 'generic' functions for JUnit tests.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class JunitTestHelper
{
    /**
     * Is <code>aValue</code> between <code>aExpected</code> and
     * <code>aExpected</code> + <code>aElapsedMilli</code>?
     * 
     * @param aTestMessage The test's Id
     * @param aExpected The start time
     * @param aElapsedMilli The max elapsed time
     * @param aValue The date + time in question
     * @throws AssertionFailedError when the test fails
     */
    public static void assertMaxElapsedTime(
            String aTestMessage,
            Date aExpected,
            int aElapsedMilli,
            Date aValue)
    throws  AssertionFailedError
    {
        if ( aExpected == null )
        {
            Assert.assertNull( aTestMessage, aValue );
            return;
        }

        Assert.assertNotNull( aTestMessage, aValue );

        Calendar cal = Calendar.getInstance();
        cal.setTime( aExpected );
        cal.add( Calendar.MILLISECOND, aElapsedMilli );

        Date maxd = cal.getTime();
        
        Assert.assertTrue(
            aTestMessage+": expected <"+maxd+"> but got <"+aValue+">",
            maxd.after(aValue)
        );
    }

    /**
     * Do the 2 dates equal?
     * 
     * <p>The milli-second field(s) are not included in the comparison.
     * </p>
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected value
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     */
    public static void assertEquals(
            String aTestMessage,
            Date aExpected,
            Date aValue)
    throws  AssertionFailedError
    {
        if ( aExpected == null )
        {
            Assert.assertNull( aTestMessage, aValue );
            return;
        }

        Assert.assertNotNull( aTestMessage, aValue );

        Calendar cal = Calendar.getInstance();
        cal.setTime( aExpected );
        cal.set( Calendar.MILLISECOND, 0 );

        Calendar otherCal = Calendar.getInstance();
        otherCal.setTime( aValue );
        otherCal.set( Calendar.MILLISECOND, 0 );
        
        Assert.assertEquals( aTestMessage, cal, otherCal );
    }

    /**
     * Do the 2 streams contain the same content?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected value
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     * @throws IOException when either stream cannot be read
     */
    public static void assertEquals(
            String aTestMessage,
            InputStream aExpected,
            InputStream aValue)
    throws  AssertionFailedError,
            IOException
    {
        if ( aExpected == null )
        {
            Assert.assertNull( aTestMessage, aValue );
            return;
        }

        Assert.assertNotNull( aTestMessage, aValue );

        int c;
        while ( (c = aExpected.read()) > -1 )
        {
            Assert.assertTrue( aTestMessage, ( aValue.read() == c ) );
        }
    }

    /**
     * Do the 2 streams contain the same content?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected value
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     * @throws IOException when either stream cannot be read
     */
    public static void assertEquals(
            String aTestMessage,
            Reader aExpected,
            Reader aValue)
    throws  AssertionFailedError,
            IOException
    {
        if ( aExpected == null )
        {
            Assert.assertNull( aTestMessage, aValue );
            return;
        }

        Assert.assertNotNull( aTestMessage, aValue );

        int c;
        while ( (c = aExpected.read()) > -1 )
        {
            Assert.assertTrue( aTestMessage, ( aValue.read() == c ) );
        }
    }

    /**
     * Do the 2 objects equal?
     * 
     * <p>If <code>aExpected</code> is <code>null</code>,
     * <code>aValue</code> must be <code>null</code>.
     * </p>
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected value
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     */
    public static void assertEquals(
            String aTestMessage,
            Object aExpected,
            Object aValue)
    throws  AssertionFailedError
    {
        if ( aExpected == null )
            Assert.assertNull( aTestMessage, aValue );
        else
        {
            Assert.assertEquals( aTestMessage, aExpected, aValue );
        }
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The list of values
     * @param aExpected The expected value
     * @throws AssertionFailedError when the test fails
     * @see #listContains(char[], char)
     */
    public static void assertContains(
            String aTestMessage,
            char[] aList,
            char aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }
    
    /**
     * Does the array contain the expected value?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The list of values
     * @param aExpected The expected value
     * @throws AssertionFailedError when the test fails
     * @see #listContains(double[], double)
     */
    public static void assertContains(
            String aTestMessage,
            double[] aList,
            double aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The list of values
     * @param aExpected The expected value
     * @throws AssertionFailedError when the test fails
     * @see #listContains(int[], int)
     */
    public static void assertContains(
            String aTestMessage,
            int[] aList,
            int aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The list of values
     * @param aExpected The expected value
     * @throws AssertionFailedError when the test fails
     * @see #listContains(long[], long)
     */
    public static void assertContains(
            String aTestMessage,
            long[] aList,
            long aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }

    /**
     * Does the list contain the expected item?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The values to test
     * @param aExpected The expected item
     * @throws AssertionFailedError when the test fails
     * @see #listContains(Object[], Object)
     */
    public static void assertContains(
            String aTestMessage,
            Object[] aList,
            Object aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }

    /**
     * Does the list contain the expected item?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The values to test
     * @param aExpected The expected item
     * @throws AssertionFailedError when the test fails
     * @see #listContains(Collection, Object)
     */
    public static void assertContains(
            String aTestMessage,
            Collection<?> aList,
            Object aExpected)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aList, aExpected) );
    }

    /**
     * Does the list contain all expected items?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected items
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listContains(Object[], Object[])
     */
    public static void assertContains(
            String aTestMessage,
            Object[] aExpected,
            Object[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aValues, aExpected) );
    }

    /**
     * Does the list contain all expected items?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected items
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listContains(Collection, Collection)
     */
    public static void assertContains(
            String aTestMessage,
            Collection<?> aExpected,
            Collection<?> aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContains(aValues, aExpected) );
    }

    /**
     * Does the list contain unique items only?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listContainsUniqueItemsOnly(Object[])
     */
    public static void assertContainsUniqueItemsOnly(
            String aTestMessage,
            Object[] aList)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContainsUniqueItemsOnly(aList) );
    }

    /**
     * Does the list contain unique items only?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aList The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listContainsUniqueItemsOnly(Collection)
     */
    public static void assertContainsUniqueItemsOnly(
            String aTestMessage,
            Collection<?> aList)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listContainsUniqueItemsOnly(aList) );
    }

    /**
     * Do the arrays contain the same values at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(char[], char[])
     */
    public static void assertEquals(
            String aTestMessage,
            char[] aExpected,
            char[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Do the arrays contain the same values at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(double[], double[])
     */
    public static void assertEquals(
            String aTestMessage,
            double[] aExpected,
            double[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Do the arrays contain the same values at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(int[], int[])
     */
    public static void assertEquals(
            String aTestMessage,
            int[] aExpected,
            int[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Do the arrays contain the same values at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(long[], long[])
     */
    public static void assertEquals(
            String aTestMessage,
            long[] aExpected,
            long[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Do the lists contain the same items at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(Object[], Object[])
     */
    public static void assertEquals(
            String aTestMessage,
            Object[] aExpected,
            Object[] aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Do the lists contain the same items at the same positions?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected values
     * @param aValues The values to test
     * @throws AssertionFailedError when the test fails
     * @see #listEquals(Collection, Collection)
     */
    public static void assertEquals(
            String aTestMessage,
            Collection<?> aExpected,
            Collection<?> aValues)
    throws  AssertionFailedError
    {
        Assert.assertTrue( aTestMessage, listEquals(aExpected, aValues) );
    }

    /**
     * Are the 2 dates (day/month/year) the same?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected date
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     * @see #toDateOnly(Date)
     */
    public static void assertEqualsDateOnly(
            String aTestMessage,
            Date aExpected,
            Date aValue)
    throws  AssertionFailedError
    {
        Assert.assertEquals(
            aTestMessage, toDateOnly(aExpected), toDateOnly(aValue)
        );
    }

    /**
     * Are the 2 times (hour:minute:seconds.milli) the same?
     * 
     * @param aTestMessage The message to display when the test fails
     * @param aExpected The expected time
     * @param aValue The value to test
     * @throws AssertionFailedError when the test fails
     * @see #toTimeOnly(Date)
     */
    public static void assertEqualsTimeOnly(
            String aTestMessage,
            Date aExpected,
            Date aValue)
    throws  AssertionFailedError
    {
        Assert.assertEquals(
            aTestMessage, toTimeOnly(aExpected), toTimeOnly(aValue)
        );
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aList The array of values (can be NULL)
     * @param aExpected The expected value
     * @return true when yes
     */
    public static boolean listContains(char[] aList, char aExpected)
    {
        if ( aList != null )
        {  
            for ( int i=0;  i < aList.length;  i++ )
            {
                if ( aList[i] == aExpected )
                    return true;
            }
        }
        return false;
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aList The array of values (can be NULL)
     * @param aExpected The expected value
     * @return true when yes
     */
    public static boolean listContains(double[] aList, double aExpected)
    {
        if ( aList != null )
        {  
            for ( int i=0;  i < aList.length;  i++ )
            {
                if ( aList[i] == aExpected )
                    return true;
            }
        }
        return false;
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aList The array of values (can be NULL)
     * @param aExpected The expected value
     * @return true when yes
     */
    public static boolean listContains(int[] aList, int aExpected)
    {
        if ( aList != null )
        {  
            for ( int i=0;  i < aList.length;  i++ )
            {
                if ( aList[i] == aExpected )
                    return true;
            }
        }
        return false;
    }

    /**
     * Does the array contain the expected value?
     * 
     * @param aList The array of values (can be NULL)
     * @param aExpected The expected value
     * @return true when yes
     */
    public static boolean listContains(long[] aList, long aExpected)
    {
        if ( aList != null )
        {  
	        for ( int i=0;  i < aList.length;  i++ )
	        {
	            if ( aList[i] == aExpected )
	                return true;
	        }
        }
        return false;
    }

    /**
     * Does the list contain the expected item?
     * 
     * @param aList The list (can be NULL)
     * @param aExpected The expected item
     * @return true when yes
     */
    public static boolean listContains(Object[] aList, Object aExpected)
    {
        if ( aList == null )
            return ( aExpected == null );
        
        if ( aExpected == null )
            return true;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i].equals(aExpected) )
                return true;
        }
        
        return false;
    }

    /**
     * Does the list contain the expected item?
     * 
     * @param aList The list (can be NULL)
     * @param aExpected The expected item (can be NULL)
     * @return true when yes
     */
    public static boolean listContains(Collection<?> aList, Object aExpected)
    {
        if ( aList == null )
            return ( aExpected == null );
            
        return listContains( aList.toArray(), aExpected );            
    }

    /**
     * Does the list contain all the expected items?
     * 
     * <p>Returns <code>true</code> when no items are expected.
     * </p>
     * 
     * <p>Returns <code>false</code> when items are expected, but the list
     * is empty.
     * </p>
     * 
     * @param aList The list (can be NULL)
     * @param aExpected The expected items (can be NULL)
     * @return true when yes
     * @see #listContains(Object[], Object)
     */
    public static boolean listContains(Object[] aList, Object[] aExpected)
    {
        if ( aExpected == null )
            return true;

        if ( aList == null )
            return false;
        
        for ( int i=0;  i < aExpected.length;  i++ )
        {
            if ( listContains(aList, aExpected[i]) == false )
                return false;
        }

        return true;
    }

    /**
     * Does the list contain all expected items?
     * 
     * @param aList The list (can be null)
     * @param aExpected The expected items (can be NULL)
     * @return true when yes
     * @see #listContains(Object[], Object[])
     */
    public static boolean listContains(Collection<?> aList, Collection<?> aExpected)
    {
        if ( aExpected == null )
            return true;

        if ( aList == null )
            return false;
        
        return listContains( aList.toArray(), aExpected.toArray() );            
    }

    /**
     * Do the 2 lists contain the same values at the same positions?
     * 
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     */
    public static boolean listEquals(char[] aList, char[] aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        if ( aOther == null )
            return false;
        
        if ( aList.length != aOther.length )
            return false;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i] != aOther[i] )
                return false;
        }
        
        return true;
    }

    /**
     * Do the 2 lists contain the same values at the same positions?
     * 
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     */
    public static boolean listEquals(double[] aList, double[] aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        if ( aOther == null )
            return false;
        
        if ( aList.length != aOther.length )
            return false;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i] != aOther[i] )
                return false;
        }
        
        return true;
    }

    /**
     * Do the 2 lists contain the same values at the same positions?
     * 
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     */
    public static boolean listEquals(int[] aList, int[] aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        if ( aOther == null )
            return false;
        
        if ( aList.length != aOther.length )
            return false;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i] != aOther[i] )
                return false;
        }
        
        return true;
    }

    /**
     * Do the 2 lists contain the same values at the same positions?
     * 
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     */
    public static boolean listEquals(long[] aList, long[] aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        if ( aOther == null )
            return false;
        
        if ( aList.length != aOther.length )
            return false;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i] != aOther[i] )
                return false;
        }
        
        return true;
    }

    /**
     * Do the 2 lists contain the same items at the same positions?
     * 
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     */
    public static boolean listEquals(Object[] aList, Object[] aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        if ( aOther == null )
            return false;
        
        if ( aList.length != aOther.length )
            return false;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            if ( aList[i].equals(aOther[i]) == false )
                return false;
        }
        
        return true;
    }

    /**
     * Do the 2 lists contain the same items at the same positions?
     *  
     * @param aList The first list (can be NULL)
     * @param aOther The second list (can be NULL)
     * @return true when yes
     * @see #listEquals(Object[], Object[])
     */
    public static boolean listEquals(Collection<?> aList, Collection<?> aOther)
    {
        if ( aList == null )
            return ( aOther == null );
        
        return listEquals( aList.toArray(), aOther.toArray() );
    }

    /**
     * Are all items in the list unique?
     * 
     * @param aList The list
     * @return true when all items are unique
     */
    public static boolean listContainsUniqueItemsOnly(Object[] aList)
    {
        if ( aList == null )
            return true;
        
        for ( int i=0;  i < aList.length;  i++ )
        {
            for ( int j=0;  j < aList.length;  j++ )
            {
                if ( (i != j) && (aList[i].equals(aList[j]) == true) )
                {
                    System.err.println( "listContainsUniqueItemsOnly [DEBUG] item #" + i + " == item #" + j + " (" + aList[i] + " == " + aList[j] + ")" );
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Are all items in the list unique?
     * 
     * @param aList The list
     * @return true when all items are unique
     */
    public static boolean listContainsUniqueItemsOnly(Collection<?> aList)
    {
        return listContainsUniqueItemsOnly(aList.toArray());
    }
    
    /**
     * Add the stated number of seconds to the date.
     * 
     * @param aDate The date
     * @param aSeconds The number of seconds to add
     * @return the new date
     */
    public static Date addSeconds(Date aDate, int aSeconds)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( aDate );

        cal.add( Calendar.SECOND, aSeconds );
        
        return cal.getTime();
    }
    
    /**
     * Extract the data from the string.
     * 
     * <p>Dates can have the form:
     * <ol>
     * <li>dd/mm/yyyy hh:mm:ss
     * <li>dd/mm/yyyy hh:mm
     * <li>dd/mm/yyyy
     * </ol>
     * </p>
     * 
     * @param aDateStr The date
     * @return the <code>java.util.Date</code>
     * @throws ParseException when the date string is invalid
     */
    public static Date toDate(String aDateStr)
    throws ParseException
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" );
            return sdf.parse( aDateStr );
        }
        catch (ParseException e)
        {
            // do nothing
        }

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy HH:mm" );
            return sdf.parse( aDateStr );
        }
        catch (ParseException e)
        {
            // do nothing
        }

        SimpleDateFormat sdf = new SimpleDateFormat( "dd/MM/yyyy" );
        return sdf.parse( aDateStr );
    }
    
    /**
     * Remove the milli-seconds field.
     * 
     * @param aDate The date
     * @return the new date
     */
    public static Date toSimpleDate(Date aDate)
    {
        if ( aDate == null )
            return null;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime( aDate );
        cal.set( Calendar.MILLISECOND, 0 );        
        return cal.getTime();
    }

    /**
     * Convert the date to date-only.
     * 
     * <p>ie. zero the hours, minutes etc.
     * </p>
     * 
     * @param aDate The date
     * @return the date in date-only form
     */
    public static Date toDateOnly(Date aDate)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( aDate );
        
        cal.set( Calendar.HOUR_OF_DAY, 0 );
        cal.set( Calendar.MINUTE, 0 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        
        return cal.getTime();
    }

    /**
     * Convert the date to time.
     * 
     * <p>ie. zero the year, month etc.
     * </p>
     * 
     * @param aDate The date
     * @return the date in time-only form
     */
    public static Date toTimeOnly(Date aDate)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( aDate );
        
        cal.set( Calendar.YEAR, 0 );
        cal.set( Calendar.MONTH, 0 );
        cal.set( Calendar.DAY_OF_YEAR, 0 );
        
        return cal.getTime();
    }
    
    /**
     * Delete the stated file / directory.
     * 
     * <p>This method is automatic. It will not issue warnings or confirmations
     * </p>
     * 
     * <p>This method is recursive.
     * </p>
     * 
     * @param aPath The file to be deleted
     * @throws IOException when a file cannot be deleted
     */
    public static void rmdir(File aPath)
    throws IOException
    {
        if ( aPath.exists() == false )
            return;
        
        if ( aPath.isDirectory() )
        {
            File[] files = aPath.listFiles();
            for ( int i=0;  i < files.length;  i++ )
                rmdir( files[i] );
        }
        
        if ( aPath.delete() == false )
        {
            throw new IOException( "Cannot delete " + aPath );
        }        
    }

	/**
	 * Find the resource's parent resource.
	 * 
	 * @param aClass The resource loader owner
	 * @param aResourceName The resource to be found
	 * @return the URL for the parent resource
	 * @throws MalformedURLException when a URL cannot be created
	 */
	public static URL getResourceParent(Class<?> aClass, String aResourceName)
	throws MalformedURLException
	{
        URL resourceURL = aClass.getResource( aResourceName );
        TestCase.assertNotNull( "resourceURL", resourceURL );
        
        final String eform = resourceURL.toExternalForm();
        final int pos = eform.lastIndexOf( "/" ); 
        TestCase.assertTrue( "pos", (pos > -1) );
        final String ppath = eform.substring( 0 , pos );
        
        return new URL( ppath );
	}
	
	/**
	 * Get the resource's path.
	 * 
	 * <p>Locate the resource using <code>aClass</code>'s resource loader.
	 * </p>
	 * 
	 * @param aClass The resource loader owner
	 * @param aResourceName The resource to be found
	 * @return the path
	 * @throws URISyntaxException when an error occurs
	 * @throws UnsupportedEncodingException when <code>URLDeocoder</code> fails
	 */
	public static File getResourceFile(Class<?> aClass, String aResourceName)
	throws URISyntaxException, UnsupportedEncodingException
	{
        URL resourceURL = aClass.getResource( aResourceName );
        TestCase.assertNotNull( "resourceURL", resourceURL );
        TestCase.assertNotNull( "resourceURL.file", resourceURL.getFile() );
        String fpath = URLDecoder.decode( resourceURL.getFile(), "UTF-8" );
        return new File(fpath);
	}

	/**
	 * Get the resource's path.
	 * 
	 * <p>Locate the resource using <code>aClass</code>'s resource loader.
	 * </p>
	 * 
	 * @param aClass The resource loader owner
	 * @param aResourceName The resource to be found
	 * @return the path
	 * @throws URISyntaxException when an error occurs
	 * @throws UnsupportedEncodingException when <code>URLDeocoder</code> fails
	 */
	public static String getResourcePath(Class<?> aClass, String aResourceName)
	throws URISyntaxException, UnsupportedEncodingException
	{
		File f = getResourceFile( aClass, aResourceName );
		return f.getPath();
	}

	/**
	 * Get the parent folder containing the stated resource.
	 * 
	 * <p>Locate the resource using <code>aClass</code>'s resource loader.
	 * </p>
	 * 
	 * @param aClass The resource loader owner
	 * @param aResourceName The resource to be found
	 * @return the path
	 * @throws URISyntaxException when an error occurs
	 * @throws UnsupportedEncodingException when <code>URLDeocoder</code> fails
	 */
	public static File getResourceParentFile(Class<?> aClass, String aResourceName)
	throws URISyntaxException, UnsupportedEncodingException
	{
		File f = getResourceFile( aClass, aResourceName );
		return f.getParentFile();
	}

	/**
	 * Reset Log4j.
	 * 
	 * <p>Reset Log4j options using <code>BasicConfigurator</code>. Then,
	 * configure Log4j by using a <code>PropertyConfigurator</code>
	 * to load the stated properties file.
	 * </p>
	 * 
	 * <p>Locate the properties file using <code>aClass</code>'s resource loader.
	 * </p>
	 * 
	 * @param aClass The resource loader owner
	 * @param aPropertiesFile The Log4j properties file
	 * @throws URISyntaxException when an error occurs
	 * @throws UnsupportedEncodingException when <code>URLDeocoder</code> fails
	 */
	public static void initLog4j(Class<?> aClass, String aPropertiesFile)
	throws URISyntaxException, UnsupportedEncodingException
	{
        BasicConfigurator.resetConfiguration();
        PropertyConfigurator.configure(
        	getResourcePath(aClass, aPropertiesFile)
        );		
	}
}
