package io.github.crabzilla.vertx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointsHelper {

	static final String COMMAND_HANDLER = "-cmd-handler";
	static final String EVENTS_HANDLER = "-events-handler";

  public static String restEndpoint(String name) {
    return camelCaseToSpinalCase(name);
  }

  public static String cmdHandlerEndpoint(String name) {
		return camelCaseToSpinalCase(name).concat(COMMAND_HANDLER);
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
