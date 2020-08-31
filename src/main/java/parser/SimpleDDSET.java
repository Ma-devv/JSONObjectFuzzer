package parser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

// Credits to https://rahul.gopinath.org/post/2020/08/03/simple-ddset/
public class SimpleDDSET {
	private int MAX_TRIES_FOR_ABSTRACTION = 10;
	HashSet<String> excludeSet = null;
	private ArrayList<Integer> abstracted_pt_id = new ArrayList<Integer>(); // ID's of the parsetrees that have already been abstracted
	
	private ParseTree generalize(ParseTree main_tree, ArrayList<Integer> path, EarleyParser earleyParserGG) {
		ParseTree biggest_node = main_tree.getParseTreeForPath(path, 0);
		System.out.println(String.format("Main tree:\n%s\nPath: %s\nBiggest node:\n%s\n", main_tree.tree_to_string(), path.toString(), biggest_node.tree_to_string()));
		if(biggest_node.name.equals("<integer>")) {
			System.out.printf("");
		}
		if(!biggest_node.is_nt()) { // Terminals are not interesting for us
			return biggest_node;
		}
		if(can_abstract(main_tree, biggest_node, path, earleyParserGG)) { // Abstract the trees
			// If the abstraction was successful, we can mark the biggest_node as abstract and return the ParseTree
			// As anything that comes after the biggest node has obviously been successful abstracted as well
			biggest_node.setAbstracted(true);
			return biggest_node;
		}
		int path_counter = 0;
		for(ParseTree p : biggest_node.children) {
			if(!p.is_nt() && !excludeSet.contains(p.name)) {
				path_counter++;
				continue;
			}
			ArrayList<Integer> tmp_path = new ArrayList<Integer>();
			tmp_path.addAll(path);
			tmp_path.add(path_counter);
			generalize(main_tree, tmp_path, earleyParserGG);
			path_counter++;
		}
		return null;
	}

	private boolean can_abstract(ParseTree main_tree, ParseTree biggest_node, ArrayList<Integer> path, EarleyParser earleyParserGG) {
		int i = 0;
		while(i < MAX_TRIES_FOR_ABSTRACTION) {
			// Save the main/original tree
			ParseTree main_tree_copy = new ParseTree(main_tree);
			System.out.println("Created a copy for main tree:\n" + main_tree.tree_to_string());
			// replace_all_paths_with_generated_values(main_tree_copy, new ArrayList<Integer>(), earleyParserGG);
			System.out.println("Copied main tree after <abstract> replacements:\n" + main_tree.tree_to_string());
			// Additionally, change the tree of the node we are currently at
			ParseTree rand_tree = create_random_tree(biggest_node.name, earleyParserGG);
			if(rand_tree == null) {
				return false;
			}
			HashMap<String, TreeMap<Integer, ArrayList<ArrayList<Integer>>>> lst = new HashMap<String, TreeMap<Integer,ArrayList<ArrayList<Integer>>>>();
			rand_tree.getPathListForSymbols(new ArrayList<Integer>(), 0, lst);
			Outer_loop:
			for(Entry<Integer, ArrayList<ArrayList<Integer>>> tree_map : lst.get(biggest_node.name).entrySet()) {
				for(ArrayList<Integer> new_path : tree_map.getValue()) {
					rand_tree = rand_tree.getParseTreeForPath(new_path, 0);
					break Outer_loop;
				}
			}
			// rand_tree = rand_tree.getParseTreeForPath(path, 0);
			if(rand_tree == null) {
				return false;
			}
			System.out.printf("main_tree:\n%s\ntarget_tree:\n%s\nrand_tree:\n%s\n",
					main_tree.tree_to_string(),
					biggest_node.tree_to_string(),
					rand_tree.tree_to_string());
			main_tree_copy.replaceTreeNodeUsingPath(rand_tree, path, 0);
			
			System.out.println("Copied main tree after biggest node replacements:\n" + main_tree_copy.tree_to_string());
			String merged_input = main_tree_copy.getTerminals();
			if(!checkIfJSONPasses(merged_input) || !checkIfGGFails(earleyParserGG, merged_input)) { // If the change of the tree does not hold once
				System.out.println("Unable to parse the newly merged string; return false");
				// Then the abstraction of the biggest_node failed
				return false;
			}
			i++;
		}
		// If we were able to parse the string at any time (so with each abstraction), we can mark the tree as abstracted
		System.out.println("Successfully abstracted the node\n" + biggest_node.tree_to_string());
		biggest_node.setAbstracted(true);
		return true;
	}

	private void replace_all_paths_with_generated_values(ParseTree main_tree, ArrayList<Integer> path, EarleyParser earleyParserGG) {
		// Will replace all previously as abstract marked nodes as well as the current node
		ParseTree curr_tree = main_tree.getParseTreeForPath(path, 0);
		if(main_tree.isAbstracted()) {
			System.out.println("Replace_all_paths_with_generated_values: Abstracted: " + main_tree.isAbstracted() + "\nTree:\n" + main_tree.tree_to_string());
			ParseTree rand_tree = create_random_tree(curr_tree.name, earleyParserGG);
			HashMap<String, TreeMap<Integer, ArrayList<ArrayList<Integer>>>> lst = new HashMap<String, TreeMap<Integer,ArrayList<ArrayList<Integer>>>>();
			rand_tree.getPathListForSymbols(new ArrayList<Integer>(), 0, lst);
			Outer_loop:
			for(Entry<Integer, ArrayList<ArrayList<Integer>>> tree_map : lst.get(curr_tree.name).entrySet()) {
				for(ArrayList<Integer> new_path : tree_map.getValue()) {
					rand_tree = rand_tree.getParseTreeForPath(new_path, 0);
					break Outer_loop;
				}
			}
			main_tree.replaceTreeNodeUsingPath(rand_tree, path, 0);
			System.out.printf("Tree is abstracted. Replaced tree with the following random tree:\n%s", main_tree.tree_to_string());
		} else {
			int child = 0;
			for(ParseTree p : curr_tree.children) { // Replace each node that is marked as abstract
				if(p.is_nt() && !excludeSet.contains(p.name)) {
					ArrayList<Integer> tmp_path = new ArrayList<Integer>();
					tmp_path.addAll(path);
					tmp_path.add(child);
					replace_all_paths_with_generated_values(main_tree, tmp_path, earleyParserGG);
				}
				child++;
			}

		}
	}

	private ParseTree replace_tree_with_random_tree(ParseTree main_tree, ParseTree target_tree, EarleyParser earleyParserGG) {
		// Given a main tree, this method will replace the given 
		// target_tree with the randomly chosen tree
		ParseTree rand_tree = create_random_tree(target_tree.name, earleyParserGG);
		System.out.printf("main_tree(%d):\n%s\ntarget_tree(%d):\n%s\nrand_tree(%d):\n%s\n", 
				main_tree.hashCode(),
				main_tree.tree_to_string(),
				target_tree.hashCode(),
				target_tree.tree_to_string(),
				rand_tree.hashCode(),
				rand_tree.tree_to_string());
		main_tree.replaceTreeNode(target_tree, rand_tree, true);
		return null;
	}
	private ParseTree create_random_tree(String name, EarleyParser earleyParserGG) {
		ParseTree pt = null;
		try {
			LimitFuzzer lf = new LimitFuzzer(earleyParserGG.grammar);
			while(true) {
				ArrayList<ArrayList<String>> aas_lst = lf.fuzz(name, 1000, 10);
				for(ArrayList<String> as : aas_lst) {
					if(as != null) {
						StringBuilder sb = new StringBuilder();
						for(String s : as) {
							sb.append(s);
						}
						if(pt == null) {
							List<Object> o = Fuzzer.parseStringUsingLazyExtractor(sb.toString(), earleyParserGG, 10);
							if(o == null) {
								return pt;
							}
							pt = (ParseTree) o.get(0);
						} else if(sb.length() > 0 && sb.length() > pt.getTerminals().length()) {
							List<Object> o = Fuzzer.parseStringUsingLazyExtractor(sb.toString(), earleyParserGG, 10);
							if(o == null) {
								return pt;
							}
							pt = (ParseTree) o.get(0);
//							if(pt != null) {
//								int counter =  0;
//								System.out.printf("[%d]: %s\n%s\n", counter++, sb.toString(), pt.tree_to_string());
//							}
						}
					}
				}
				if(pt.getTerminals().length() > 5 || pt == null) {
					break;
				}
			}
//			for(ArrayList<String> as : lf.fuzz(name, 1000, 1)) {
//				StringBuilder sb = new StringBuilder();
//				for(String s : as) {
//					sb.append(s);
//				}
//				if(sb.length() > 0) {
//					pt = (ParseTree) Fuzzer.parseStringUsingLazyExtractor(sb.toString(), earleyParserGG, 10).get(0);
//					if(pt != null) {
//						System.out.printf("[%d]: %s\n%s\n", counter++, sb.toString(), pt.tree_to_string());
//					}
//				}
//			}
		} catch (Exception e) {
			System.out.println("create_random_tree: " + e.toString());
			return null;
		}
		return pt;
	}

	private Boolean checkIfGGFails(EarleyParser ep, String adjusted_string) {
		// Negation as we want to check if the GG FAILS; hence we must negate the result
		return !Fuzzer.checkIfStringCanBeParsedWithGivenGrammar(ep, adjusted_string);
	}
	
	
	private boolean checkIfJSONPasses(String terminals) {
		System.out.println("Try to parse the string: " + terminals);
		if(terminals.startsWith("{")) {
			try {
				JSONObject obj = new JSONObject(terminals);
				System.out.println("Parsed successfully");
				return true;
			} catch (Exception e) {
				System.out.println("Not parsed successfully");
				return false;
			}
		} else {
			try {
				JSONArray arr = new JSONArray(terminals);
				System.out.println("Parsed successfully");
				return true;
			} catch (Exception e) {
				System.out.println("Not parsed successfully");
				return false;
			}
		}
	}
	
	public ParseTree abstractTree(ParsedStringSettings pss, HashSet<String> excludeSet, EarleyParser earleyParserGG) {
		this.excludeSet = excludeSet;
		System.out.println(String.format("Abstract the following pss:\n%s\n", pss.toString()));
		ArrayList<Integer> path = new ArrayList<Integer>();
		path.add(0);
		path.add(0);
		generalize(pss.getDd_tree(), path, earleyParserGG);
		System.out.println(String.format("Abstracted the tree:\n%s\n", pss.toString()));
		return null;
	}
}
