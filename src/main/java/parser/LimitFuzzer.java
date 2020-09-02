package parser;
// Credits https://rahul.gopinath.org/post/2019/05/28/simplefuzzer-01/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class LimitFuzzer {
	private Grammar grammar;
	private HashMap<String, Integer> key_cost = new HashMap<String, Integer>();
	private HashMap<String, TreeMap<String, Integer>> cost = new HashMap<String, TreeMap<String, Integer>>();
	
	public LimitFuzzer(Grammar grammar) {
		this.grammar = grammar;
		this.key_cost = new HashMap<String, Integer>();
		this.cost = this.compute_cost(grammar);
	}

	private ArrayList<String> gen_key(String key, int depth, int max_depth) {
		if(!this.grammar.containsKey(key)) {
			ArrayList<String> obj = new ArrayList<String>();
			obj.add(key);
			return obj;
		}
		ArrayList<GRule> rules = new ArrayList<GRule>();
		if(depth > max_depth) {
			ArrayList<List<Object>> clst = new ArrayList<List<Object>>(); // Array list with tuples
			for(GRule rule : this.grammar.get(key)) {
				List<Object> tuple = new ArrayList<Object>();
				tuple.add(0, this.cost.get(key).get(rule.toString()));
				tuple.add(1, rule);
				clst.add(tuple);
			}
			for(List<Object> obj : clst) {
				int c = (int) obj.get(0);
				GRule r = (GRule) obj.get(1);
				if(c == (int) clst.get(0).get(0)) {
					rules.add(r);
				}
			}
		} else {
			for(GRule g : this.grammar.get(key)) {
				rules.add(g);
			}
		}
		Random rand = new Random();
		int rand_pos = rand.nextInt(rules.size());
		return this.gen_rule(rules.get(rand_pos), depth + 1, max_depth);
	}
	
	private ArrayList<String> gen_rule(GRule rule, int depth, int max_depth) {
		ArrayList<String> res = new ArrayList<String>();
		for(String token : rule) {
			ArrayList<String> obj = this.gen_key(token, depth, max_depth);
			if(obj != null) {
				res.addAll(obj);
			}
		}
		return res;
	}

	public ArrayList<ArrayList<String>> fuzz(String key, int max_depth, int n) {
		ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < n; i++) {
			res.add(i, this.gen_key(key, 0, max_depth));
		}
		return res;
	}

	private int symbol_cost(Grammar grammar, String symbol, HashSet<String> seen) {
//		System.out.printf("Symbol: %s\n", symbol);
		if(key_cost.containsKey(symbol)) {
			return this.key_cost.get(symbol);
		}
		if(seen.contains(symbol)) {
			this.key_cost.put(symbol, Integer.MAX_VALUE);
			return Integer.MAX_VALUE;
		}
		int result = 0; // default = 0
		ArrayList<Integer> tmp_min = new ArrayList<Integer>();  
		for(GRule rule : grammar.get(symbol)) {
			HashSet<String> tmp_seen = new HashSet<String>();
			tmp_seen.addAll(seen);
			tmp_seen.add(symbol);
			int i = this.expansion_cost(grammar, rule, tmp_seen);
			if(i != 0) {
				tmp_min.add(i);
			}
			// System.out.printf("Min compare: %s\n", a);
		}
		Collections.sort(tmp_min);
		if(tmp_min.size() > 0) {
			result = tmp_min.get(0);
		}
		this.key_cost.put(symbol, result);
		return result;
	}
	
	private int expansion_cost(Grammar grammar, GRule tokens, HashSet<String> seen) {
		int res = 0;
//		System.out.printf("Tokens: %s\n", tokens.toString());
		ArrayList<Integer> tmp_max = new ArrayList<Integer>();
		for(String token : tokens) {
			if(grammar.containsKey(token)) {
				res = this.symbol_cost(grammar, token, seen);
				// System.out.printf("Max compare: %s\n", a);
				// res = Math.max(0, a);
				res = res == Integer.MAX_VALUE ? res : res + 1;
				tmp_max.add(res);
			}
		}
		Collections.sort(tmp_max);
		if(tmp_max.size() > 0) {
			res = tmp_max.get(tmp_max.size() - 1); // As the first element is the smallest one, we have to return the last element
		} else {
			res++;
		}
		return res;
	}
	
	private HashMap<String, TreeMap<String, Integer>> compute_cost(Grammar grammar){
		HashMap<String, TreeMap<String, Integer>> cost = new HashMap<String, TreeMap<String, Integer>>();
		for(Map.Entry<String, GDef> entry : grammar.entrySet()) {
			String name = entry.getKey();
			// name = "<start>";
			// GDef rule = entry.getValue();
			if(!cost.containsKey(name)) {
				cost.put(name, new TreeMap<String,Integer>());
			}
			for(GRule rule : grammar.get(name)) {
				if(cost.containsKey(name)) {
					// TreeMap<String, Integer> new_map = new TreeMap<String, Integer>();
					cost.get(name).put(rule.toString(), expansion_cost(grammar, rule, new HashSet<String>()));
					// cost.get(name).putAll(new_map);
					//		expansion_cost(grammar, rule, new HashSet<String>());
					// cost.get(name).add(rule.toString(), ));
				} else {
					TreeMap<String, Integer> new_map = new TreeMap<String, Integer>();
					new_map.put(rule.toString(), expansion_cost(grammar, rule, new HashSet<String>()));
					cost.put(name, new_map);
				}
			}
			
		}
//		for(Map.Entry<String, TreeMap<String, Integer>> x : cost.entrySet()) {
//			System.out.printf("Symbol: %s\n", x.getKey());
//			for(Map.Entry<String, Integer> y : x.getValue().entrySet()) {
//				System.out.printf("%s\n", y.toString());
//			}
//			System.out.printf("\n\n\n");
//			
//		}
		return cost;
	}
	
}