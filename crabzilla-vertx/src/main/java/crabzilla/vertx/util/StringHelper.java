package crabzilla.vertx.util;

import crabzilla.model.AggregateRoot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

	static final String COMMAND_HANDLER = "cmd-handler";
	static final String EVENTS_HANDLER = "%s-events-handler";

	public static String commandHandlerId(Class<? extends AggregateRoot> aggregateRootClass) {
		return COMMAND_HANDLER + camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}

	public static String circuitBreakerId(Class<? extends AggregateRoot> aggregateRootClass) {
		return COMMAND_HANDLER + camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}
	public static String eventsHandlerId(String bcName) {
		return String.format(EVENTS_HANDLER, bcName);
	}

	public static String aggregateRootId(Class<? extends AggregateRoot> aggregateRootClass) {
		return camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
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
