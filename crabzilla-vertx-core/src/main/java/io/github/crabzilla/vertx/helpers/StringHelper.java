package io.github.crabzilla.vertx.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

	static final String COMMAND_HANDLER = "-cmd-handler";
	static final String EVENTS_HANDLER = "-events-handler";

  public static String restEndpoint(Class<?> aggregateRootClass) {
    return camelCaseToSpinalCase(aggregateRootClass.getSimpleName());
  }

  public static String cmdHandlerEndpoint(Class<?> aggregateRootClass) {
		return camelCaseToSpinalCase(aggregateRootClass.getSimpleName()).concat(COMMAND_HANDLER);
	}

	public static String projectorEndpoint(String bcName) {
  	return camelCaseToSpinalCase(bcName).concat(EVENTS_HANDLER);
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
