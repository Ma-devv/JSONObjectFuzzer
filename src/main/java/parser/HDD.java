package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.PriorityQueue;

public class HDD {
	private boolean log = true;
	
	/*
	 * Searches for subtrees whose name matches symbol
	 * */
	private ParserLib golden_grammar_PL;
	private EarleyParser golden_grammar_EP;
	public ArrayList<ParseTreeTuple> subtrees_with_symbol(ParseTree tree, String symbol, ArrayList<ParseTreeTuple> result, int depth) {
		if(result == null) {
			result = new ArrayList<ParseTreeTuple>();
		}
		if(tree.name.equals(symbol)) {
			ParseTreeTuple ptt = new ParseTreeTuple(depth, tree);
			result.add(ptt);
		}
		for(ParseTree c : tree.children) {
			subtrees_with_symbol(c, symbol, result, depth+1);
		}
		return result;
	}
	public ParsedStringSettings perses_delta_debug(ParseTree tree, Grammar grammar, ParsedStringSettings master_pss, HashSet<String> excludeSet) {
		ParsedStringSettings result = null;
		ParseTree pt = new ParseTree(tree.name, tree.children); // create new ParseTree object (deep copy)
		ParseTreeTupleComparator pttComp = new ParseTreeTupleComparator();
		PriorityQueue<ParseTreeTuple> pq = new PriorityQueue<ParseTreeTuple>(new ParseTreeTupleComparator());
		pq.add(new ParseTreeTuple(pt.count_leafes(), pt)); // Add the "root" tree to the key to get started
		while(!pq.isEmpty()) { // While there are trees in the queue
			ParseTree reprocess = null;
			ParseTree biggest_node = pq.poll().getPt(); // Get the "biggest" node
			if(this.log) {
				System.out.println("\nBiggest node:\n" + biggest_node.save_tree());
			}
			String bsymbol = biggest_node.name; // Get the "biggest" symbol
			// We need the biggest symbol to find subtrees with the same name/token
			ArrayList<ParseTreeTuple> ssubtrees = subtrees_with_symbol(biggest_node, bsymbol, null, 0);
			// Now discard the first entry of ssubtrees as this should be the tree itself
			ssubtrees.remove(0);
			// Sort the found subtrees according to their DEPTH (not leafe size, why?) TODO
			ssubtrees.sort(new ParseTreeTupleComparator());
			if(ssubtrees.size() == 0) {
				if(this.log) {
					System.out.println("Did not find any subtrees matching " + bsymbol + ". Continue...");
				}
			}
			else {
				if(this.log) {
					System.out.println("Found " + ssubtrees.size() + " subtrees (without the root tree itself) for " + bsymbol);
				}
			}
			for(int i = ssubtrees.size()-1; i >= 0; i--) { // Loop in reversed order
				ParseTreeTuple stree = ssubtrees.get(i);
				System.out.format("Replace the biggest node with:\n%s\n", stree.getPt().save_tree());
				replaceBiggestNode(biggest_node, stree);
				// Results in us having a new set of characters that we can try to parse using the adjusted grammar
				ParsedStringSettings pss = checkIfPossibleToParse(master_pss, stree.getPt().tree_to_string(), excludeSet, bsymbol);
				if(pss != null) { // Now we have to check if the modified non terminals can be parsed again using the adjusted grammar
					// If it is possible to parse the adjusted string with the adjusted grammar, then we proceed
					// System.out.println("Pss for subtree " + bsymbol + ":\n" + pss.toString());
					reprocess = stree.getPt();
					result = pss;
					break;
				}
			}
			if(reprocess != null) { // Did we found a subtree/sub-input that has been parsed successfully?
				biggest_node = reprocess;
				ParseTreeTuple t_ptt = new ParseTreeTuple(biggest_node.count_leafes(), biggest_node);
				pq.add(t_ptt);
			}
			else {
				for(ParseTree stree : biggest_node.children) {
					if(stree.is_nt()) {
						System.out.println("Added Subtree " +stree.name + " to the queue");
						ParseTreeTuple t_ptt = new ParseTreeTuple(stree.count_leafes(), stree);
						pq.add(t_ptt);
					}
				}
			}
		}
		return result;
		
	}
	
	private ParsedStringSettings checkIfPossibleToParse(ParsedStringSettings master_pss, String adjusted_string, HashSet<String> excludeSet, String bsymbol) {
		try {
			// The adjusted string should be rejected by the golden grammar ...
			this.getGolden_grammar_PL().parse_string(adjusted_string, this.getGolden_grammar_EP()); // Try to parse the string; return value can be obtained
			// If the golden grammar parses the string, we will discard the string => return null
			if(this.log) {
				System.out.println("String: " + adjusted_string + " successfully parsed using the golden grammar...");
			}
			return null;
		} catch (Exception e) {
			if(this.log) {
				System.out.println("Golden grammar failed to parse " + adjusted_string + " successfully");
			}
			// ... and should be parsed using the adjusted golden grammar
			try {
				EarleyParser ep_adjusted = null;
	    		Date d_start = new Date();
				Date d_end = new Date();
	    		// Replace the rule with <anychars>
				d_start = new Date();
//				if(this.log) {
//					System.out.println("HDD: Create EarleyParser with ParserLib grammar");
//				}
				ep_adjusted = new EarleyParser(master_pss.getPl().grammar);
				d_end = new Date();
				long difference = d_end.getTime() - d_start.getTime();
//				if(this.log) {
//					System.out.println("HDD: Successfully created EarleyParser with ParserLib grammar in " + difference / 1000 + " seconds");
//				}
				
	    		ParseTree result = master_pss.getPl().parse_string(adjusted_string, ep_adjusted); // Try to parse the string
//	    		if(this.log) {
//	    			System.out.println("HDD: String " + adjusted_string + " successfully parsed using the adjusted golden grammar");
//				}
	    		String s = !adjusted_string.equals("") ? String.format("Original string: %s => minimized string: %s", master_pss.getCreated_string(), adjusted_string)
	    				: String.format("Original string: %s => minimized string is an EMPTY STRING: %s", master_pss.getCreated_string(), adjusted_string);
	            ParsedStringSettings pss = new ParsedStringSettings(
	            		s, // Created string
	            		result.count_nodes(0, excludeSet),
	            		result.count_leafes(),
	            		master_pss.getChanged_rule(), 
	            		master_pss.getChanged_token(), 
	            		master_pss.getChanged_elem() + "\nOld tree before applying HDD:\n" + master_pss.getTree().save_tree(), 
	            		result,
	            		master_pss.getPl());
				if(this.log) {
					System.out.println("String: " + adjusted_string + " successfully parsed using the adjusted golden grammar");
				}
	            return pss;
			} catch (Exception e2) {
				if(this.log) {
					System.out.println("Failed to parse the string " + adjusted_string + " using the adjusted golden grammar");
				}
				return null;
			}
		}
	}
	/*
	 * Replace the given node "biggest_node" with the subtree "stree" 
	 * 
	 * */
	private void replaceBiggestNode(ParseTree biggest_node, ParseTreeTuple stree) {
		biggest_node = stree.getPt();
	}
	
	public ParsedStringSettings startHDD(ParsedStringSettings pss, HashSet<String> excludeSet, ParserLib golden_grammar_PL, EarleyParser golden_grammar_EP){
		if(this.log) {
			System.out.println("Applying HDD");
		}
		this.setGolden_grammar_PL(golden_grammar_PL);
		this.setGolden_grammar_EP(golden_grammar_EP);
		if(!pss.getCreated_string().equals("{\"abcd\": [{\"pqrs\":}]}")) {
			System.out.println("");
		}
		return perses_delta_debug(pss.getTree(), pss.getPl().grammar, pss, excludeSet);
	}
	public ParserLib getGolden_grammar_PL() {
		return golden_grammar_PL;
	}
	public void setGolden_grammar_PL(ParserLib golden_grammar_PL) {
		this.golden_grammar_PL = golden_grammar_PL;
	}
	public EarleyParser getGolden_grammar_EP() {
		return golden_grammar_EP;
	}
	public void setGolden_grammar_EP(EarleyParser golden_grammar_EP) {
		this.golden_grammar_EP = golden_grammar_EP;
	}
	
}
