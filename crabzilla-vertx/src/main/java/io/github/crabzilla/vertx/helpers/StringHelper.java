package io.github.crabzilla.vertx.helpers;

import io.vertx.core.Verticle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringHelper {

	static final String COMMAND_HANDLER = "cmd-handler";
	static final String EVENTS_HANDLER = "%s-events-handler";

	public static String commandHandlerId(Class<?> aggregateRootClass) {
		return COMMAND_HANDLER + "-" + camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}

	public static String circuitBreakerId(Class<?> aggregateRootClass) {
		return COMMAND_HANDLER + "-" + camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}
	public static String eventsHandlerId(String bcName) {
		return String.format(EVENTS_HANDLER, bcName);
	}

	public static String aggregateRootId(Class<?> aggregateRootClass) {
		return camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}

	public static Map<String, Verticle> inDeploymentOrder(Map<String, Verticle> map) {
		return map.entrySet().stream()
						.sorted(Map.Entry.comparingByKey())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
										(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	private static String camelCaseToSnakeCase(String start) {
		Matcher m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "_" + m.group());
		}
		m.appendTail(sb);
		return sb.toString().toLowerCase();
	}

}
