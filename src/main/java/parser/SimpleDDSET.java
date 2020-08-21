package parser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

// Credits to https://rahul.gopinath.org/post/2020/08/03/simple-ddset/
public class SimpleDDSET {
	private int MAX_TRIES_FOR_ABSTRACTION = 2;
	private HashMap<String, ArrayList<ParseTree>> dd_random_trees = null;
	HashSet<String> excludeSet = null;
	private ArrayList<Integer> abstracted_pt_id = new ArrayList<Integer>(); // ID's of the parsetrees that have already been abstracted
	
	private ParseTree generalize(ParseTree main_tree, ArrayList<Integer> path, String data_type) {
		ParseTree biggest_node = main_tree.getParseTreeForPath(path, 0);
		System.out.println(String.format("Main tree:\n%s\nPath: %s\nBiggest node:\n%s\n", main_tree.tree_to_string(), path.toString(), biggest_node.tree_to_string()));
		if(!biggest_node.is_nt()) { // Terminals are not interesting for us
			return biggest_node;
		}
		if(can_abstract(main_tree, biggest_node, path)) { // Abstract the trees
			// If the abstraction was successful, we can mark the biggest_node as abstract and return the ParseTree
			// As anything that comes after the biggest node has obviously been successful abstracted as well
			biggest_node.setAbstracted(true);
			return biggest_node;
		}
		int path_counter = 0;
		for(ParseTree p : biggest_node.children) {
			ArrayList<Integer> tmp_path = new ArrayList<Integer>();
			tmp_path.addAll(path);
			tmp_path.add(path_counter);
			generalize(main_tree, tmp_path, data_type);
			path_counter++;
		}
		return null;
	}

	private boolean can_abstract(ParseTree main_tree, ParseTree biggest_node, ArrayList<Integer> path) {
		int i = 0;
		while(i < MAX_TRIES_FOR_ABSTRACTION) {
			// Save the main/original tree
			ParseTree main_tree_copy = new ParseTree(main_tree);
			System.out.println("Created a copy for main tree:\n" + main_tree.tree_to_string());
			ParseTree p = replace_all_paths_with_generated_values(main_tree_copy);
			System.out.println("Copied main tree after <abstract> replacements:\n" + main_tree.tree_to_string());
			// Additionally, change the tree of the node we are currently at
			ParseTree rand_tree = get_random_tree(biggest_node.name);
			// TODO temporarily if we cannot find random trees
			if(rand_tree == null) {
				return false;
			}
			System.out.printf("main_tree:\n%s\ntarget_tree:\n%s\nrand_tree:\n%s\n",
					main_tree.tree_to_string(),
					biggest_node.tree_to_string(),
					rand_tree.tree_to_string());
			main_tree_copy.replaceTreeNodeWithPath(main_tree_copy, rand_tree, path, 0);
			
			System.out.println("Copied main tree after biggest node replacements:\n" + p.tree_to_string());
			if(!parsedByJSON(p.getTerminals())) { // If the change of the tree does not hold once
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

	private ParseTree replace_all_paths_with_generated_values(ParseTree main_tree) {
		// Will replace all previously as abstract marked nodes as well as the current node
		if(main_tree.isAbstracted()) {
			System.out.println("Replace_all_paths_with_generated_values: Abstracted: " + main_tree.isAbstracted() + "\nTree:\n" + main_tree.tree_to_string());
			replace_tree_with_random_tree(main_tree, main_tree); // Within the main_tree, replace p with a randomly chosen tree that matches his name
			System.out.printf("Tree is abstracted. Replaced tree with the following random tree:\n%s", main_tree.tree_to_string());
			return null;
		}
		for(ParseTree p : main_tree.children) { // Replace each node that is marked as abstract
			if(p.is_nt() && !excludeSet.contains(p.name)) {
				replace_all_paths_with_generated_values(p);
			}
		}
		return main_tree;
	}

	private ParseTree replace_tree_with_random_tree(ParseTree main_tree, ParseTree target_tree) {
		// Given a main tree, this method will replace the given 
		// target_tree with the randomly chosen tree
		ParseTree rand_tree = get_random_tree(target_tree.name);
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
	private ParseTree get_random_tree(String name) {
		try {
			Random rand = new Random();
			int max = dd_random_trees.get(name).size();
			return dd_random_trees.get(name).get(rand.nextInt(max));
		} catch (Exception e) {
			System.out.println("Get_random_tree: " + e.toString());
			return null;
		}
	}

	private boolean parsedByJSON(String terminals) {
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
	
	private ParseTree generate_random_value(String key) {
		Random rand = new Random();
		int max = dd_random_trees.get(key).size(); // Because nextInt is exclusive on the upper bound
		return dd_random_trees.get(key).get(rand.nextInt(max));
	}
	
	public ParseTree abstractTree(ParsedStringSettings pss, HashSet<String> excludeSet, HashMap<String, ArrayList<ParseTree>> dd_random_trees) {
		this.excludeSet = excludeSet;
		this.dd_random_trees = dd_random_trees;
		System.out.println(String.format("Abstract the following pss:\n%s\n", pss.toString()));
		ArrayList<Integer> path = new ArrayList<Integer>();
		path.add(0);
		path.add(0);
		generalize(pss.getTree(), path, pss.getData_type());
		System.out.println(String.format("Abstracted the tree:\n%s\n", pss.toString()));
		return null;
	}
}
