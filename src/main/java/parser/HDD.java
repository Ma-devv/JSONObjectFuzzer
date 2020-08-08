package parser;

import java.util.ArrayList;
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
		// ParseTreeTupleComparator pttComp = new ParseTreeTupleComparator();
		PriorityQueue<ParseTreeTuple> pq = new PriorityQueue<ParseTreeTuple>(new ParseTreeTupleComparator());
		pq.add(new ParseTreeTuple(pt.count_leafes(), pt)); // Add the "root" tree to the key to get started
		while(!pq.isEmpty()) { // While there are trees in the queue
			ParseTree reprocess = null;
			ParseTree biggest_node = pq.poll().getPt(); // Get the "biggest" node
			if(this.log) {
				System.out.println("\nBiggest node:\n" + biggest_node.tree_to_string());
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
				// Check if the subtree has at least one terminal character outside of <anychars> block and is not empty or equals a space character
				if(checkIfAtLeastOneNonAnychar(stree, excludeSet) && (!(stree.getPt().getTerminals().equals("") || stree.getPt().getTerminals().equals(" ")))) {
					System.out.format("Replace the biggest node with:\n%s\n", stree.getPt().tree_to_string());
					replaceBiggestNode(biggest_node, stree);
					// Results in us having a new set of characters that we can try to parse using the adjusted grammar
					ParsedStringSettings pss = checkIfPossibleToParse(master_pss, stree.getPt().getTerminals(), excludeSet, bsymbol);
					if(pss != null) { // Now we have to check if the modified non terminals can be parsed again using the adjusted grammar
						// If it is possible to parse the adjusted string with the adjusted grammar, then we proceed
						// System.out.println("Pss for subtree " + bsymbol + ":\n" + pss.toString());
						if(this.log) {
							System.out.println("Set new \"Minimized string using HDD\": " + pss.getCreated_string()); // Created string = hdd string; we set the hdd string
							// at the end in startHDD before returning
						}
						reprocess = stree.getPt();
						result = pss;
						break;
					}
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
	
	private boolean checkIfAtLeastOneNonAnychar(ParseTreeTuple stree, HashSet<String> excludeSet) {
		for(String s : stree.getPt().getParentOfTerminals().split(", ")) {
			if(!excludeSet.contains(s)) {
				return true;
			}
		}
		if(this.log) {
			System.out.println("Terminals within the tree are all represented using <anychar>.");
		}
		return false;
	}
	private ParsedStringSettings checkIfPossibleToParse(ParsedStringSettings master_pss, String adjusted_string, HashSet<String> excludeSet, String bsymbol) {
		try {
			// The adjusted string should be rejected by the golden grammar ...
			this.getGolden_grammar_PL().check_string(adjusted_string, this.getGolden_grammar_EP()); // Try to parse the string; return value can be obtained
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
				ep_adjusted = new EarleyParser(master_pss.getPl().grammar);
	    		ParseTree result = master_pss.getPl().check_string(adjusted_string, ep_adjusted); // Try to parse the string
	    		if(result != null) {
	    			master_pss.setHdd_string(adjusted_string);
	    			ParsedStringSettings pss = new ParsedStringSettings(
		            		adjusted_string, // Created string
		            		"",
		            		"",
		            		master_pss.getTree().count_nodes(0, excludeSet),
		            		master_pss.getTree().count_leafes(),
		            		master_pss.getChanged_rule(), 
		            		master_pss.getChanged_token(), 
		            		master_pss.getChanged_elem(), 
		            		master_pss.getTree(),
		            		master_pss.getPl());
					if(this.log) {
						System.out.println("String: " + adjusted_string + " successfully parsed using the adjusted golden grammar");
					}
					return pss;
	    		}
	    		return null;
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
			System.out.printf("Applying HDD\nID: %s\nString to minimize: %s\nTree to minimize:\n%s\n", pss.hashCode(), pss.getCreated_string(), pss.getTree().tree_to_string());
		}
		this.setGolden_grammar_PL(golden_grammar_PL);
		this.setGolden_grammar_EP(golden_grammar_EP);
		ParsedStringSettings result = perses_delta_debug(pss.getTree(), pss.getPl().grammar, pss, excludeSet);
		if(result != null) {
			pss.setHdd_string(result.getCreated_string());
			if(this.log) {
				System.out.println("Updated HDD string: " + result.getCreated_string());
			}
		}
		else {
			if(this.log) {
				System.out.println("Unable to minimized the string " + pss.getRemoved_anychar_string() + " any further");
			}
		}
		return pss;
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
