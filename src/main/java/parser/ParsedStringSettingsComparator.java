package parser;

import java.util.Comparator;

public class ParsedStringSettingsComparator implements Comparator<ParsedStringSettings> {

	@Override
	public int compare(ParsedStringSettings o1, ParsedStringSettings o2) {
		/* 
		 * Order: Element with the longest non <anychar> sequence @Head of the queue 
		 * Return -1 if the first object is "smaller" than the second object
		 * 
		 * */
		if(o1.getString_after_removing_anychars().length() < o2.getString_after_removing_anychars().length()) {
			return 1;
		}
		else if(o1.getString_after_removing_anychars().length() > o2.getString_after_removing_anychars().length()) {
			return -1;
		}
		else {
			return 0;
		}
	}

}
