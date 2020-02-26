package me.neznamy.tab.shared.placeholders;

import java.util.*;

public class Placeholders {

	public static final char colorChar = '\u00a7';
	
	//my registered placeholders
	public static Map<String, PlayerPlaceholder> myPlayerPlaceholders;
	public static Map<String, ServerPlaceholder> myServerPlaceholders;
	public static Map<String, ServerConstant> myServerConstants;
	
	//my used placeholders + used papi placeholders
	public static Map<String, PlayerPlaceholder> usedPlayerPlaceholders;
	public static Map<String, ServerPlaceholder> usedServerPlaceholders;
	public static Map<String, ServerConstant> usedServerConstants;
	public static List<String> usedPlaceholders;

	public static void clearAll() {
		myPlayerPlaceholders = new HashMap<String, PlayerPlaceholder>();
		myServerPlaceholders = new HashMap<String, ServerPlaceholder>();
		myServerConstants = new HashMap<String, ServerConstant>();
		
		usedPlayerPlaceholders = new HashMap<String, PlayerPlaceholder>();
		usedServerPlaceholders = new HashMap<String, ServerPlaceholder>();
		usedServerConstants = new HashMap<String, ServerConstant>();
		usedPlaceholders = new ArrayList<String>();
	}
	public static List<Placeholder> getAllUsed(){
		List<Placeholder> usedPlaceholders = new ArrayList<Placeholder>();
		usedPlaceholders.addAll(usedPlayerPlaceholders.values());
		usedPlaceholders.addAll(usedServerPlaceholders.values());
		return usedPlaceholders;
	}
	public static List<String> detectAll(String s){
		List<String> list = new ArrayList<String>();
		if (s == null) return list;
		while (s.contains("%")) {
			s = s.substring(s.indexOf("%")+1, s.length());
			if (s.contains("%")) {
				String placeholder = s.substring(0, s.indexOf("%"));
				s = s.substring(s.indexOf("%")+1, s.length());
				list.add("%" + placeholder + "%");
			}
		}
		return list;
	}

	//code taken from bukkit, so it can work on bungee too
	public static String color(String textToTranslate){
		if (textToTranslate == null) return null;
		if (!textToTranslate.contains("&")) return textToTranslate;
		char[] b = textToTranslate.toCharArray();
		for (int i = 0; i < b.length - 1; i++) {
			if ((b[i] == '&') && ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[(i + 1)]) > -1)){
				b[i] = colorChar;
				b[(i + 1)] = Character.toLowerCase(b[(i + 1)]);
			}
		}
		return new String(b);
	}
	//code taken from bukkit, so it can work on bungee too
	public static String getLastColors(String input) {
		String result = "";
		int length = input.length();
		for (int index = length - 1; index > -1; index--){
			char section = input.charAt(index);
			if ((section == colorChar) && (index < length - 1)){
				char c = input.charAt(index + 1);
				if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".contains(c+"")) {
					result = colorChar + "" + c + result;
					if ("0123456789AaBbCcDdEeFfRr".contains(c+"")) {
						break;
					}
				}
			}
		}
		return result;
	}
	public static List<Placeholder> detectPlaceholders(String rawValue, boolean playerPlaceholders) {
		if (rawValue == null || (!rawValue.contains("%") && !rawValue.contains("{"))) return new ArrayList<Placeholder>();
		List<Placeholder> placeholdersTotal = new ArrayList<Placeholder>();
		for (Placeholder placeholder : playerPlaceholders ? getAllUsed() : usedServerPlaceholders.values()) {
			if (rawValue.contains(placeholder.getIdentifier())) {
				placeholdersTotal.add(placeholder);
				for (String child : placeholder.getChilds()) {
					for (Placeholder p : detectPlaceholders(child, playerPlaceholders)) {
						if (!placeholdersTotal.contains(p)) placeholdersTotal.add(p);
					}
				}
			}
		}
		return placeholdersTotal;
	}
}