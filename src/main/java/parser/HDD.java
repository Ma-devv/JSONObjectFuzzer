package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class HDD {	
	/*
	 * Searches for subtrees whose name matches symbol
	 * */
	private ParserLib golden_grammar_PL;
	private EarleyParser golden_grammar_EP;

	public String perses_delta_debug(ParseTree tree, Grammar grammar, ParsedStringSettings pss, ParsedStringSettings original_pss, HashSet<String> excludeSet, boolean log) {
		String result = tree.getTerminals();
		ParseTree pt = new ParseTree(tree.name, tree.children); // create new ParseTree object (deep copy)
		// ParseTreeTupleComparator pttComp = new ParseTreeTupleComparator();
		PriorityQueue<ParseTreeTriple> pq = new PriorityQueue<ParseTreeTriple>(new ParseTreeTripleComparator());
		HashMap<Integer, ArrayList<Integer>> init_hmap = new HashMap<Integer, ArrayList<Integer>>();
		int curr_depth = 0;
		init_hmap.put(curr_depth, new ArrayList<Integer>());
		pq.add(new ParseTreeTriple(pt.count_leafes(), pt, init_hmap)); // Add the "root" tree to the key to get started
		ArrayList<Integer> curr_path = new ArrayList<Integer>();
		ArrayList<Integer> new_path = new ArrayList<Integer>(); // To override curr_path when reprocess = true;
		// TODO Need to be updated at some point when the merging was successful
		// Initialize paths for symbols
		// String = symobl; name of the ParseTree
		// value is a Map whose key is the depth
		// the value of this key again is an arraylist, that contains the paths (multiple paths can have the same depth, e.g. <ws>)
		HashMap<String, TreeMap<Integer, ArrayList<ArrayList<Integer>>>> path_to_symbols = new HashMap<String, TreeMap<Integer,ArrayList<ArrayList<Integer>>>>();
		tree.getPathListForSymbols(new ArrayList<Integer>(), 0, path_to_symbols);
		
		while(!pq.isEmpty()) { // While there are trees in the queue
			try {
				ParseTree reprocess = null;
				ParseTreeTriple ptt = pq.poll();
				ParseTree biggest_node = ptt.getPt(); // Get the "biggest" node
				Map.Entry<Integer, ArrayList<Integer>> k_v_depth_path = ptt.getTree_path_with_depth().entrySet().iterator().next();
				curr_depth = k_v_depth_path.getKey();
				curr_path = k_v_depth_path.getValue();
				if(log) {
					System.out.printf("Current path: %s of the main tree:\n%s\n", curr_path, biggest_node.tree_to_string());
				}
				String bsymbol = biggest_node.name; // Get the "biggest" symbol
				// We need the biggest symbol to find subtrees with the same name/token
				
				if(path_to_symbols.get(bsymbol) == null) { // Are there subtrees for the matching tree symbol/name?
					continue;
				}
				Outer_loop:
				for(Map.Entry<Integer, ArrayList<ArrayList<Integer>>> depth_path_list_mapping : path_to_symbols.get(bsymbol).entrySet()) {
					// Also important: verify that the depth of the looped path is bigger than the one of the current path
					// Otherwise we would replace a subtree with a top-subtree
					int looped_depth = depth_path_list_mapping.getKey();
					if(looped_depth <= curr_depth) {
						continue;
					}
					for(ArrayList<Integer> looped_path : depth_path_list_mapping.getValue()) { // For each path
						// Check if the path is reachable and if the subtree is a real children of the tree
						if(!pss.getHdd_tree().pathReachable(curr_path, looped_path, 0)) {
							if(log) {
								System.out.printf("Path either not reachable or the subtree is not a real children\n");
							}
							continue;
						}
						System.out.printf("Looped path: %s\n", looped_path.toString());
						// Check if current and received path/tree are the same
						ParseTree subtree = pss.getHdd_tree().getParseTreeForPath(looped_path, 0);
						// Create a copy of the pss so that we are also able to undo the changes
						ParsedStringSettings saved_pss = new ParsedStringSettings(pss);
						ParseTree saved_biggest_node = new ParseTree(biggest_node);
//						System.out.println("Saved biggest pss:\n" + saved_pss.getTree().tree_to_string());
						System.out.printf("Tree that should replace the biggest node:\n%s\n", subtree.tree_to_string());
						// pss.getTree().replaceTreeNode(biggest_node, stree.getPt(), log);
						pss.getHdd_tree().replaceTreeNodeUsingPath(subtree, curr_path, 0);
						System.out.println("Saved biggest pss:\n" + saved_pss.getHdd_tree().tree_to_string());
						System.out.println("New Tree representing the string " + pss.getHdd_tree().getTerminals() + ":\n" + pss.getHdd_tree().tree_to_string());
						// Check if the subtree has at least one terminal character outside of <anychars> block and is not empty or equals a space character
						if(checkIfAtLeastOneNonAnychar(pss.getHdd_tree(), excludeSet, log) && 
								(!(pss.getHdd_tree().getTerminals().equals("") || 
								pss.getHdd_tree().getTerminals().equals(" "))) &&
								pss.getHdd_tree().getTerminals().length() <= saved_pss.getHdd_tree().getTerminals().length()) {
							if(checkIfJSONPasses(pss, log)) {
								// Results in us having a new set of characters that we can try to parse using the adjusted grammar
								// Now we have to check if the modified terminals can be parsed again using the adjusted grammar
								if(checkIfGGFails(getGolden_grammar_EP(), pss.getHdd_tree().getTerminals())) {
									new_path = new ArrayList<Integer>();
									new_path = pss.getHdd_tree().getPathForNewMergedTree(subtree, new ArrayList<Integer>());
									path_to_symbols.clear();
									pss.getHdd_tree().getPathListForSymbols(new ArrayList<Integer>(), 0, path_to_symbols);
									if(log) {
										System.out.format("Replace the biggest node with:\n%s\n", subtree.tree_to_string());
									}
									biggest_node = subtree; 
									if(log) {
										System.out.println("Set new \"Minimized string using HDD\": " + pss.getHdd_tree().getTerminals()); // Created string = hdd string; we set the hdd string
									}
									reprocess = subtree;
									result = pss.getHdd_tree().getTerminals();
									// TODO BREAK OUTER LOOP
									break Outer_loop;
								}
							}
						}
						// Something went wrong; undo the changes
						// pss = saved_pss;
						if(log) {
							System.out.println("Undo changes; restore biggest node and tree");
						}
						pss.setHdd_tree(new ParseTree(saved_pss.getHdd_tree()));
						biggest_node = saved_biggest_node;
//						System.out.println("Undone changes. Biggest node:\n" + biggest_node.tree_to_string());
//						System.out.println("Master tree:\n" + pss.getTree().tree_to_string());
					}
				}

				if(reprocess != null) { // Did we found a subtree/sub-input that has been parsed successfully?
					HashMap<Integer, ArrayList<Integer>> tmp_hmap = new HashMap<Integer, ArrayList<Integer>>();
					// The depth stays the same as we replaced the node of the biggest tree
					tmp_hmap.put(curr_depth, new_path);
					ParseTreeTriple t_ptt = new ParseTreeTriple(biggest_node.count_leafes(), biggest_node, tmp_hmap);
					pq.add(t_ptt);
				}
				else {
					for(int i = 0; i < biggest_node.children.size(); i++) {
						ParseTree stree = biggest_node.children.get(i);
						if(stree.is_nt() && !excludeSet.contains(stree.name)) { // Not interested in terminals and in <anychar> blocks
							System.out.println("Added Subtree " +stree.name + " to the queue");
							ArrayList<Integer> tmp_path = new ArrayList<Integer>();
							tmp_path.addAll(curr_path);
							tmp_path.add(i);
							HashMap<Integer, ArrayList<Integer>> tmp_hmap = new HashMap<Integer, ArrayList<Integer>>();
							tmp_hmap.put(curr_depth + 1, tmp_path);
							ParseTreeTriple t_ptt = new ParseTreeTriple(stree.count_leafes(), stree, tmp_hmap);
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
	
	private boolean checkIfJSONPasses(ParsedStringSettings pss, boolean log) {
		String terminals = pss.getHdd_tree().getTerminals();
		// terminals = "[R14^y]";
		if(terminals.startsWith("{")) {
			try {
				JSONObject obj = new JSONObject(terminals);
				return true;
			} catch (Exception e) {
				return false;
			}
		} else {
			try {
				JSONArray obj = new JSONArray(terminals);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	private boolean checkIfAtLeastOneNonAnychar(ParseTree stree, HashSet<String> excludeSet, boolean log) {
		for(String s : stree.getParentOfTerminals().split(", ")) {
			if(!excludeSet.contains(s)) {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * Checks if the GG fails parsing the string
	 * If so return false
	 * Return true otherwise
	 * */
	private Boolean checkIfGGFails(EarleyParser ep, String adjusted_string) {
		// Negation as we want to check if the GG FAILS; hence we must negate the result
		return !Fuzzer.checkIfStringCanBeParsedWithGivenGrammar(ep, adjusted_string);
	}
	
	public ParsedStringSettings startHDD(ParsedStringSettings pss, HashSet<String> excludeSet, ParserLib golden_grammar_PL, EarleyParser golden_grammar_EP, boolean log){		
		if(log) {
			System.out.printf("Applying HDD\nID: %s\nString to minimize: %s\nTree to minimize:\n%s\n", pss.hashCode(), pss.getCreated_string(), pss.getHdd_tree().tree_to_string());
		}
		this.setGolden_grammar_PL(golden_grammar_PL);
		this.setGolden_grammar_EP(golden_grammar_EP);
		String result = perses_delta_debug(pss.getHdd_tree(), pss.getPl().grammar, pss, pss, excludeSet, log);
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
