package parser;

import java.util.PriorityQueue;

public class HDD {
	
	public void perses_delta_debug(ParseTree tree) {
		ParseTree pt = new ParseTree(tree.name, tree.children); // create new ParseTree object (deep copy)
		PriorityQueue<ParseTreeTuple> pq = new PriorityQueue<ParseTreeTuple>();
		pq.add(new ParseTreeTuple(pt.count_leafes(), pt));
		
	}
	
}
