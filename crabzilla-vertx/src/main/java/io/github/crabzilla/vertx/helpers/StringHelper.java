package io.github.crabzilla.vertx.helpers;

import io.vertx.core.Verticle;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringHelper {

	static final String COMMAND_HANDLER = "cmd-handler";
	static final String EVENTS_HANDLER = "%s-events-handler";

	public static String commandHandlerId(Class<?> aggregateRootClass) {
		return COMMAND_HANDLER + "-" + camelCaseToSpinalCase(aggregateRootClass.getSimpleName());
	}

	public static String circuitBreakerId(Class<?> aggregateRootClass) {
		return COMMAND_HANDLER + "-" + camelCaseToSpinalCase(aggregateRootClass.getSimpleName());
	}
	public static String eventsHandlerId(String bcName) {
		return String.format(EVENTS_HANDLER, bcName);
	}

	public static String aggregateId(Class<?> aggregateRootClass) {
		return camelCaseToSpinalCase(aggregateRootClass.getSimpleName());
	}

	public static SortedMap<String, Verticle> inDeploymentOrder(Map<String, Verticle> map) {
		return map.entrySet().stream()
						.sorted(Map.Entry.comparingByKey())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
										(oldValue, newValue) -> oldValue, TreeMap::new));
	}

	private static String camelCaseToSpinalCase(String start) {
		Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "-" + m.group());
		}
		m.appendTail(sb);
		return sb.toString().toLowerCase();
	}

}
