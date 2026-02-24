package cl.eos.dipalza.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class FechaFormateada extends Date {
  private static final long serialVersionUID = 1L;
  private static final int[] DAYS = { 0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };


	public FechaFormateada() {
		super();
	}

	public FechaFormateada(long fecha) {
		super(fecha);
	}

	public FechaFormateada(Date fecha) {
		super(fecha.getTime());
	}
	
	public int compareTo(Date fecha) {
		int iguales = -1;
		if (fecha instanceof Date) {
			if (this.getTime() == ((Date) fecha).getTime()) {
				iguales = 0;
			} else if (this.getTime() < ((Date) fecha).getTime()) {
				iguales = -1;
			} else {
				iguales = 1;
			}
		}

		return iguales;
	}

	public String toString() {

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(getTime());
		TimeZone tz = TimeZone.getTimeZone("GMT-4:00");
		cal.setTimeZone(tz);
		String fecha = cal.get(Calendar.DAY_OF_MONTH) + "/"
				+ (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
		
		return fecha;
	}
	
	public static String getFechaFormatrada(Date fecha) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(fecha.getTime());
		TimeZone tz = TimeZone.getTimeZone("GMT-4:00");
		cal.setTimeZone(tz);
		String ff = cal.get(Calendar.DAY_OF_MONTH) + "/"
				+ (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
		
		return ff;
		
	}

	public static FechaFormateada nextDate(Date fecha) {
		FechaFormateada ff = null;
		int day, month, year;
		
		Calendar c =  Calendar.getInstance();
		c.setTime(fecha);
		day =  c.get(Calendar.DAY_OF_MONTH);
		month =  c.get(Calendar.MONTH) + 1;
		year =  c.get(Calendar.YEAR);
        if (isValid(month, day + 1, year)) {
        	day++;
        	if(month == 2 && ((day > 29) ||  (day > 28 && !isLeapYear(year)))){
        		day = 1;
        		month++;
        	}
        }
        else if (isValid(month + 1, 1, year)) {
        	month++;
        	day = 1;
        }
        else {                     
        	day = 1;
        	month = 1;
        	year++;
        }
    	c.set(Calendar.DAY_OF_MONTH, day);
    	c.set(Calendar.MONTH, month - 1);
    	c.set(Calendar.YEAR, year);
    	Date d = c.getTime();
    	ff = new FechaFormateada(d);
    	return ff;
	}
	
	public static FechaFormateada nextWeekDate(Date fecha) {
		FechaFormateada ff = nextDate(fecha);
		Calendar c = Calendar.getInstance();
		c.setTime(ff);
		if(c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.setTime(nextDate(c.getTime()));
		}
    	ff = new FechaFormateada(c.getTime());
    	return ff;
		
	}

	// is the given date valid?
	private static boolean isValid(int m, int d, int y) {
	    if (m < 1 || m > 12)      return false;
	    if (d < 1 || d > DAYS[m]) return false;
	    if (m == 2 && d == 29 && !isLeapYear(y)) return false;
	    return true;
	}
	
	// is y a leap year?
	private static boolean isLeapYear(int y) {
	    if (y % 400 == 0) return true;
	    if (y % 100 == 0) return false;
	    return (y % 4 == 0);
	}

}
