package parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.json.*;

import jdk.internal.org.jline.terminal.Terminal;

import java.io.IOException;
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

class ParseTree {
    String name;
    ArrayList<ParseTree> children;
    private boolean indented_nt = false; // TODO change - howto?
    private boolean anychar_seen_gpofa = false; // TODO change GPOFA: getPosOfFristAnychar()
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
    	tree += "   ".repeat(indent) + this.name;
        for (ParseTree p : this.children) {
            tree = p._tree_to_string(indent + 1, tree + "\n");
        }
        return tree;
    }

    public String tree_to_string() {
    	return this._tree_to_string(0, "");
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
	public HashMap<Integer, Integer> getMapOfPosStringAnychar() {
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
    public void replaceTreeNode(ParseTree biggest_node, ParseTree subtree_target) {
    	// Call replaceTreeNode recursive as long as the given subtree matches the subtree of the targetTree
//    	System.out.printf("\nCurrent node (%d):\n%s\n", this.hashCode(), this.tree_to_string());
    	if(this.hashCode() == biggest_node.hashCode() && this.name.equals(biggest_node.name)) {
//    		System.out.println("Replaced " + this.name + ":\n" + this.tree_to_string());
    		this.children = subtree_target.children;
//    		System.out.println("Through\n" + subtree_target.tree_to_string());
    	}
    	for(ParseTree pt : this.children) {
    		pt.replaceTreeNode(biggest_node, subtree_target);
    	}
    }

	@Override
	public int hashCode() {
		// We need to adjust the hashCode() as we need something to compare 
		// two objects during HDD (equals not possible due to deep copy)
		return this.count_leafes() + this.getTerminals().hashCode();
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
}

/*
class LeoParser(EarleyParser):
    def complete(self, col, state):
        return self.leo_complete(col, state)

    def leo_complete(self, col, state):
        detred = self.deterministic_reduction(state)
        if detred:
            col.add(detred.copy())
        else:
            self.earley_complete(col, state)

    def deterministic_reduction(self, state):
        raise NotImplemented()

    def uniq_postdot(self, st_A):
        col_s1 = st_A.s_col
        parent_states = [
            s for s in col_s1.states if s.expr and s.at_dot() == st_A.name
        ]
        if len(parent_states) > 1:
            return None
        matching_st_B = [s for s in parent_states if s.dot == len(s.expr) - 1]
        return matching_st_B[0] if matching_st_B else None

    def get_top(self, state_A):
        st_B_inc = self.uniq_postdot(state_A)
        if not st_B_inc:
            return None

        t_name = st_B_inc.name
        if t_name in st_B_inc.e_col.transitives:
            return st_B_inc.e_col.transitives[t_name]

        st_B = st_B_inc.advance()

        top = self.get_top(st_B) or st_B
        return st_B_inc.e_col.add_transitive(t_name, top)

    def deterministic_reduction(self, state):
        return self.get_top(state)

    def __init__(self, grammar, **kwargs):
        super().__init__(grammar, **kwargs)
        self._postdots = {}

    def uniq_postdot(self, st_A):
        col_s1 = st_A.s_col
        parent_states = [
            s for s in col_s1.states if s.expr and s.at_dot() == st_A.name
        ]
        if len(parent_states) > 1:
            return None
        matching_st_B = [s for s in parent_states if s.dot == len(s.expr) - 1]
        if matching_st_B:
            self._postdots[matching_st_B[0]._t()] = st_A
            return matching_st_B[0]
        return None

    def expand_tstate(self, state, e):
        if state._t() not in self._postdots:
            return
        c_C = self._postdots[state._t()]
        e.add(c_C.advance())
        self.expand_tstate(c_C.back(), e)

    def rearrange(self, table):
        f_table = [Column(c.index, c.letter) for c in table]
        for col in table:
            for s in col.states:
                f_table[s.s_col.index].states.append(s)
        return f_table

    def parse(self, text, start_symbol):
        cursor, states = self.parse_prefix(text, start_symbol)
        start = next((s for s in states if s.finished()), None)
        if cursor < len(text) or not start:
            raise SyntaxError("at " + repr(text[cursor:]))

        self.r_table = self.rearrange(self.table)
        forest = self.extract_trees(self.parse_forest(self.table, start))
        for tree in forest:
            yield tree

    def parse_forest(self, chart, state):
        if isinstance(state, TState):
            self.expand_tstate(state.back(), state.e_col)

        return super().parse_forest(chart, state)
*/

/*
class IterativeEarleyParser(LeoParser):
    def parse_paths(self, named_expr_, chart, frm, til_):
        return_paths = []
        path_build_stack = [(named_expr_, til_, [])]

        def iter_paths(path_prefix, path, start, k, e):
            x = path_prefix + [(path, k)]
            if not e:
                return_paths.extend([x] if start == frm else [])
            else:
                path_build_stack.append((e, start, x))

        while path_build_stack:
            named_expr, til, path_prefix = path_build_stack.pop()
            *expr, var = named_expr

            starts = None
            if var not in self.grammar:
                starts = ([(var, til - len(var),
                        't')] if til > 0 and chart[til].letter == var else [])
            else:
                starts = [(s, s.s_col.index, 'n') for s in chart[til].states
                      if s.finished() and s.name == var]

            for s, start, k in starts:
                iter_paths(path_prefix, s, start, k, expr)

        return return_paths

    def choose_a_node_to_explore(self, node_paths, level_count):
        first, *rest = node_paths
        return first

    def extract_a_tree(self, forest_node_):
        start_node = (forest_node_[0], [])
        tree_build_stack = [(forest_node_, start_node[-1], 0)]

        while tree_build_stack:
            forest_node, tree, level_count = tree_build_stack.pop()
            name, paths = forest_node

            if not paths:
                tree.append((name, []))
            else:
                new_tree = []
                current_node = self.choose_a_node_to_explore(paths, level_count)
                for p in reversed(current_node):
                    new_forest_node = self.forest(*p)
                    tree_build_stack.append((new_forest_node, new_tree, level_count + 1))
                tree.append((name, new_tree))

        return start_node

    def extract_trees(self, forest):
        yield self.extract_a_tree(forest)
*/
/*
    test_cases = [
        (A1_GRAMMAR, '1-2-3+4-5'),
        # (A2_GRAMMAR, '1+2'),
        # (A3_GRAMMAR, '1+2+3-6=6-1-2-3'),
        # (LR_GRAMMAR, 'aaaaa'),
        # (RR_GRAMMAR, 'aa'),
        # (DIRECTLY_SELF_REFERRING, 'select a from a'),
        # (INDIRECTLY_SELF_REFERRING, 'select a from a'),
        # (RECURSION_GRAMMAR, 'AA'),
        # (RECURSION_GRAMMAR, 'AAaaaa'),
        # (RECURSION_GRAMMAR, 'BBccbb')
    ]

    for i, (grammar, text) in enumerate(test_cases):
        print(i, text)
        tree, *_ =  IterativeEarleyParser(grammar, canonical=True).parse(text)
        assert text == tree_to_string(tree)
*/

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
                    
     
       
        
      
    
              
       
   
        
    
     
