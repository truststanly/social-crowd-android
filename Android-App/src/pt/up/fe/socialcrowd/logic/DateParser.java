package pt.up.fe.socialcrowd.logic;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class DateParser {
	public static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static Date parseString(String format, String date) throws ParseException {
		SimpleDateFormat dateformat = new SimpleDateFormat(format);
		return dateformat.parse(date);		
	}
	public static Date parseString(String date) throws ParseException {
		SimpleDateFormat dateformat = new SimpleDateFormat(DEFAULT_FORMAT);
		return dateformat.parse(date);		
	}
	public static String parseDate(Date date) {
		SimpleDateFormat dateformat = new SimpleDateFormat(DEFAULT_FORMAT);
		return dateformat.format(date);
	}
	public static String parseDate(String format, Date date) {
		SimpleDateFormat dateformat = new SimpleDateFormat(format);
		return dateformat.format(date);
	}
}