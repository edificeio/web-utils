package edu.one.core.infra.collections;

import java.util.Arrays;

public class Joiner {

	private final String separator;

	private Joiner(String separator) {
		this.separator = separator;
	}

	public static Joiner on(String separator) {
		if (separator != null) {
			return new Joiner(separator);
		}
		return null;
	}

	public String join(Iterable<?> items) {
		StringBuilder sb = new StringBuilder();
		for (Object item: items) {
			sb.append(separator).append(item.toString());
		}
		return (sb.length() > separator.length()) ? sb.substring(separator.length()) : "";
	}

	public String join(Object[] items) {
		return join(Arrays.asList(items));
	}

}
