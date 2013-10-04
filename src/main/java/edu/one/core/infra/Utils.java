package edu.one.core.infra;

import java.io.InputStream;
import java.util.Scanner;

public class Utils {

	public static <T> T getOrElse(T value, T defaultValue) {
		return getOrElse(value, defaultValue, true);
	}

	public static <T> T getOrElse(T value, T defaultValue, boolean allowEmpty) {
		if (value != null && (allowEmpty || !value.toString().trim().isEmpty())) {
			return value;
		}
		return defaultValue;
	}

	public static String inputStreamToString(InputStream in) {
		Scanner scanner = new Scanner(in, "UTF-8");
		String content = scanner.useDelimiter("\\A").next();
		scanner.close();
		return content;
	}

}
