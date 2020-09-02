package parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.json.*;

import jdk.internal.org.jline.terminal.Terminal;

import java.awt.Choice;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

class GRule extends ArrayList<String> {
    public GRule() {
    }
}

class GDef extends ArrayList<GRule> {
    public GDef() {
    }
}

class Grammar extends HashMap<String, GDef> {
    public Grammar() {
    }
}

class SK {
    State s;
    char k;
    SK(State s, char k) {
        this.s = s;
        this.k = k;
    }
}

class TPath {
    SK sk;
    List<Column> chart;
    public TPath(SK sk, List<Column> chart) {
        this.sk = sk;
        this.chart = chart;
    }
}

class NamedForest {
    String name;
    ArrayList<ArrayList<TPath>> paths;
    public NamedForest(String name, ArrayList<ArrayList<TPath>> paths) {
        this.name = name;
        this.paths = paths;
    }
}

class SIInfo {
    State state;
    int index;
    char c;
    public SIInfo(State state, int index, char c) {
        this.state = state;
        this.index = index;
        this.c = c;
    }
}

class G {
    Grammar grammar;
    Map<String, Double> min_len;
    G(Grammar g) {
        this.grammar = g;
        this.min_len = this.compute_min_length();
    }

    public List<String> nullable() {
        List<String> nullable = new ArrayList<String>();
        for (String key : this.min_len.keySet()) {
            if (this.min_len.get(key) == 0.0) {
                nullable.add(key);
            }
        }
        return nullable;
    }

    private double _key_min_length(String k, Set<String> seen) {
        if (!this.grammar.containsKey(k)) {
            return k.length();
        }
        if (seen.contains(k)) {
            return Double.POSITIVE_INFINITY;
        }

        double min = Double.POSITIVE_INFINITY;
        for (GRule r : this.grammar.get(k)) {
            Set<String> inter = new HashSet<String>(seen);
            inter.add(k);
            double m = this._rule_min_length(r, inter);
            if (m < min) {
                min = m;
            }
        }
        return min;
    }

    private double _rule_min_length(GRule rule, Set<String> seen) {
        double sum = 0;
        for (String k : rule) {
            sum += this._key_min_length(k, seen);
        }
        return sum;
    }

    Map<String, Double> compute_min_length() {
        Map<String, Double> min_len = new HashMap<String, Double>();
        for (String k : this.grammar.keySet()) {
            min_len.put(k, _key_min_length(k, new HashSet<String>()));
        }
        return min_len;
    }

    Grammar single_char_tokens() {
        Grammar g_ = new Grammar();
        for (String key : this.grammar.keySet()) {
            GDef rules_ = new GDef();
            for (GRule rule : this.grammar.get(key)) {
                GRule rule_ = new GRule();
                for (String token : rule) {
                    if (this.grammar.keySet().contains(token)) {
                        rule_.add(token);
                    } else {
                        for (String c : token.split("")) {
                            int l = c.length();
                            switch (l) {
                                case 1:
                                    rule_.add(c);
                                    break;
                                case 0:
                                    break;
                                default:
                                    throw new RuntimeException("Invalid token");
                            }
                        }
                    }
                }
                rules_.add(rule_);
            }
            g_.put(key, rules_);
        }
        return g_;
    }
}
// Parser.py

class ParseTree{
    String name;
    ArrayList<ParseTree> children;
    private boolean indented_nt = false; // TODO change - howto?
    private boolean anychar_seen_gpofa = false; // TODO change GPOFA: getPosOfFristAnychar()
    private boolean abstracted = false;
    
    public ParseTree(String name, ArrayList<ParseTree> children) {
        this.name = name;
        this.children = children;
    }
    /*
     * Copy
     * */
    
    public ParseTree(ParseTree source) {
    	this.name = source.name;
    	this.indented_nt = source.indented_nt;
    	this.anychar_seen_gpofa = source.anychar_seen_gpofa;
    	this.pos_counter = source.pos_counter;
    	this.currently_seen_anychars = source.currently_seen_anychars;
    	this.return_break = source.return_break;
    	this.children = new ArrayList<ParseTree>();
    	for(ParseTree pt_source : source.children) {
    		this.children.add(new ParseTree(pt_source));
    	}
    	this.abstracted = source.abstracted;
    }
        
    private String _tree_to_string_line(String s) {
    	s += "'" + this.name + "', ";
    	for(ParseTree p : this.children) {
    		s += "[";
    		s += p._tree_to_string_line("");
    		s += "]";
    	}
    	if(this.children.size() == 0) {
    		s += "[]";
    	}
    	return s;
    }
    // Credits: https://stackoverflow.com/questions/5849154/can-we-write-our-own-iterator-in-java
    public Iterator<ParseTree> iterator() {
        Iterator<ParseTree> it = new Iterator<ParseTree>() {

            private int currentIndex = 0;

        	@Override
        	public boolean hasNext() {
        		return children.size() > currentIndex && children.get(currentIndex) != null;
        	}

        	@Override
        	public ParseTree next() {
        		return(children.get(currentIndex++));}

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }

	@Override
	public String toString() {
		return String.format("Terminals represented by this tree: %s\nTree:\n%s\n", this.getTerminals(), this.tree_to_string());
	}
	
	public String tree_to_string_line() {
    	return "(" + this._tree_to_string_line("") + ")";
    }
    
    private int _count_leafes(ParseTree tree, int nodeCount) {
    	// System.out.println(tree.name);
    	int result = nodeCount;
    	if(tree.children.size() == 0) {
    		// System.out.println(tree.name);
    		return 1;
    	}
    	for(ParseTree p : tree.children) {
    		// System.out.println(p.name);
			result += _count_leafes(p, 0);
    	}
    	return result;
    			
    }
    public int count_leafes() {
    	return this._count_leafes(this, 0);
    }
    public boolean is_nt() {
    	return (this.name.charAt(0) == '<' && this.name.charAt(this.name.length() - 1) == '>');
    }
    public String getTerminals() {
    	return this._getTerminals(this, "");
    }
    
    /*
     * Returns the value of the terminals
     * 
     * */
    private String _getTerminals(ParseTree result, String tree) {
    	if(!(result.is_nt())) {
    		return result.name;
    	}
        for (ParseTree p : result.children) {
        	tree += p._getTerminals(p, "");
        }
        return tree;
    }
    
    public String getParentOfTerminals() {
    	return this._getParentOfTerminals(this, "");
    }
    
    /*
     * Returns the value of the terminals
     * 
     * */
    private String _getParentOfTerminals(ParseTree result, String tree) {
        for (ParseTree p : result.children) {
        	if(!p.is_nt()) {
        		tree += result.name + ", ";
        	}
        	tree += p._getParentOfTerminals(p, "");
        }
        return tree;
    }
    
    public String tree_print_nterminals_wo_anychars() {
    	return this._tree_print_nterminals_wo_anychars(this, "", 0, false, this);
    }
    
    /*
     * Returns the name/value of the non terminals
     * 
     * */
    private String _tree_print_nterminals_wo_anychars(ParseTree result, String tree, int indent, boolean seen_anychar, ParseTree master) {
    	if(!(result.is_nt())) {
			if(seen_anychar) {
				if(indented_nt) {
					tree += result.name;
				}
				else {
					indented_nt = true;
					// Patch all the other childs
					setIndentedTrue(master);
					tree += "   ".repeat(indent+1) + "<anychar/s/p>\n";
					tree += "   ".repeat(indent+2) + this.name;
				}
    		}
			else {
				if(indented_nt) {
					tree += "\n";
					tree += "   ".repeat(indent) + this.name + "\n";
				}
				else {
					tree += "   ".repeat(indent) + this.name + "\n";
				}
    		}
    	}
    	else if(!(result.name.equals("<anychars>") || result.name.equals("<anychar>") || result.name.equals("<anycharsp>") || result.name.equals("<anycharp>"))) {
    		if(indented_nt) {
    			tree += "   ".repeat(indent) + this.name + "\n";
    		}
    		else {
    			tree += "   ".repeat(indent) + this.name + "\n";
    		}
    	}
    	else{
    		// Seen <anychars>
    		seen_anychar = true;
    		indent -= 1;
    	}
        for (ParseTree p : this.children) {
            tree = p._tree_print_nterminals_wo_anychars(p, tree, indent + 1, seen_anychar, master);
        }
        return tree;
    }
    
    private String _removeAnycharChars(ParseTree pt, String s, boolean seen_anychar) {
    	if(pt.is_nt() && seen_anychar) {
    		s += pt.name;
    	}
    	else if(pt.name.equals("<anychars>") || pt.name.equals("<anychar>") || pt.name.equals("<anycharsp>") || pt.name.equals("<anycharp>")) {
    		seen_anychar = true;
    	}
    	
    	for(ParseTree p : pt.children) {
    		s = _removeAnycharChars(p, s, seen_anychar);
    	}
    	return s;
    	
    }
    
    public String removeAnycharChars() {
    	return _removeAnycharChars(this, "", false);
    }
    
    private void setIndentedTrue(ParseTree master) {
    	master.indented_nt = true;
		for(ParseTree pt : master.children) {
			setIndentedTrue(pt);
		}
		
	}
	public int count_nodes(int nodeCount, HashSet<String> excludeSet) {
    	int result = nodeCount;
    	if(excludeSet.contains(this.name)) {
    		return result;
    	}
    	for(ParseTree p : this.children) {
    		result = p.count_nodes(result + 1, excludeSet);
    	}
    	return result;
    }
    private String _tree_to_string(int indent, String tree) {
    	String s = this.name.equals(" ") ? "' '" : "'" + this.name + "'";
    	tree += "\t".repeat(indent) + s;
        for (ParseTree p : this.children) {
            tree = p._tree_to_string(indent + 1, tree + "\n");
        }
        return tree;
    }

    public String tree_to_string() {
    	return this._tree_to_string(0, "");
    }
    
    private String _abstract_tree_to_string(int indent, String tree) {
    	tree += "   ".repeat(indent) + this.name + "\tA: " + this.isAbstracted();
        for (ParseTree p : this.children) {
            tree = p._abstract_tree_to_string(indent + 1, tree + "\n");
        }
        return tree;
    }

    public String abstract_tree_to_string() {
    	return this._abstract_tree_to_string(0, "");
    }
    
	private int _getPosOfFirstAnychar(ParseTree pt, int counter) {
		if(!pt.is_nt()) {
			if(!anychar_seen_gpofa) {
				counter++;
			}
		}
		else if(pt.name.equals("<anychars>") || pt.name.equals("<anychar>") || pt.name.equals("<anycharsp>") || pt.name.equals("<anycharp>")) {
			if(!anychar_seen_gpofa) {
				setAnycharSeen(this, true);
			}
			
		}
		for(ParseTree p : pt.children) {
			counter = _getPosOfFirstAnychar(p, counter);
		}
		return counter;
	}
	
	/*
	 * Returns the length of the string BEFORE hitting a 
	 * <anychar> rule in the tree
	 * 
	 * */
	public int getPosOfFirstAnychar() {
		return _getPosOfFirstAnychar(this, 0);
	}
	
	
	// Returns the (amount_of_seen_anychar + 1) position of an anychar tag
	// E.g. calling this method with amount_of_seen_anychar = 0, this will return the position of the FIRST anychar tag as well as its length
	// When calling this method with amount_of_seen_anychar = 1, this will return the position of the SECOND anychar tag as well as its length
	// Returns null when no further anychar tags are coming after amount_of_seens_anychar
	int pos_counter = 0;
	int currently_seen_anychars = 0;
	boolean return_break = false;
	private HashMap<Integer, Integer> _getMapOfPosStringAnychar(ParseTree pt, HashMap<Integer, Integer> result, int amount_of_seen_anychar) {
//		System.out.println(pt.name);
		if(return_break) {
//			System.out.println("Return break");
			return result;
		}
		// Check if "dead block". I.e. an anychar block without a terminal as a child
//		else if((pt.name.equals("<anychars>") || pt.name.equals("<anychar>") || 
//		    pt.name.equals("<anycharsp>") || pt.name.equals("<anycharp>")) &&
//			pt.children.size() == 0) { 
//			System.out.println("Deadblock");
//			return result;
//		}
		else if(!pt.is_nt()) { // Is the given tree a character, i.e. a terminal?
			if(!anychar_seen_gpofa) { // If so, did we already see a anychar tag?
				pos_counter++;	 // If not, then we increase the position of the starting anychar string as we then pass by one terminal non anychar character
//				System.out.println("Terminal. No <anychar> tag has been visited yet. Increased pos counter");
			}
			else { // We already saw an anychar tag
				if(currently_seen_anychars >= amount_of_seen_anychar) {
//					System.out.println("Terminal. Already visited an <anychar> tag");
					if(result == null) { // Is this the first terminal character after an anychar tag?
						result = new HashMap<Integer, Integer>(); // If yes, we create a new HashMap
						result.put(pos_counter, 1); // And put the starting position as well as the currenct length
//						System.out.println("First terminal within the <anychar> block");
					}
					else { // The HashMap already has elements
//						System.out.println("Following terminal within the <anychar> block");
						int length_of_anychar_string = result.get(pos_counter); // Get the value
						result.put(pos_counter, length_of_anychar_string + 1); // Increase the length by one
					}
				}
				else { // Should not happen
//					System.out.println("Terminal. The amount of currently visited anychar blocks is bigger then it should be"); 
					pos_counter++;
				}
			}
		}
		else if(pt.name.equals("<anychars>") || pt.name.equals("<anychar>") || pt.name.equals("<anycharsp>") || pt.name.equals("<anycharp>")) {
//			System.out.println("Anychar block");
			if(!anychar_seen_gpofa && currently_seen_anychars <= amount_of_seen_anychar) {
				setAnycharSeen(this, true);
//				System.out.println("Updated anychar_seen to true");
			}
			
		}
		else if(anychar_seen_gpofa) { // Should be true for the first tag after an anychar block
//			System.out.println("First block after an anychar block. Increased currently seen anychars by 1");
			currently_seen_anychars += 1; // Increase this counter here as we want to increase this counter when we finished one <anychar> block
			if(currently_seen_anychars > amount_of_seen_anychar) {
				return_break = true;
//				System.out.println("Set return break to true");
				if(result == null) { // So that we dont return null in case of an anychar block representing nothing 
					result = new HashMap<Integer, Integer>();
					result.put(-1, -1);
					
				}
				return result;
			}
//			System.out.println("Set AnycharSeen to false as we continue");
			setAnycharSeen(this, false);
		}
		for(ParseTree p : pt.children) {
			if(return_break) {
//				System.out.println("Return break");
				return result;
			}
			HashMap<Integer, Integer> tmp = _getMapOfPosStringAnychar(p, result, amount_of_seen_anychar);
			// TODO WE NEED TO CHECK HERE IF THE ANYCHAR BLOCKS ARE OVER, OTHERWISE WE DO HAVE A PROBLEM WHEN A TERMINAL SYMBOL FOLLOWS DIRECTLY ON AN ANYCHAR BLOCK
			if(tmp != null) {
				if(result == null) {
					result = tmp;
				}
				else {
					result.putAll(tmp);
				}
			}
			if(anychar_seen_gpofa && !((pt.name.equals("<anychars>") || pt.name.equals("<anychar>") || pt.name.equals("<anycharsp>") || pt.name.equals("<anycharp>")))) { // Should be true for the first tag after an anychar block
//				System.out.println("Reached first parent block not representing anychar. Increased currently seen anychars by 1");
				currently_seen_anychars += 1; // Increase this counter here as we want to increase this counter when we finished one <anychar> block
				if(currently_seen_anychars > amount_of_seen_anychar) {
					return_break = true;
//					System.out.println("Set return break to true");
					if(result == null) { // So that we dont return null in case of an anychar block representing nothing 
						result = new HashMap<Integer, Integer>();
						result.put(-1, -1);
						
					}
					return result;
				}
//				System.out.println("Set AnycharSeen to false as we continue");
				setAnycharSeen(this, false);
			}
		}
//		System.out.println("Return result");
		return result;
	}
	
	/*
	 * Returns the length of the string BEFORE hitting a 
	 * <anychar> rule in the tree
	 * 
	 * */
	// TODO Reset counters like pos_counter, etc for multiple use
	public HashMap<Integer, Integer> getMapOfPosStringAnychar() {
		setUpFunction();
//		System.out.println(this.tree_to_string());
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		int amount_of_seen_anychar = 0;
		HashMap<Integer, Integer> pos_length_mapping = _getMapOfPosStringAnychar(this, null, amount_of_seen_anychar);
		// Map.Entry<String, GDef> entry : master.entrySet()
		// int anycharsBlock = 10; // TODO, should return the amount of anychars block within the tree
		while(pos_length_mapping != null) {
			pos_counter = 0;
			currently_seen_anychars = 0;
			return_break = false;
			anychar_seen_gpofa = false;
			amount_of_seen_anychar += 1;
			int key = (Integer) pos_length_mapping.keySet().toArray()[0];
			int value = pos_length_mapping.get(key);
//			System.out.printf("Key: %d, Value: %d\n", key, value);
			if(!(key == -1 || value == -1)) {
				result.put(key, value);
			}
			pos_length_mapping = _getMapOfPosStringAnychar(this, null, amount_of_seen_anychar);
		}
		return result;
	}
	
    private void setPosCounter(ParseTree parseTree, int i) {
    	parseTree.pos_counter = i;
		for(ParseTree pt : parseTree.children) {
			setPosCounter(pt, i);
		}
		
	}

	private void setReturnBreak(ParseTree parseTree, boolean b) {
		parseTree.return_break = b;
		for(ParseTree pt : parseTree.children) {
			setReturnBreak(pt, b);
		}
		
	}

	private void setCurrentlySeenAnychars(ParseTree parseTree, int i) {
		parseTree.currently_seen_anychars = i;
		for(ParseTree pt : parseTree.children) {
			setCurrentlySeenAnychar(pt, i);
		}
		
	}

	private void setUpFunction() {
		setAnycharSeen(this, false);
		setCurrentlySeenAnychar(this, 0);
		setCurrentlySeenAnychars(this, 0);
		setReturnBreak(this, false);
		setPosCounter(this, 0);
	}
	private void setCurrentlySeenAnychar(ParseTree parseTree, int i) {
		parseTree.currently_seen_anychars = i;
		for(ParseTree pt : parseTree.children) {
			setCurrentlySeenAnychar(pt, i);
		}
		
	}
	private void setAnycharSeen(ParseTree master, boolean value) {
    	master.anychar_seen_gpofa = value;
		for(ParseTree pt : master.children) {
			setAnycharSeen(pt, value);
		}
		
	}
    public String print_tree_anychar_chars() {
    	return this._print_tree_anychar_chars(this, "", null);
    }
    
    /*
     * Returns the char sequence of the non terminals of <anychar> trees
     * 
     * */
    private String _print_tree_anychar_chars(ParseTree result, String tree, ParseTree parent) {
    	if(!(result.is_nt())) {
    		if(parent != null) {
    			if(parent.name.equals("<anychars>") || parent.name.equals("<anychar>") || parent.name.equals("<anycharsp>") || parent.name.equals("<anycharp>")) {
    				return result.name;
    			}
    		}
    	}
        for (ParseTree p : result.children) {
        	tree += p._print_tree_anychar_chars(p, "", result);
        }
        return tree;
    }
    
    
    /*
     * Used for HDD
     * Given a subtree and a target tree, this method will replace
     * the target tree with the subtree. With doing this, we can
     * easily extract the corresponding string to the new generated
     * tree. Note: Subtree has to be a real subtree of the targetTree
     * Returns a modified target tree
     * */
    public void replaceTreeNode(ParseTree biggest_node, ParseTree subtree_target, boolean log) {
    	// Call replaceTreeNode recursive as long as the given subtree matches the subtree of the targetTree
//    	System.out.printf("\nCurrent node (%d):\n%s\n", this.hashCode(), this.tree_to_string());
    	if(this.hashCode() == biggest_node.hashCode() && this.name.equals(biggest_node.name)) {
    		if(log) {
//    			System.out.println("Replaced " + this.name + ":\n" + this.tree_to_string());
    		}
    		this.children = subtree_target.children;
    		if(log) {
//    			System.out.println("Through\n" + subtree_target.tree_to_string());
    		}
    	}
    	for(ParseTree pt : this.children) {
    		pt.replaceTreeNode(biggest_node, subtree_target, log);
    	}
    }

    
    public void replaceTreeNodeUsingPath(ParseTree replacement, ArrayList<Integer> path, int path_counter) {
    	try {
    		if(path.size() == path_counter) {
    			// Replace tree
//    			System.out.printf("Replace tree:\n%s\nwith tree:\n%s\n", this, replacement);
    			this.children = replacement.children;
        	}
    		else if(path.size() > path_counter) {
    			int pos = path.get(path_counter);
        		this.children.get(pos).replaceTreeNodeUsingPath(replacement, path, path_counter + 1);
        	}
		} catch (Exception e) {
			System.out.println("Error replaceTreeNodeUsingPath: " + e.toString());
		}
    }
    
    public ParseTree getParseTreeForPath(ArrayList<Integer> path, int path_counter) {
    	// Given a path, this method will return the children at the position of the path
    	if(path.size() == path_counter) {
    		return this;
    	}
    	int pos = path.get(path_counter);
    	return this.children.get(pos).getParseTreeForPath(path, path_counter + 1);
    }
    
    
    
	public boolean isAbstracted() {
		return abstracted;
	}

	public void setAbstracted(boolean abstracted) {
		this.abstracted = abstracted;
	}
	public String tree_to_string_prune() {
		String s = "";
		if(this.name.startsWith("<") && this.name.endsWith(">")) {
			return this.name + ",";
		}
		for(ParseTree p : this.children) {
			s += p.tree_to_string_prune();
		}
		return s;
	}

	public List<Object> tree_to_string_any(ParseTree pt) {
		if(pt.name.contains("any")) {
			String s = pt.tree_to_string_prune();
			List<Object> obj = new ArrayList<Object>();
			obj.add(0, s.length());
			obj.add(1, String.format("<any %s>", s));
			return obj;
		}
		if(pt.name.startsWith("<") && pt.name.endsWith(">")) {
			List<Object> obj = new ArrayList<Object>();
			obj.add(0, 0);
			obj.add(1, pt.name);
			return obj;
		} else {
			List<Object> gs = tree_to_string_any_helper(pt);
			return gs;
			
		}
	}
	private List<Object> tree_to_string_any_helper(ParseTree pt) {
		List<Object> lst = new ArrayList<Object>();
		for(ParseTree p : pt.children) {
			lst = tree_to_string_any(p);
		}
		return lst;
	}

	public void getPathListForSymbols(ArrayList<Integer> path, int depth, HashMap<String, TreeMap<Integer, ArrayList<ArrayList<Integer>>>> lst) {
		if(lst.get(this.name) != null) { // Is there already a TreeMap within the HashMap?
			TreeMap<Integer, ArrayList<ArrayList<Integer>>> depth_path_lst_map = lst.get(this.name);
			if(depth_path_lst_map.get(depth) != null) { // Is there already a path entry for this depth? (needed for the correct sorting)
				// If this is the case, then we can easily add the current path to the list
				ArrayList<ArrayList<Integer>> dept_path_lst = depth_path_lst_map.get(depth);
				// Add the path
				dept_path_lst.add(path);
			} else {
				ArrayList<ArrayList<Integer>> tmp_lst = new ArrayList<ArrayList<Integer>>();
				tmp_lst.add(path);
				depth_path_lst_map.put(depth, tmp_lst);
			}
		} else {
			lst.put(this.name, new TreeMap<Integer, ArrayList<ArrayList<Integer>>>());
			TreeMap<Integer, ArrayList<ArrayList<Integer>>> depth_path_lst_map = lst.get(this.name);
			ArrayList<ArrayList<Integer>> tmp_lst = new ArrayList<ArrayList<Integer>>();
			tmp_lst.add(path);
			depth_path_lst_map.put(depth, tmp_lst);
		}
		for(int i = 0; i < this.children.size(); i++) {
			ArrayList<Integer> tmp_path = new ArrayList<Integer>();
			tmp_path.addAll(path);
			tmp_path.add(i);
			if(this.children.get(i).children.size() > 0 && this.is_nt() && !this.children.get(i).name.contains("any")) {
				this.children.get(i).getPathListForSymbols(tmp_path, depth + 1, lst);
			}
		}
	}

	public boolean pathReachable(ArrayList<Integer> curr_path, ArrayList<Integer> looped_path, int path_counter) {
		try {
			if(curr_path.get(path_counter) != looped_path.get(path_counter)) {
				return false;
			}
			if(curr_path.size() - 1 == path_counter) {
				if(this.children.size() > 0) { // Check if the subtree is a real children of the original tree
					return true;
				} else {
					return false;
				}
			}
			return this.children.get(curr_path.get(path_counter)).pathReachable(curr_path, looped_path, path_counter + 1);
		} catch (Exception e) {
			System.out.printf("pathReachable: %s\n", e.toString());
		}
		return false;
	}

	// Try to get the path for a specified subtree
	public ArrayList<Integer> getPathForNewMergedTree(ParseTree subtree, ArrayList<Integer> path) {
		if(this.children == subtree.children) {
			return path;
		}
		int i = 0;
		ArrayList<Integer> returned_path = null;
		for(ParseTree pt : this.children) {
			ArrayList<Integer> tmp_lst = new ArrayList<Integer>();
			tmp_lst.addAll(path);
			tmp_lst.add(i++);
			returned_path = pt.getPathForNewMergedTree(subtree, tmp_lst);
			if(returned_path != null) {
				return returned_path;
			}
		}
		return returned_path;
		
	}
	
	private ArrayList<Integer> representing_char = new ArrayList<Integer>();
	public int setRepresentingChar(int char_counter) {
		// String string_represented_by_tree = this.getTerminals(); // Return the terminals that this string is representing
		for(int i = 0; i < this.getTerminals().length(); i++) {
			this.representing_char.add(i + char_counter);
		}
		for(ParseTree pt : this.children) {
			if(!pt.is_nt()) { // If terminal, increase the char_counter as the child of pt would then be a character
				char_counter++;
			} else {
				char_counter = pt.setRepresentingChar(char_counter);
			}
		}
		return char_counter;
	}
	
    private String _dd_tree_with_representing_list(int indent, String tree) {
    	tree += "\t".repeat(indent) + this.name + "\t" + this.representing_char.toString();
        for (ParseTree p : this.children) {
            tree = p._dd_tree_with_representing_list(indent + 1, tree + "\n");
        }
        return tree;
    }

    public String dd_tree_with_representing_list() {
    	return this._dd_tree_with_representing_list(0, "");
    }

	public List<Object> removeTreesNotRepresentedByGivenArray(List<Integer> result, int deleted) {
		try {
//			System.out.printf("%s: %s, keep numbers: %s\n", this.name, this.representing_char, result);
			boolean delete_child = false;
			if(this.representing_char.size() == 1) { // Tree is only representing one character; should always be the case due to the structure of anychar
				if(!result.contains(this.representing_char.get(0))) { // If the character that is being represented by this tree
					// is not in the result, we will delete the children of the parent tree linking to this tree
					List<Object> obj = new ArrayList<Object>();
					obj.add(0, true); // delete the children
					obj.add(++deleted);
					return obj;
				}
			}
			ArrayList<Object> obj = null;
			ArrayList<Integer> child_pos_to_delete = new ArrayList<Integer>();
			for(int x = 0; x < this.children.size(); x++) {
				ParseTree pt = this.children.get(x);
				if(pt.is_nt()) { // We are not interested in the terminals
					ArrayList<Object> tmp_obj = (ArrayList<Object>) pt.removeTreesNotRepresentedByGivenArray(result, deleted);
					if(tmp_obj != null) {
						obj = tmp_obj;
					} else {
						continue;
					}
					if(obj != null) {
						if((boolean) obj.get(0)) {
							delete_child = true;
							deleted += (int) obj.get(1);
							child_pos_to_delete.add(x);
						}
					}
				}
			}
			if(delete_child) {
				if(child_pos_to_delete.size() == this.children.size()) { // If all childrens have to be deleted, just replace the the childrens with an empty list
//					System.out.printf("Delete all childrens for %s\n", this.name);
					this.children = new ArrayList<ParseTree>();
				} else { // Otherwise first "delete" the child, then rearrange the ParseTrees within the children list
					obj.set(0, false); // If there are childrens left in this node, do not delete the whole node
//					System.out.printf("Set delete to false for this tree");
					for(int pos : child_pos_to_delete) {
//						System.out.printf("Delete child %d:\n%s\n", pos, this.children.get(pos));
						this.children.remove(pos);
					}
					/*
					obj.set(0, false); // If there are childrens left in this node, do not delete the whole node
					int children_size = children.size();
					for(int j = 0; j < children_size; j++) {
						if(child_pos_to_delete.contains(j)) { // If child on position J in the children list should be removed
							if(!child_pos_to_delete.contains(j+1)) { // check if we can replace it with the next element
								for(int z = j + 1, y = j; z + 1 < children_size; z++, y++) { // Check if there exists a child on pos j+1 and shift any following child
									this.children.set(y, this.children.get(z));
								}
							}
						}
					}
					*/
				}
			}
			// Rearrange children list (if child 1 gets deleted and child 2 not, move child 2 to the position of child one
			return obj;
		} catch (Exception e) {
			System.out.printf("removeTreesNotRepresentedByArray: %s\n%s", this.name, e.toString());
			return null;
		}
		
	}

	// Checks if the given symbol is within the tree
	// If so, return true
	// Otherwise return false
	public boolean treeContainsSymbol(String symbol) {
		if(this.name.equals(symbol)) {
			return true;
		}
		for(ParseTree pt : this.children) {
			if(pt.treeContainsSymbol(symbol)) { // Otherwise this could be overridden by the next child in the tree
				return true;
			}
		}
		return false;
	}
	
	public String getAbstractedString(String s) {
		if(this.abstracted) {
			return s+= String.format("%s", this.name);
		}
		if(!this.is_nt()) {
			return s+= this.name;
		}
		for(ParseTree pt : this.children) {
			s += pt.getAbstractedString("");
		}
		return s;
	}
	
}

class ParseForest implements Iterable<ParseTree> {
    int cursor;
    List<State> states;
    public ParseForest(int cursor, List<State> states) {
        this.cursor = cursor;
        this.states = states;
    }

    @Override
    public Iterator<ParseTree> iterator() {
        // TODO
        return null;
    }

}

interface ParserI {
    ParseForest parse_prefix(String text, String start_symbol);
    ArrayList<ParseTree> parse(String text, String start_symbol) throws ParseException;
    Iterator<ParseTree> parse_check(String text, String start_symbol) throws ParseException;
}

abstract class Parser implements ParserI {
    String start_symbol;
    Grammar grammar;
    Parser(Grammar grammar) {
        this(grammar, "<start>");
    }
    Parser(Grammar grammar, String start_symbol) {
        this.start_symbol = start_symbol;
        this.grammar = new G(grammar).single_char_tokens();
        // we do not require a single rule for the start symbol
        int start_rules_len = grammar.get(start_symbol).size();
        if (start_rules_len != 1) {
            GRule gr = new GRule();
            gr.addAll(Arrays.asList(new String[]{this.start_symbol}));
            GDef gd = new GDef();
            gd.addAll(Arrays.asList(new GRule[]{gr}));
            this.grammar.put("<>", gd);
        }
    }

    public ArrayList<ParseTree> parse(String text) throws ParseException {
        return this.parse(text, this.start_symbol);
    }
    public Iterator<ParseTree> parse_check(String text) throws ParseException {
        return this.parse_check(text, this.start_symbol);
    }
    
}

class Item {
    String name;
    GRule expr;
    int dot;
    Item(String name, GRule expr, int dot) {
        this.name = name;
        this.expr = expr;
        this.dot = dot;
    }

    public boolean finished() {
        return this.dot >= this.expr.size();
    }

    Item advance() {
        return new Item(this.name, this.expr, this.dot + 1);
    }

    public String at_dot() {
        if (this.dot < this.expr.size()) {
            return this.expr.get(this.dot);
        } else {
            return "";
        }
    }

}

class State extends Item {
    public Column s_col;
    public Column e_col;
    State(String name, GRule expr, int dot, Column s_col, Column e_col) {
        super(name, expr, dot);
        this.s_col = s_col;
        this.e_col = e_col;
    }

    String _idx(Column v) {
        if (v == null) return "-1";
        return Integer.toString(v.index);
    }

    public String toString() {
        ArrayList<String> lst = new ArrayList<String>();
        for (int i = 0; i < this.expr.size(); i++) {
            if (i == this.dot) {
                lst.add("|");
            }
            lst.add(this.expr.get(i).toString());
        }
        if (this.dot == this.expr.size()) {
            lst.add("|");
        }
        //lst = [ str(p) for p in [*this.expr[:this.dot], '|', *this.expr[this.dot:]] ]
        return this.name + ":= " + String.join(" ", lst) + "(" + _idx(this.s_col) + "," + _idx(this.e_col) + ")";
    }

    State copy() {
        return new State(this.name, this.expr, this.dot, this.s_col, this.e_col);
    }

    State advance() {
        return new State(this.name, this.expr, this.dot + 1, this.s_col, null);
    }

    State back() {
        return new TState(this.name, this.expr, this.dot - 1, this.s_col, this.e_col);
    }

    String _t() {
        return (this.name + "," + this.expr.toString() + "," + this.dot + "," + this.s_col.index);
    }

    boolean equals(State s) {
        return this._t() == s._t();
    }
}

class TState extends State {
    public TState(String name, GRule expr, int dot, Column s_col, Column e_col) {
        super(name, expr, dot, s_col, e_col);
    }
    TState copy() {
        return new TState(this.name, this.expr, this.dot, this.s_col, this.e_col);
    }
}

class Column {
    int index;
    char letter;
    ArrayList<State> states = new ArrayList<State>();
    HashMap<String, State> unique = new HashMap<String, State>();
    HashMap<String, State> transitives = new HashMap<String, State>();

    Column(int index, char letter) {
        this.index = index;
        this.letter = letter;
    }

    public String toString() {
        ArrayList<String> finished_states = new ArrayList<String>();
        for (State s: this.states) {
            if (s.finished()) {
                finished_states.add(s.toString());
            }
        }
        String finished = String.join("\n", finished_states);
        return String.format("%s chart[%d]\n%s", this.letter, this.index, finished);
    }

    State add(State state) {
        String s_state = state.toString();
        if (this.unique.containsKey(s_state) ) {
            return this.unique.get(s_state);
        }
        this.unique.put(s_state, state);
        this.states.add(state);
        state.e_col = this;
        return this.unique.get(s_state);
    }
    State add_transitive(String key, State state) {
        // assert key not in self.transitives
        this.transitives.put(key, new TState(state.name, state.expr, state.dot, state.s_col, state.e_col));
        return this.transitives.get(key);
    }
}
class EarleyParser extends Parser {
    List<String> epsilon;
    List<Column> table;
    private boolean log = false;
    private String grammar_key = "<object>";
	private boolean coalesce_tokens = true;
	private HashSet<String> tokens = new HashSet<String>();
    
    public String get_grammar_key() {
    	return this.grammar_key;
    }
    public void set_grammar_key(String grammar_key) {
    	this.grammar_key = grammar_key;
    }
    
    public EarleyParser(Grammar grammar) {
        super(grammar);
        G g = new G(grammar);
        this.epsilon = g.nullable();
    }

    void predict(Column col, String sym, State state) {
        for (GRule alt : this.grammar.get(sym)) {
        	State new_state = new State(sym, alt, 0, col, null);
            col.add(new_state);
            if(this.log) {
            	System.out.println("Added " + new_state.toString());
            }
        }
        if (this.epsilon.contains(sym)) {
        	State new_state = state.advance();
            col.add(new_state);
        }
    }

    void scan(Column col, State state, char letter) {
        if (letter == col.letter) {
            State s = state.advance();
            col.add(s);
        }
    }

    void complete(Column col, State state) {
        this.earley_complete(col, state);
    }

    void earley_complete(Column col, State state) {
        List<State> parent_states = new ArrayList<State>();
        for (State st: state.s_col.states) {
            if (st.at_dot().equals(state.name)) {
                parent_states.add(st);
            }
        }
        for (State st : parent_states) {
            col.add(st.advance());
        }
    }

    List<Column> chart_parse(String letters, String start) {
        GRule alt = this.grammar.get(start).get(0);
        List<Column> chart = new ArrayList<Column>();
        chart.add(new Column(0, '\0'));
        for (int i = 1; i < (letters.length() + 1); i++) {
            char tok = letters.charAt(i - 1);
            chart.add(new Column(i, tok));
        }
        State s = new State(start, alt, 0, chart.get(0), null);
        chart.get(0).add(s);
        return this.fill_chart(chart);
    }

    List<Column> fill_chart(List<Column> chart) {
        for (int i = 0; i < chart.size(); i++) {
            Column col = chart.get(i);
                                                            
            // col.states get modified.
            int j = 0;
            while (j < col.states.size()) {
                //for (State state: col.states)
                State state = col.states.get(j++);
                if (state.finished()) {
                    this.complete(col, state);
                                                       
                } else {
                    String sym = state.at_dot();
                                                         
                    if (this.grammar.containsKey(sym)) {
                        this.predict(col, sym, state);
                    } else {
                        if (i + 1 >= chart.size()) {
                            continue;
                        }
                        int l = sym.length();
                        switch (l) {
                            case 1:
                                char c = sym.charAt(0);
                                this.scan(chart.get(i + 1), state, c);
                                break;
                            // case 0:
                            //    should not happen: state.finished() should be true
                            default:
                                throw new RuntimeException("Only single characters allowed");
                        }
                    }
                }
            }

            if (this.log) {
                out(col.toString() + "\n");
            }
        }
        return chart;
    }



    private void out(String var) {
        System.out.println(var);
    }

    @Override
    public ParseForest parse_prefix(String text, String start_symbol) {
        this.table = this.chart_parse(text, start_symbol); 
        List<State> states = new ArrayList<State>();
        for (int i = this.table.size(); i != 0; i--) {
            Column col = this.table.get(i-1); // Need to work on the elements within col (State st)
            for (State st : col.states) {
                if (st.name.equals(start_symbol)) {
                    states.add(st);
                }
            }
            if (states.size() != 0) {
                return new ParseForest(col.index, states);
            }
        }
        return new ParseForest(-1, states);
    }

    ArrayList<ArrayList<SK>> _paths(int frm, List<Column> chart, State state, int start, char k, GRule e) {
        if (e.isEmpty()) {
            ArrayList<ArrayList<SK>> sk = new ArrayList<ArrayList<SK>>();
            if (start == frm) {
                ArrayList<SK> sk1 = new ArrayList<SK>();
                sk1.add(new SK(state, k));
                sk.add(sk1);
                return sk;
            } else {
                return sk;
            }
        } else {
            ArrayList<ArrayList<SK>> sk = new ArrayList<ArrayList<SK>>();
            for (ArrayList<SK> r : this.parse_paths(e, chart, frm, start)) {
                ArrayList<SK> sk1 = new ArrayList<SK>();
                sk1.add(new SK(state, k));
                sk1.addAll(r);
                sk.add(sk1);
            }
            return sk;
        }
    }

    ArrayList<ArrayList<SK>> parse_paths(GRule named_expr, List<Column> chart, int frm, int til) {
        ArrayList<SIInfo> starts = new ArrayList<SIInfo>();
        String var = named_expr.get(named_expr.size()-1);
        GRule expr = new GRule();
        for (int i = 0; i < named_expr.size()-1; i++) {
            expr.add(named_expr.get(i));
        }
        //(GRule) named_expr.subList(0, named_expr.size() - 1);
        if (!this.grammar.containsKey(var)) {
            if (var.length() != 1) {
                throw new RuntimeException("Only single chars allowed.");
            }
            if (til > 0 && chart.get(til).letter == var.charAt(0)) {
                starts.add(new SIInfo(new State(var, new GRule(), 0, null, null), til - var.length(), 't'));
            }
        } else {
            for (State s: chart.get(til).states) {
                if (s.finished() && s.name.equals(var)) {
                    starts.add(new SIInfo(s, s.s_col.index, 'n'));
                }
            }
        }
        ArrayList<ArrayList<SK>> result = new ArrayList<ArrayList<SK>>();
        for (SIInfo sii : starts) {
            for (ArrayList<SK> pth : this._paths(frm, chart, sii.state, sii.index, sii.c, expr)) {
                result.add(pth);
            }
        }
        return result;
        //return [p for s, start, k in starts for p in _paths(frm, chart, s, start, k, named_expr.expr)];
    }

    NamedForest parse_forest(List<Column> chart, State state) {
        ArrayList<ArrayList<SK>> pathexprs = null;
        if (!state.expr.isEmpty()) {
            pathexprs = this.parse_paths(state.expr, chart, state.s_col.index, state.e_col.index);
        } else {
            pathexprs = new ArrayList<ArrayList<SK>>();
        }

        ArrayList<ArrayList<TPath>> paths = new ArrayList<ArrayList<TPath>>();
        for (ArrayList<SK> pathexpr: pathexprs) {
            ArrayList<TPath> tpath = new ArrayList<TPath>();
            for (int i = pathexpr.size(); i != 0; i--) {
                SK vk = pathexpr.get(i - 1);
                tpath.add(new TPath(vk, chart));
            }
            paths.add(tpath);
        }
        return new NamedForest(state.name, paths);
        /*return state.name, [[(v, k, chart) for v, k in reversed(pathexpr)]
                            for pathexpr in pathexprs];*/
    }

    NamedForest forest(State s, char kind, List<Column> chart) {
        if (kind == 'n') {
            return this.parse_forest(chart, s);
        } else {
             return new NamedForest(s.name, new ArrayList<ArrayList<TPath>>());
         }
    }

    
    ParseTree extract_a_tree(NamedForest forest_node, int pos) {
        if (forest_node.paths.isEmpty()) {
            return new ParseTree(forest_node.name, new ArrayList<ParseTree>());
        }

        ArrayList<ParseTree> lst = new ArrayList<ParseTree>();
        Random random = new Random();
        pos = random.nextInt(forest_node.paths.size());
        // System.out.printf("Paths: %d, rand: %d", forest_node.paths.size(), pos);
		for (TPath p : forest_node.paths.get(pos)) {
            lst.add(this.extract_a_tree(this.forest(p.sk.s, p.sk.k, p.chart), pos));
        }
	    //[self.extract_a_tree(self.forest(*p)) for p in paths[0]]
        return new ParseTree(forest_node.name, lst);
    }


    ArrayList<ParseTree> extract_trees(NamedForest forest) {
        ArrayList<ParseTree> pt = new ArrayList<ParseTree>();
        HashSet<String> h_pt = new HashSet<String>();
        for(int i = 0; i < forest.paths.size(); i++) {
        	for(int j = 0; j < 20; j++) {
        		// For loop to randomize the path traversal - Hotfix for the time being
        		ParseTree tmp = this.extract_a_tree(forest, i);
        		if(!h_pt.contains(tmp.tree_to_string())) {
        			h_pt.add(tmp.tree_to_string());
        			pt.add(tmp);
//        			System.out.println("Nr " + h_pt.size() + ":\n" + tmp.tree_to_string());
        		}
        	}
        }
        return pt;
    }
    
    ParseTree extract_a_tree_check(NamedForest forest_node) {
        if (forest_node.paths.isEmpty()) {
            return new ParseTree(forest_node.name, new ArrayList<ParseTree>());
        }

        ArrayList<ParseTree> lst = new ArrayList<ParseTree>();

		for (TPath p : forest_node.paths.get(0)) {
            lst.add(this.extract_a_tree_check(this.forest(p.sk.s, p.sk.k, p.chart)));
        }
        //[self.extract_a_tree(self.forest(*p)) for p in paths[0]]
        return new ParseTree(forest_node.name, lst);
    }


    Iterator<ParseTree> extract_trees_check(NamedForest forest) {
        ArrayList<ParseTree> pt = new ArrayList<ParseTree>();
        pt.add(this.extract_a_tree_check(forest));
        return pt.iterator();
    }
    
    /*
    ParseTree extract_a_tree(NamedForest forest_node) {
        if (forest_node.paths.isEmpty()) {
            return new ParseTree(forest_node.name, new ArrayList<ParseTree>());
        }

        ArrayList<ParseTree> lst = new ArrayList<ParseTree>();
        ArrayList<ArrayList<ParseTree>> tmp_list = new ArrayList<ArrayList<ParseTree>>();
        if(forest_node.paths.size() > 1) {
        	System.out.println("");
        }
		for (TPath p : forest_node.paths.get(0)) {
            lst.add(this.extract_a_tree(this.forest(p.sk.s, p.sk.k, p.chart)));
        }
	    //[self.extract_a_tree(self.forest(*p)) for p in paths[0]]
        return new ParseTree(forest_node.name, lst);
    }


    Iterator<ParseTree> extract_trees(NamedForest forest) {
        ArrayList<ParseTree> pt = new ArrayList<ParseTree>();
        pt.add(this.extract_a_tree(forest));
        return pt.iterator();
    }
    */
    
    public ArrayList<ParseTree> parse(String text,String start_symbol) throws ParseException {
        ParseForest p = this.parse_prefix(text, start_symbol);
        State start = null;
        for (State s : p.states) {
            if (s.finished()) {
                start = s;
            }
        }
        if (p.cursor < text.length() || (start == null)) {
        	// NamedForest forest = this.parse_forest(this.table, start);
            throw new ParseException("at " + p.cursor);
        }
        
        NamedForest forest = this.parse_forest(this.table, start);
        return this.extract_trees(forest);
    }
    
    public Iterator<ParseTree> parse_check(String text,String start_symbol) throws ParseException {
    ParseForest p = this.parse_prefix(text, start_symbol);
    State start = null;
    for (State s : p.states) {
        if (s.finished()) {
            start = s;
        }
    }
    if (p.cursor < text.length() || (start == null)) {
    	// NamedForest forest = this.parse_forest(this.table, start);
        throw new ParseException("at " + p.cursor);
    }
    
    NamedForest forest = this.parse_forest(this.table, start);
    return this.extract_trees_check(forest);
}
/*
    def extract_trees(self, forest_node):
        name, paths = forest_node
        if not paths:
            yield (name, [])
        results = []
        for path in paths:
            ptrees = [self.extract_trees(self.forest(*p)) for p in path]
            for p in zip(*ptrees):
                yield (name, p)
*/

	
	private ArrayList<ParseTree> coalesce(ArrayList<ParseTree> children) {
		StringBuilder last = new StringBuilder();
		ArrayList<ParseTree> new_lst = new ArrayList<ParseTree>();
		for(ParseTree pt : children) {
			if(!this.grammar.containsKey(pt.name)) {
				last.append(pt.name);
			} else {
				if(last.length() > 0) {
					new_lst.add(new ParseTree(last.toString(), new ArrayList<ParseTree>()));
					last = new StringBuilder();
				}
				new_lst.add(new ParseTree(pt.name, pt.children));
			}
		}
		if(last.length() > 0) {
			new_lst.add(new ParseTree(last.toString(), new ArrayList<ParseTree>()));
		}
		return new_lst;
	}
}

public class ParserLib {
    Grammar grammar;

    public Grammar loadGrammar(JSONObject json_grammar) {
        Grammar g = new Grammar();
        for (String key : json_grammar.keySet()) {
            JSONArray json_def = json_grammar.getJSONArray(key);
            GDef gd = new GDef();
            for (int i = 0; i < json_def.length(); i++) {
                JSONArray json_rule = json_def.getJSONArray(i);
                GRule gr = new GRule();
                for (int j = 0; j < json_rule.length(); j++) {
                    String token = json_rule.getString(j);
                    gr.add(token);
                }
                gd.add(gr);
            }
            g.put(key, gd);
        }
        return g;
    }

    public ParserLib(String grammar_file) throws IOException {
        Path path = FileSystems.getDefault().getPath(grammar_file);
        String content = Files.readString(path, StandardCharsets.UTF_8);
        JSONObject json_grammar = new JSONObject(content);
        grammar = this.loadGrammar(json_grammar);
    }

    public void _show_tree(ParseTree result, int indent) {
        System.out.println("   ".repeat(indent) + result.name);
        for (ParseTree p : result.children) {
            this._show_tree(p, indent + 1);
        }
    }

    public void show_tree(ParseTree result) {
        this._show_tree(result, 0);
    }
    
    public ArrayList<ParseTree> parse_string(String content, EarleyParser ep) throws ParseException, IOException {
    	// Equivalent to parse_text but without reading from a file; just parsing the fuzzed input
		// EarleyParser ep = new EarleyParser(this.grammar);
        ArrayList<ParseTree> result = ep.parse(content);
        return result;
    }
    
    public ParseTree check_string(String content, EarleyParser ep) throws ParseException, IOException {
    	// Equivalent to parse_text but without reading from a file; just parsing the fuzzed input
        Iterator<ParseTree> result = ep.parse_check(content);
        if (result.hasNext()) {
            return result.next();
        }
        return null;
    }
    
}

class ChoiceNode {
	private int counter;
	private ChoiceNode parent;
	private int total;
	private int chosen;
	private ChoiceNode next;
	
	@Override
	public String toString() {
		return String.format("counter: %d, chosen: %d, total: %d\n", counter, chosen, total);
	}
	
	public ChoiceNode(ChoiceNode parent, int total, int counter) {
		this.parent = parent;
		this.counter = counter;
		this.total = total;
		this.chosen = 0;
		this.next = null;
	}
	
	public int chosen() {
		assert !this.finished();
		return this.chosen;
	}

	public ChoiceNode increment() {
		this.next = null;
		if(this.parent == null) {
			return null;
		}
		this.setChosen(getChosen() + 1);
		if(this.finished()) {
			return this.parent.increment();
		}
		return this;
	}

	public boolean finished() {
		// TODO Auto-generated method stub
		return this.getChosen() >= this.getTotal();
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public ChoiceNode getParent() {
		return parent;
	}

	public void setParent(ChoiceNode parent) {
		this.parent = parent;
	}

	public void setNext(ChoiceNode next) {
		this.next = next;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getChosen() {
		return chosen;
	}

	public void setChosen(int chosen) {
		this.chosen = chosen;
	}

	public ChoiceNode getNext() {
		return next;
	}

}

class LazyExtractor{
	private EarleyParser parser;
	private String text; // input string
	private ChoiceNode choices;
	private NamedForest my_forest;
	private int choose_path_i;
	private int choose_path_l;
	int global_counter = 0;
	
	
	public LazyExtractor(EarleyParser parser, String text, ChoiceNode choices, int global_counter) throws ParseException {
		this.parser = parser;
		this.text = text;
		this.choices = choices;
		this.global_counter = global_counter;
		
		ParseForest forest = parser.parse_prefix(text, "<start>");
	    State start = null;
        for (State s : forest.states) {
            if (s.finished()) {
                start = s;
            }
        }
	    if (forest.cursor < text.length() || (start == null)) {
	    	// NamedForest forest = this.parse_forest(this.table, start);
	        throw new ParseException("at " + forest.cursor);
	    }
	    this.my_forest = parser.parse_forest(parser.table, start);
	}
	
	public List<Object> extract_a_tree(int counter) {
		List<Object> obj = new ArrayList<Object>();
		// choices = new ChoiceNode(null, 1, 0);
		LEReturnTriple ler = null;
		while(!this.choices.finished()) {
			HashSet<LETriple> seen = new HashSet<LETriple>();
			ler = extract_a_node(this.my_forest, seen, this.choices, null);
			if(ler.getParsetree() != null) {
				counter++;
//				if(counter % 200 == 0) {
//					// System.out.printf("Tree[%d]:\n%s\n", counter, ler.getParsetree().tree_to_string());
//					System.out.printf("Tree[%d]:\n", counter);
//				}
//				 System.out.printf("Tree[%d]:\n%s\n", counter, ler.getParsetree().tree_to_string());
			}
			// ChoiceNode c = ler.getChoicenode();
			if(ler.getPostree() != null) {
				obj.add(0, ler.getParsetree());
				obj.add(1, ler.getChoicenode());
				obj.add(2, counter);
				return obj;
			} else {
				 ler.getChoicenode().increment();
				 // System.out.printf("%s\n", c);
			}
			
		}
		return null;
	}

	private LEReturnTriple extract_a_node(NamedForest forest_node, HashSet<LETriple> seen, ChoiceNode choices, LETriple new_seen_element) {
		if(new_seen_element != null) {
			seen.add(new_seen_element);
		}
//		System.out.printf("\n----------\nseen: %s\nchoices: %s\n", seen.toString(), choices.toString());
		String name = forest_node.name;
		ArrayList<ArrayList<TPath>> paths = forest_node.paths;
		if(paths.size() == 0) {
			LETriple let = new LETriple(name, 0, 1);
			LEReturnTriple ler = new LEReturnTriple(new Postree(let, new ArrayList<Postree>()), new ParseTree(name, new ArrayList<ParseTree>()), choices);
			return ler;
		}
		ArrayList<ParseTree> child_nodes;
		ArrayList<Postree> pos_nodes;
		int i;
		int l;
		while(true) {
			ChoiceNode new_choices = this.choose_path(paths, choices);
//			System.out.printf("choose_path; global counter: %d: %s\n", this.global_counter, new_choices);
			i = this.choose_path_i;
			l = this.choose_path_l;
			ArrayList<TPath> curr_path = paths.get(i);
			if(curr_path == null) {
				LEReturnTriple ler = new LEReturnTriple(null, null, new_choices);
				return ler;	
			}
			child_nodes = new ArrayList<ParseTree>();
			pos_nodes = new ArrayList<Postree>();
			for(int j = 0; j < curr_path.size(); j++) {
				List<Column> chart = curr_path.get(j).chart;
				Character kind = curr_path.get(j).sk.k;
				State s = curr_path.get(j).sk.s;
				LETriple nid;
				if(s.s_col == null || s.e_col == null) {
					nid = new LETriple(s.name, -1, -1);
				} else {
					nid = new LETriple(s.name, s.s_col.index, s.e_col.index);
				}
				// System.out.printf("nid (%d): %s\n", nid.hashCode(), nid.toString());
//				for(LETriple x : seen) {
//					System.out.printf("Elem (%d): %s\nnid equals elem: %s\n", x.hashCode(), x.toString(), nid.equals(x));
//				}
//				System.out.print("");
				if(seen.contains(nid)) {
					LEReturnTriple ler = new LEReturnTriple(null, null, new_choices);
					return ler;
				}
				NamedForest f = this.parser.forest(s, kind, chart);
//				System.out.printf("seen: %s\nnid: %s\n", seen, nid);
				@SuppressWarnings("unchecked")
				LEReturnTriple result = extract_a_node(f, (HashSet<LETriple>) seen.clone(), new_choices, nid);
//				System.out.printf("%s", result);
				ChoiceNode newer_choices = result.getChoicenode();
				if(result.getPostree() == null) {
					LEReturnTriple ler = new LEReturnTriple(null, null, newer_choices);
					return ler;
				}
				child_nodes.add(result.getParsetree());
				pos_nodes.add(result.getPostree());
				new_choices = newer_choices;
			}
			choices = new_choices;
			if(pos_nodes.size() == 0) {
				LEReturnTriple ler = new LEReturnTriple(null, null, choices);
				return ler;
			} else {
				break;
			}
		}
		LETriple let = new LETriple(name, i, l);
		LEReturnTriple ler = new LEReturnTriple(new Postree(let, pos_nodes), new ParseTree(name, child_nodes), choices);
		return ler;
		
	}
	
	private ChoiceNode choose_path(ArrayList<ArrayList<TPath>> paths, ChoiceNode choices) {
		int i = 0;
		this.choose_path_l = paths.size();
		if(choices.getNext() != null) {
			if(choices.getNext().finished()) {
				paths = null;
				return choices.getNext();
			}
			i = choices.getNext().chosen();
			
		} else {
			choices.setNext(new ChoiceNode(choices, this.choose_path_l, global_counter));
			global_counter++;
			i = choices.getNext().chosen();
		}
		this.choose_path_i = i;
		choices = choices.getNext();
		return choices;
	}
}
class LETriple{ // Lazy extractor tuple for set seen
	private String name;
	private int s_col_index;
	private int e_col_index;
	
	public LETriple(String name, int s_col_index, int e_col_index) {
		this.name = name;
		this.s_col_index = s_col_index;
		this.e_col_index = e_col_index;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(!(obj instanceof LETriple)) {
			return false;
		}
		LETriple le = (LETriple) obj;
		return this.name.equals(le.name) && this.s_col_index == le.s_col_index && this.e_col_index == le.e_col_index;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode() + this.e_col_index + this.s_col_index;
	}
	
	@Override
	public String toString() {
		return String.format("('%s', %d, %d)", name, s_col_index, e_col_index);
	}
}
class LEReturnTriple{
	private Postree postree;
	private ParseTree parsetree;
	private ChoiceNode choicenode;
	
	public LEReturnTriple(Postree postree, ParseTree parsetree, ChoiceNode choicenode) {
		this.postree = postree;
		this.parsetree = parsetree;
		this.choicenode = choicenode;
	}

	public Postree getPostree() {
		return postree;
	}

	public void setPostree(Postree postree) {
		this.postree = postree;
	}

	public ParseTree getParsetree() {
		return parsetree;
	}

	public void setParsetree(ParseTree parsetree) {
		this.parsetree = parsetree;
	}

	public ChoiceNode getChoicenode() {
		return choicenode;
	}

	public void setChoicenode(ChoiceNode choicenode) {
		this.choicenode = choicenode;
	}
	
	@Override
	public String toString() {
		return String.format("postree: %s\nparsetree: %s\nnewer choices: %s\n", postree.toString(), parsetree.tree_to_string_line(), choicenode);
	}
	
}

class Postree{
	private LETriple let;
	private ArrayList<Postree> lst;
	
	public Postree(LETriple let, ArrayList<Postree> lst) {
		this.let = let;
		this.lst = lst;
	}

	public LETriple getLet() {
		return let;
	}

	public void setLet(LETriple let) {
		this.let = let;
	}

	public ArrayList<Postree> getLst() {
		return lst;
	}

	public void setLst(ArrayList<Postree> lst) {
		this.lst = lst;
	}

	@Override
	public String toString() {
		return _toString("");
	}
	private String _toString(String s) {
		s += this.let.toString();
		for(Postree pos : this.lst) {
			s += "[" + pos._toString("") + "]";
		}
		if(this.lst.size() == 0) {
			s += ", []";
		}
		return s;
	}
}