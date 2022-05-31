package com.playmonumenta.plugins.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class DateUtils {
	// TODO Read from config, default to system time zone aka ZoneId.systemDefault() - Tim

	// Offset server time to UTC-17 to change when the new day arrives.
	// getDaysSinceEpoch() uses its own perceived epoch
	// so it should sync up nicely with changes in getDayOfWeek().
	public static final int HOURS_OFFSET = -17;
	public static final ZoneId TIMEZONE = ZoneOffset.ofHours(HOURS_OFFSET);

	public static LocalDateTime utcDateTime() {
		return LocalDateTime.now(ZoneId.of("UTC"));
	}

	public static LocalDateTime localDateTime() {
		return LocalDateTime.now(TIMEZONE);
	}

	public static LocalDateTime localDateTime(long dailyVersion) {
		LocalDateTime epoch = localDateTime(1970, 1, 1);
		long dayOffset = dailyVersion - getDaysSinceEpoch(epoch);
		return epoch.plusDays(dayOffset);
	}

	public static LocalDateTime localDateTime(int year, int month, int dayOfMonth) {
		return LocalDateTime.of(year, month, dayOfMonth, 0, 0, 0);
	}

	public static LocalDateTime startOfNextDay() {
		return localDateTime(getDaysSinceEpoch() + 1);
	}

	public static LocalDateTime startOfNextWeek() {
		return localDateTime(getNextWeeklyVersionStartDate());
	}

	public static int getYear() {
		return getYear(localDateTime());
	}

	public static int getYear(LocalDateTime localDateTime) {
		return localDateTime.getYear();
	}

	public static int getMonth() {
		return getMonth(localDateTime());
	}

	public static int getMonth(LocalDateTime localDateTime) {
		return localDateTime.getMonthValue();
	}

	public static int getDayOfMonth() {
		return getDayOfMonth(localDateTime());
	}

	public static int getDayOfMonth(LocalDateTime localDateTime) {
		return localDateTime.getDayOfMonth();
	}

	// 1 is Sunday, 7 is Saturday
	public static int getDayOfWeek() {
		return getDayOfWeek(localDateTime());
	}

	public static int getDayOfWeek(LocalDateTime localDateTime) {
		// .getValue() gives 1 for Monday, 7 for Sunday, so we cycle the numbers
		return Math.floorMod(localDateTime.getDayOfWeek().getValue(), 7) + 1;
	}

	// Also known as DailyVersion
	public static long getDaysSinceEpoch() {
		// In our specified timezone, how many days we perceive it is since our 1 Jan 1970.
		// Different timezones have different dates for the same point in time,
		// so this simple comparison will yield different numbers of days for them
		return getDaysSinceEpoch(localDateTime());
	}

	public static long getDaysSinceEpoch(LocalDateTime localDateTime) {
		// In our specified timezone, how many days we perceive it is since our 1 Jan 1970.
		// Different timezones have different dates for the same point in time,
		// so this simple comparison will yield different numbers of days for them
		return localDateTime.toLocalDate().toEpochDay();
	}

	// Note: This method is intentionally UTC-only.
	public static long getSecondsSinceEpoch() {
		return LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
	}

	public static boolean getAmPm() {
		return getAmPm(localDateTime());
	}

	public static boolean getAmPm(LocalDateTime localDateTime) {
		return getHourOfDay(localDateTime) >= 12;
	}

	public static int getHourOfDay() {
		return getHourOfDay(localDateTime());
	}

	public static int getHourOfDay(LocalDateTime localDateTime) {
		return localDateTime.getHour();
	}

	public static int getHourOfTwelve() {
		return getHourOfTwelve(localDateTime());
	}

	public static int getHourOfTwelve(LocalDateTime localDateTime) {
		int hourOfTwelve = getHourOfDay(localDateTime) % 12;
		return (hourOfTwelve == 0) ? 12 : hourOfTwelve;
	}

	public static int getMinute() {
		return getMinute(localDateTime());
	}

	public static int getMinute(LocalDateTime localDateTime) {
		return localDateTime.getMinute();
	}

	public static int getSecond() {
		return getSecond(localDateTime());
	}

	public static int getSecond(LocalDateTime localDateTime) {
		return localDateTime.getSecond();
	}

	public static int getMs() {
		return getMs(localDateTime());
	}

	@SuppressWarnings("JavaLocalDateTimeGetNano")
	public static int getMs(LocalDateTime localDateTime) {
		return localDateTime.getNano() / 1000000;
	}

	public static long getWeeklyVersion() {
		return getWeeklyVersion(localDateTime());
	}

	public static long getWeeklyVersion(LocalDateTime localDateTime) {
		return getWeeklyVersion(getDaysSinceEpoch(localDateTime));
	}

	public static long getWeeklyVersion(long dailyVersion) {
		return Math.floorDiv(dailyVersion + 6, 7);
	}

	public static long getDaysIntoWeeklyVersion() {
		return getDaysIntoWeeklyVersion(localDateTime());
	}

	public static long getDaysIntoWeeklyVersion(LocalDateTime localDateTime) {
		return Math.floorMod(localDateTime.toLocalDate().toEpochDay() - 1, 7) + 1L;
	}

	public static long getDaysLeftInWeeklyVersion() {
		return getDaysLeftInWeeklyVersion(localDateTime());
	}

	public static long getDaysLeftInWeeklyVersion(LocalDateTime localDateTime) {
		return 8 - getDaysIntoWeeklyVersion(localDateTime);
	}

	public static long getWeeklyVersionStartDate() {
		return getWeeklyVersionStartDate(localDateTime());
	}

	public static long getWeeklyVersionStartDate(LocalDateTime localDateTime) {
		return getDaysSinceEpoch(localDateTime) - getDaysIntoWeeklyVersion(localDateTime) + 1;
	}

	public static long getWeeklyVersionEndDate() {
		return getWeeklyVersionEndDate(localDateTime());
	}

	public static long getWeeklyVersionEndDate(LocalDateTime localDateTime) {
		return getWeeklyVersionStartDate(localDateTime) + 6;
	}

	public static long getNextWeeklyVersionStartDate() {
		return getNextWeeklyVersionStartDate(localDateTime());
	}

	public static long getNextWeeklyVersionStartDate(LocalDateTime localDateTime) {
		return getWeeklyVersionStartDate(localDateTime) + 7;
	}

	public static String untilNewDay() {
		long seconds = untilNewDay(ChronoUnit.SECONDS);
		long minutes = seconds / 60L;
		seconds %= 60L;
		long hours = minutes / 60L;
		minutes %= 60L;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	public static long untilNewDay(ChronoUnit unit) {
		return localDateTime().until(startOfNextDay(), unit);
	}

	public static String untilNewWeek() {
		long seconds = untilNewDay(ChronoUnit.SECONDS);
		long minutes = seconds / 60L;
		seconds %= 60L;
		long hours = minutes / 60L;
		minutes %= 60L;
		if (hours < 24) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		}
		long days = hours / 24L;
		hours %= 24;
		return String.format("%d days %02d hours", days, hours);
	}

	public static long untilNewWeek(ChronoUnit unit) {
		return localDateTime().until(startOfNextWeek(), unit);
	}
}
