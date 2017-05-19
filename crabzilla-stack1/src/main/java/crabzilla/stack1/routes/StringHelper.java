package crabzilla.stack1.routes;

import crabzilla.model.AggregateRoot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StringHelper {

	static String aggregateRootId(Class<? extends AggregateRoot> aggregateRootClass) {
		return camelCaseToSnakeCase(aggregateRootClass.getSimpleName());
	}

	static String commandId(Class<?> commandClass) {
		return camelCaseToSnakeCase(commandClass.getSimpleName());
	}

	
	static String aggrCmdRoot(String prefix,
                            Class<? extends AggregateRoot> aggregateRootClass, Class<?> commandClass) {
		return camelCaseToSnakeCase(prefix + aggregateRootClass.getSimpleName() + "-" + commandClass.getSimpleName());
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

  private static String snakeCaseToCamelCase() {
    return snakeCaseToCamelCase();
  }

  public static String snakeCaseToCamelCase(String start) {
		StringBuffer sb = new StringBuffer();
		for (String s : start.split("_")) {
			sb.append(Character.toUpperCase(s.charAt(0)));
			if (s.length() > 1) {
				sb.append(s.substring(1, s.length()).toLowerCase());
			}
		}
		return sb.toString().toLowerCase();
	}

	static String camelizedCron(String cron) {
		return cron.replace(' ', '+');
	}

}
