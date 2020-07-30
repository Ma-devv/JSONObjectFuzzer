package parser;

import java.util.Comparator;

public class ParseTreeTupleComparator implements Comparator<ParseTreeTuple> {
	/* 
	 * Order: Biggest element @Head of the queue 
	 * Return -1 if the first object is "smaller" than the second object
	 * 
	 * */
	@Override
	public int compare(ParseTreeTuple o1, ParseTreeTuple o2) {
		if(o1.getTree_size() < o2.getTree_size()) {
			return 1;
		}
		else if(o1.getTree_size() > o2.getTree_size()) {
			return -1;
		}
		else {
			return 0;
		}
	}

}
