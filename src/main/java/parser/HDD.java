package parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.json.JSONArray;
import org.json.JSONObject;

public class HDD {	
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
			/*
			if(tree.children.size() > 0) { // As we are not interested in single non terminals without any childs
				ParseTreeTuple ptt = new ParseTreeTuple(depth, tree);
				result.add(ptt);
			}
			else {
				if(this.log) {
					System.out.println("Skipped empty ParseTree");
				}
			}
			*/
		}
		for(ParseTree c : tree.children) {
			subtrees_with_symbol(c, symbol, result, depth+1);
		}
		return result;
	}
	public String perses_delta_debug(ParseTree tree, Grammar grammar, ParsedStringSettings pss, ParsedStringSettings original_pss, HashSet<String> excludeSet, boolean log) {
		String result = tree.getTerminals();
		ParseTree pt = new ParseTree(tree.name, tree.children); // create new ParseTree object (deep copy)
		// ParseTreeTupleComparator pttComp = new ParseTreeTupleComparator();
		PriorityQueue<ParseTreeTuple> pq = new PriorityQueue<ParseTreeTuple>(new ParseTreeTupleComparator());
		pq.add(new ParseTreeTuple(pt.count_leafes(), pt)); // Add the "root" tree to the key to get started
		while(!pq.isEmpty()) { // While there are trees in the queue
			try {
				ParseTree reprocess = null;
				ParseTree biggest_node = pq.poll().getPt(); // Get the "biggest" node
//				if(log) {
//					System.out.println("\nBiggest node:\n" + biggest_node.tree_to_string());
//				}
				String bsymbol = biggest_node.name; // Get the "biggest" symbol
				// We need the biggest symbol to find subtrees with the same name/token
				ArrayList<ParseTreeTuple> ssubtrees = subtrees_with_symbol(biggest_node, bsymbol, null, 0);
				// Now discard the first entry of ssubtrees as this should be the tree itself
				if(ssubtrees.size() > 0) {
					ssubtrees.remove(0);
				}
				// Sort the found subtrees according to their DEPTH (not leafe size, why?) TODO
				ssubtrees.sort(new ParseTreeTupleComparator());
				if(ssubtrees.size() == 0) {
//					if(log) {
//						System.out.println("Did not find any subtrees matching " + bsymbol + ". Continue...");
//					}
				}
				else {
//					if(log) {
//						System.out.println("Found " + ssubtrees.size() + " subtrees (without the root tree itself) for " + bsymbol);
//					}
				}
				for(int i = ssubtrees.size()-1; i >= 0; i--) { // Loop in reversed order
					ParseTreeTuple stree = ssubtrees.get(i);
					// Create a copy of the pss so that we are also able to undo the changes
					ParsedStringSettings saved_pss = new ParsedStringSettings(pss);
					ParseTree saved_biggest_node = new ParseTree(biggest_node);
//					System.out.println("Saved biggest pss:\n" + saved_pss.getTree().tree_to_string());
//					System.out.println("Tree that should replace the biggest node (" + biggest_node.hashCode() + "):\n" + stree.getPt().tree_to_string());
					pss.getTree().replaceTreeNode(biggest_node, stree.getPt(), log);
//					System.out.println("Saved biggest pss:\n" + saved_pss.getTree().tree_to_string());
//					System.out.println("New Tree representing the string " + pss.getTree().getTerminals() + ":\n" + pss.getTree().tree_to_string());
					// Check if the subtree has at least one terminal character outside of <anychars> block and is not empty or equals a space character
					if(checkIfAtLeastOneNonAnychar(pss.getTree(), excludeSet, log) && (!(pss.getTree().getTerminals().equals("") || pss.getTree().getTerminals().equals(" ")))) {
						if(checkJSON(pss, log)) {
							// Results in us having a new set of characters that we can try to parse using the adjusted grammar
							// Now we have to check if the modified terminals can be parsed again using the adjusted grammar
							if(checkIfGGFails(pss.getTree().getTerminals(), log)) {
//								System.out.format("Replace the biggest node with:\n%s\n", stree.getPt().tree_to_string());
//								if(log) {
//									System.out.format("Replace the biggest node with:\n%s\n", stree.getPt().tree_to_string());
//								}
								replaceBiggestNode(biggest_node, stree, log);
								if(log) {
									System.out.println("Set new \"Minimized string using HDD\": " + pss.getTree().getTerminals()); // Created string = hdd string; we set the hdd string
								}
								reprocess = stree.getPt();
								result = pss.getTree().getTerminals();
								break;
							}
						}
					}
					// Something went wrong; undo the changes
					// pss = saved_pss;
//					if(log) {
//						System.out.println("Undo changes; restore biggest node and tree");
//					}
					pss.setTree(new ParseTree(saved_pss.getTree()));
					biggest_node = saved_biggest_node;
//					System.out.println("Undone changes. Biggest node:\n" + biggest_node.tree_to_string());
//					System.out.println("Master tree:\n" + pss.getTree().tree_to_string());
				}
				if(reprocess != null) { // Did we found a subtree/sub-input that has been parsed successfully?
					biggest_node = reprocess;
					ParseTreeTuple t_ptt = new ParseTreeTuple(biggest_node.count_leafes(), biggest_node);
					pq.add(t_ptt);
				}
				else {
					for(ParseTree stree : biggest_node.children) {
						if(stree.is_nt() && !excludeSet.contains(stree.name)) { // Not interested in terminals and in <anychar> blocks
//							System.out.println("Added Subtree " +stree.name + " to the queue");
							ParseTreeTuple t_ptt = new ParseTreeTuple(stree.count_leafes(), stree);
							pq.add(t_ptt);
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Error while applying HDD: " + e.toString());
			}
			
		}
		return result;
		
	}
	
	private boolean checkJSON(ParsedStringSettings pss, boolean log) {
		String terminals = pss.getTree().getTerminals();
		if(pss.getData_type().equals("OBJECT")) {
			try {
				JSONObject obj = new JSONObject(terminals);
				// If we were able to parse the string successfully, return true
//				if(log) {
//					System.out.println("String " + terminals + " successfully parsed using the built in JSON program parser");
//				}
				return true;
			} catch (Exception e) {
//				if(log) {
//					System.out.println("Failed to parse " + terminals + "using the built in JSON program parser. Exception: " + e.toString());
//				}
				return false;
			}
		} else {
			try {
				JSONArray obj = new JSONArray(terminals);
				// If we were able to parse the string successfully, return true
//				if(log) {
//					System.out.println("String " + terminals + " successfully parsed using the built in JSON program parser");
//				}
				return true;
			} catch (Exception e) {
//				if(log) {
//					System.out.println("Failed to parse " + terminals + "using the built in JSON program parser. Exception: " + e.toString());
//				}
				return false;
			}
		}
	}
	private boolean checkIfAtLeastOneNonAnychar(ParseTree stree, HashSet<String> excludeSet, boolean log) {
		for(String s : stree.getParentOfTerminals().split(", ")) {
			if(!excludeSet.contains(s)) {
//				if(log) {
//					System.out.println("Terminals within the tree are represented using at least one non-anychar block.");
//				}
				return true;
			}
		}
//		if(log) {
//			System.out.println("Terminals within the tree are all represented using <anychar>.");
//		}
		return false;
	}
	
	
	/*
	 * Checks if the GG fails parsing the string
	 * If so return false
	 * Return true otherwise
	 * */
	private Boolean checkIfGGFails(String adjusted_string, boolean log) {
		try {
			this.getGolden_grammar_PL().check_string(adjusted_string, this.getGolden_grammar_EP()); // Try to parse the string; return value can be obtained
//			if(log) {
//				System.out.println("Golden grammar successfully parsed " + adjusted_string);
//			}
			return false;
		} catch (Exception e) {
//			if(log) {
//				System.out.println("Golden grammar failed to parse " + adjusted_string + " successfully");
//			}
			return true;
		}
	}
	/*
	 * Replace the given node "biggest_node" with the subtree "stree" 
	 * 
	 * */
	private void replaceBiggestNode(ParseTree biggest_node, ParseTreeTuple stree, Boolean log) {
		biggest_node = stree.getPt();
	}
	
	public ParsedStringSettings startHDD(ParsedStringSettings pss, HashSet<String> excludeSet, ParserLib golden_grammar_PL, EarleyParser golden_grammar_EP, boolean log){		
		if(log) {
			System.out.printf("Applying HDD\nID: %s\nString to minimize: %s\nTree to minimize:\n%s\n", pss.hashCode(), pss.getCreated_string(), pss.getTree().tree_to_string());
		}
		this.setGolden_grammar_PL(golden_grammar_PL);
		this.setGolden_grammar_EP(golden_grammar_EP);
		String result = perses_delta_debug(pss.getTree(), pss.getPl().grammar, pss, pss, excludeSet, log);
		pss.setHdd_string(result);
		if(log) {
			System.out.println("Updated HDD string: " + result);
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
