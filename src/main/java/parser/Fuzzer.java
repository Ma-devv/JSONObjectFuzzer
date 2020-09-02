package parser;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;



public class Fuzzer {
	private String rv; // returnValue from validate_json
	private int n; // The index from validate_json
	private Character c; // The character from validate_json
	private String grammar; // Grammar for the ParserLib
	private String json_file; // Not necessary anymore; only for test purposes 
	private String current_str = ""; // String that will be validated
	private String previous_str = ""; // String that has been validated
	private Character generatedChar = null; // The character that has been generated and added to the previous_str
	private Map<String, ArrayList<String>> valid_strings = new HashMap<String, ArrayList<String>>(); // Dictionary for valid strings {(error, [valid string list]),(), ...}
	private ArrayList<Character> char_set = new ArrayList<Character>(); // Generated 
	private Set<Integer> special_char = new HashSet<Integer>(); // special characters 
	
	// Error handling
	private final String text_must_begin_with_array = "A JSONArray text must begin with";
	private final String text_must_end_with_array = "A JSONArray text must end with";
	
	private final String text_must_begin_with = "A JSONObject text must begin with";
	private final String text_must_end_with = "A JSONObject text must end with";
	private final String expected_a = "Expected";
	private final String unterminated_string = "Unterminated string";
	private final String missing_value = "Missing value";
	private final String illegal_escape = "Illegal escape";
	private final String duplicate_key = "Duplicate key";
	
	// States if the given input_str (current_str) was "wrong", "incomplete" or "complete"
	private final String wrong = "wrong";
	private final String incomplete = "incomplete";
	private final String complete = "complete";
	
	private String parsed_data_type = ""; // Will be set during fuzzing; either OBJECT for JSON object or ARRAY for JSON array
	
	// For Hierarchical Delta Debugging
	private List<Column> curr_table; // Saves the current table that parse() is generating
	private List<Column> old_table; // Saves the last table that parse() has been generated
	private ParserLib golden_grammar_PL;
	private EarleyParser golden_grammar_EP;
	private HashMap<String, GDef> adjusted_rule = new HashMap<String, GDef>();
	private ParserLib curr_pl;
	private EarleyParser curr_ep;
	private ParserLib old_pl;
	private EarleyParser old_ep;
	private String tree_key;
	private Grammar anychar_grammar;
	private ArrayList<ParsedStringSettings> listForEasiestMod = new ArrayList<ParsedStringSettings>();
	
	// Simple DDSET
	private HashMap<String, ArrayList<ParseTree>> dd_random_trees = new HashMap<String, ArrayList<ParseTree>>();
	private HashSet<ParseTree> dd_random_trees_set_check = new HashSet<ParseTree>();
	
	private boolean log = false;
	private final int MAX_INPUT_LENGTH = 100; // Sets the maximal input length when creating a string
	private HashSet<String> exclude_grammars = new HashSet<>(Arrays.asList("<anychar>", "<anychars>", "<anycharsp>", "<anycharp>"));
	
	
	// TODO 
	/* - change printable to all characters
	 * - adjust the simpleddset similar to hdd and check the simpleddset
	 * - maybe i should declare a few methods as static
	 * - problem wiht <elements> when using random tree generation 
	 * */
	public static void main(String[] args) {
		Fuzzer fuzzer = new Fuzzer("", 0, null, args[0], null); // Create new Fuzzer; initialize grammar
		try {
			fuzzer.create_valid_strings(4, fuzzer.log); // Create 20 valid strings; no log level enabled
			// Print out the strings that have been found
			for(Map.Entry<String, ArrayList<String>> entry : fuzzer.getValid_strings().entrySet()) {
				String key = entry.getKey();
				ArrayList<String> value = entry.getValue();
				System.out.println("Found " + value.size() + " inputs for the exception " + key);
				for(String s : value) {
					System.out.println("Valid String: " + s + "\n");
				}
			}
		} catch (Exception e) {
			System.out.println("Something went wrong...\nError: " + e);
			System.exit(1);
		}
		
	}

	// Constructor
	public Fuzzer(String rv, int n, Character c, String grammar, String json_file) {
		super();
		this.rv = rv;
		this.n = n;
		this.c = c;
		this.grammar = grammar;
		this.json_file = json_file;
		// Define special characters
		int[] tmp_special_char = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		for(int i : tmp_special_char) {
			this.special_char.add(i);
		}
	}
	public void create_valid_strings(int n, boolean log_level) {
		/* 
		 * Creates n strings that are valid JSON according to 
		 * org.json.JSONObject but are rejected by the golden
		 * grammar (implemented by the ParserLib)
		 * 
		 * */
		int i = 0;
		// JSONArray jsonarray = new JSONArray();
		Date start_program = new Date();
		while (true) {
			Date start_iteration = new Date();
			initializeGoldenGrammar();
			String created_string = generate(log_level); // Generate a new valid JSON Object, according to org.json.JSONObject
			if(created_string != null) {
				// Check if the created string is also valid according to the "golden grammar"
//				created_string = "{ea 92HPPf&c.:$PL}";
//				created_string = "[b),QL2\b]";
//	        	Fuzzer.parseStringUsingLazyExtractor(created_string, this.getCurr_ep(), 2000);
		        if(checkIfStringCanBeParsedWithGivenGrammar(this.getCurr_ep(), created_string)) {
		        	continue;
		        } else { 
		        	// The string has not been parsed successfully
		        	// Hence the string is valid for org.json.JSONObject 
		        	// but is not according to the golden grammar
		            // Change the golden grammar until the string can be parsed
		        	i++;
		        	System.out.println("Created string [" + (i) + "]: " + created_string);
		        	change_everything_except_anychar_one_after_another(created_string);
		        	Date end_iteration = new Date();
		        	long difference_iteration = end_iteration.getTime() - start_iteration.getTime();
		        	System.out.printf("Time that was needed for the processing of %s: %ds\n", created_string, difference_iteration / 1000);
		        	// jsonarray.put(created_string);
		        	// System.out.println("\n");
		        }
				if(i >= n) { // Did we create enough valid strings?
					Date end_program = new Date();
					long difference_iteration = end_program.getTime() - start_program.getTime();
		        	System.out.printf("Time that was needed for the program to run: %ds\n", difference_iteration / 1000);
					break;
				}
			}
		}
		// System.out.printf("%s", jsonarray);
		// System.exit(0);
		// Simple DDSET
//		SimpleDDSET sddset = new SimpleDDSET();		
//		
//   		// Sort the list
//		Collections.sort(this.getListForEasiestMod(), new ParsedStringSettingsComparator());
//		for(ParsedStringSettings pss : this.getListForEasiestMod()) {
//			sddset.abstractTree(pss, getExclude_grammars(), this.getGolden_grammar_EP());
//		}
	}
	
	public static boolean checkIfStringCanBeParsedWithGivenGrammar(EarleyParser ep, String input_string) {
		// Checks if the given string can be parsed with the given grammar of the EarleyParser
		try {
			ChoiceNode choices = new ChoiceNode(null, 1, 0);
			new LazyExtractor(ep, input_string, choices, 1);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	private void initializeGoldenGrammar() {
		try {
			if(this.log) {
				System.out.println("\n\n------------New run------------");					
			}
			this.setCurr_pl(new ParserLib(this.getGrammar()));
			this.setCurr_ep(new EarleyParser(this.getCurr_pl().grammar));
			this.setGolden_grammar_PL(this.getCurr_pl());
			this.setGolden_grammar_EP(this.getCurr_ep());
		} catch (Exception e1) {
			System.out.println("Something went wrong... " + e1.toString());
			System.exit(1);
		}
	}

	private void change_everything_except_anychar_one_after_another(String created_string) {
		/*
    	 * For every rule within the grammar, loop over the entries within the rule and replace each non terminal one at a time
    	 */
		int counter = 0;
    	HashMap<String, GDef> master = this.getCurr_pl().grammar; // First rule is unchanged and acts as master grammar
    	for(Map.Entry<String, GDef> entry : master.entrySet()) { //
    		counter++;
    		String state = entry.getKey().toString();
    		Date date = new Date();
    		System.out.println("\n\n----STATE " + counter + "/" + master.size() + ": " + state + " starting at " + new Timestamp(date.getTime()) + "----");
    		
    		if(this.getExclude_grammars().contains(state)) {
    			if(this.log) {
    				System.out.println("Skip " + state + " rule");
    			}
    			continue;
    		}
    		for (int gRuleC = 0; gRuleC < entry.getValue().size(); gRuleC++) {
    			if(this.log) {
    				System.out.println("\nRule: " + entry.getValue().get(gRuleC).toString() + ", Size: " + entry.getValue().get(gRuleC).size());    				
    			}
				for(int elemC = 0; elemC < entry.getValue().get(gRuleC).size(); elemC++) {
					try {
						if(state.equals("<sp1>")) {
							System.out.printf("");
						}
						// get(0) = ParseTree
						// get(1) = list for anychar deletion
                		List<Object> lst_obj = getBestParseTreeForAdjustedState(state, master, gRuleC, elemC, created_string);
                		if(lst_obj != null) {
                			ParseTree best_tree = (ParseTree) lst_obj.get(0);
                			// int length_of_anychar_chars = (int) lst_obj.get(1); UNUSED ATM
							@SuppressWarnings("unchecked")
							TreeMap<Integer, Integer> sorted_pos_length_lst = (TreeMap<Integer, Integer>) lst_obj.get(2);
                			ParsedStringSettings pss = createPssForBestTree(best_tree, sorted_pos_length_lst, created_string, state, master, gRuleC, elemC, entry, null);
                			if(pss != null) {
                	            HDD hdd = new HDD();
                	            hdd.startHDD(pss, this.getExclude_grammars(), this.getGolden_grammar_PL(), this.getGolden_grammar_EP(), this.log);
                	            MinimizeAnychar ma = new MinimizeAnychar();
                	            ma.startDD(pss, getGolden_grammar_EP(), getGolden_grammar_PL(), this.getCurr_ep());
                	            SimpleDDSET sddset = new SimpleDDSET();
                	            sddset.abstractTree(pss, getExclude_grammars(), this.getGolden_grammar_EP());
                	            if(pss.getAbstracted_string().contains("<") && pss.getAbstracted_string().contains(">")) { // Was the abstraction successful?
                	            	addMinimalInputToList(pss);
                	            }
                    		}
                    		else {
                    			if(this.log) {
                    				System.out.println("Continue with the next returned ParseTree or with the adjustment of the next rule");
                    			}
                    		}
                			
                		}
    				} catch (Exception e) {
    					System.out.println("change_everything_except_anychar_one_after_another: " + e.toString());
    					
    				}
				}
			}

			// Add <anycharsp> to the rule and try to parse it again
			GRule anycharsp = new GRule();
			anycharsp.add("<anycharsp>");
			if(this.log) {
				System.out.println("\nAdd <anycharsp> to the set of rules of " + state);
			}
			try {
        		List<Object> lst_obj = getBestParseTreeForAdjustedState(state, created_string, anycharsp);
        		if(lst_obj != null) {
        			ParseTree best_tree = (ParseTree) lst_obj.get(0);
        			// int length_of_anychar_chars = (int) lst_obj.get(1); // UNUSED ATM
					@SuppressWarnings("unchecked")
					TreeMap<Integer, Integer> sorted_pos_length_lst = (TreeMap<Integer, Integer>) lst_obj.get(2);
        			ParsedStringSettings pss = createPssForBestTree(best_tree, sorted_pos_length_lst, created_string, state, master, -1, -1, entry, anycharsp);
        			if(pss != null) {
        	            HDD hdd = new HDD();
        	            hdd.startHDD(pss, this.getExclude_grammars(), this.getGolden_grammar_PL(), this.getGolden_grammar_EP(), this.log);
        	            MinimizeAnychar ma = new MinimizeAnychar();
        	            ma.startDD(pss, getGolden_grammar_EP(), getGolden_grammar_PL(), this.getCurr_ep()); 	
        	            SimpleDDSET sddset = new SimpleDDSET();
        	            sddset.abstractTree(pss, getExclude_grammars(), this.getGolden_grammar_EP());
//        	            System.out.printf(""
//        	            		+ "ID: %d\n"
//        	            		+ "Generated string: %s\n"
//        	            		+ "HDD string: %s\n"
//        	            		+ "DD string: %s\n"
//        	            		+ "Abstracted string: %s\n", 
//        	            		pss.hashCode(),
//        	            		pss.getCreated_string(),
//        	            		pss.getHdd_string(),
//        	            		pss.getDd_string(),
//        	            		pss.getAbstracted_tree().getAbstractedString("")
//        	            		);
        	            if(pss.getAbstracted_string().contains("<") && pss.getAbstracted_string().contains(">")) { // Was the abstraction successful?
        	            	addMinimalInputToList(pss);
        	            }
            		}
             		else {
            			if(this.log) {
            				System.out.println("Continue with the next returned ParseTree or with the adjustment of the next rule");
            			}
            		}
        		}
			} catch (Exception e) {
				System.out.println("change_everything_except_anychar_one_after_another, anycharsp: " + e.toString());
			}
    		
    	}
    	if(true) {
    		System.out.println("\n\nDone with adjusting the grammar for " + created_string + "\n\n");
		}
    	for(ParsedStringSettings pss : this.getListForEasiestMod()) {
            System.out.printf(""
            		+ "ID: %d\n"
            		+ "Generated string: %s\n"
            		+ "HDD string: %s\n"
            		+ "DD string: %s\n"
            		+ "Abstracted string: %s\n", 
            		pss.hashCode(),
            		pss.getCreated_string(),
            		pss.getHdd_string(),
            		pss.getDd_string(),
            		pss.getAbstracted_tree().getAbstractedString("")
            		);
    	}
//    	System.exit(0);
	}
	
	private ParsedStringSettings createPssForBestTree(ParseTree pt, TreeMap<Integer, Integer> sorted_pos_length_lst, String created_string, String state, HashMap<String, GDef> master, int gRuleC, int elemC, Entry<String, GDef> entry, GRule anycharsp) {
//		System.out.printf("Original string: %s\tLength %d\n", created_string, created_string.length());
		StringBuilder sb_created_string = new StringBuilder(created_string);
		int anychars_length;
		int pos;
		int removed_chars = 0;
		StringBuilder removed_chars_from_string = new StringBuilder("");
//		HashMap<Integer, Integer> pos_length_lst = pt.getMapOfPosStringAnychar();
//		Map<Integer, Integer> sorted_pos_length_lst = new TreeMap<Integer, Integer>(pos_length_lst);
		int counter = 0;
		for(Map.Entry<Integer, Integer> pos_length : sorted_pos_length_lst.entrySet()) {
			pos = pos_length.getKey();
			pos -= removed_chars; // We take the length of characters, that have already been removed into account
			anychars_length = pos_length.getValue();

			for(int i = 0; i < anychars_length; i++) { // Can not use normal delete as there a strange behavior when start = end (then nothing will be deleted))
				try {
					removed_chars_from_string.append(sb_created_string.charAt(pos));
					sb_created_string.deleteCharAt(pos); // Delete the character at position POS anychars_length-times

					counter += 1;
				} catch (Exception e) {
					System.out.printf("Exception: %s\nPos: %d, Removed chars: %d, anychars_length: %d\nremoved_chars_from_string: %s\nsb_created_string: %s",
							e.toString(), pos, removed_chars, anychars_length, removed_chars_from_string.toString(), sb_created_string.toString());
				}
				
			}
			removed_chars += anychars_length;
		}
		if(this.log) {
			System.out.println(String.format("String representing through <anychar>: %s\tLength %d\n"
					+ "String after removing those characters: %s\n", 
					removed_chars_from_string, removed_chars_from_string.length(), sb_created_string));
		}
		if(!(gRuleC == -1 && elemC == -1)) {
			return new ParsedStringSettings(
            		created_string,
            		sb_created_string.toString(),
            		"",
            		"",
            		"",
            		pt.count_nodes(0, this.getExclude_grammars()),
            		pt.count_leafes(),
            		state, 
            		entry.getValue().get(gRuleC), 
            		entry.getValue().get(gRuleC).get(elemC),
            		new ParseTree(pt),
            		pt, 
            		null,
            		null,
            		this.getCurr_pl(),
            		this.getParsed_data_type());
		}
		else { // <Anycharsp>
			return new ParsedStringSettings(
	            		created_string,
	            		sb_created_string.toString(),
	            		"",
	            		"",
	            		"",
	            		pt.count_nodes(0, this.getExclude_grammars()),
	            		pt.count_leafes(),
	            		state, 
	            		null, 
	            		"ADDED RULE <ANYCHARSP>",
	            		new ParseTree(pt),
	            		pt,
	            		null,
	            		null,
	            		this.getCurr_pl(),
	            		this.getParsed_data_type());
		}
	}

	private void addMinimalInputToList(ParsedStringSettings pss) {
		if(pss != null) {
			this.getListForEasiestMod().add(pss);
		}
	}
	
	public static List<Object> parseStringUsingLazyExtractor(String created_string, EarleyParser ep, int max_rounds) {
		List<Object> result_lst = new ArrayList<Object>();
		ChoiceNode choices = new ChoiceNode(null, 1, 0);
		int counter = 1;
		LazyExtractor le = null;
		while(true) {
			try {
				if(le == null) {
					le = new LazyExtractor(ep, created_string, choices, 1);
				} else {
					le = new LazyExtractor(ep, created_string, choices, le.global_counter);
				}
				List<Object> lst = le.extract_a_tree(counter);
				ParseTree pt = (ParseTree) lst.get(0);
				ChoiceNode last_choice = (ChoiceNode) lst.get(1);
				counter = (int) lst.get(2);
				if(last_choice == null) {
					break;
				}
				// System.out.printf("Tree: %s\n", pt.tree_to_string());
				String s = pt.getTerminals();
				// List<Object> obj = pt.tree_to_string_any(pt);
				HashMap<Integer, Integer> anychar_pos = pt.getMapOfPosStringAnychar();
				Map<Integer, Integer> sorted_pos_length_lst = new TreeMap<Integer, Integer>(anychar_pos); // For the return value;
				int len_of_anychar_chars = 0;
				for(Map.Entry<Integer, Integer> key_value : anychar_pos.entrySet()) {
					len_of_anychar_chars += key_value.getValue();
				}
				
				if(result_lst.size() == 0) { // No elements within the return list
					result_lst.add(0, pt);
					result_lst.add(1, len_of_anychar_chars);
					result_lst.add(2, sorted_pos_length_lst);
				} else { // Already an element within the list
					if(len_of_anychar_chars < (int) result_lst.get(1)) { // New ParseTree is better and has less anychar characters
						result_lst.set(0, pt);
						result_lst.set(1, len_of_anychar_chars);
						result_lst.set(2, sorted_pos_length_lst);
					}
				}
				assert s == created_string;
				ChoiceNode v = last_choice.increment();
				if(v == null) {
					break;
				}
				if(counter > max_rounds) {
					break;
				}
			} catch (Exception e) {
				return null;
			}
		}
		return result_lst;
	}
	
	/*
	 * Checks the adjusted Grammar
	 * Returns the best ParseTree (less amounts of characters that
	 * are being represented using <anychar> blocks)
	 * Otherwise null
	 * */
	private List<Object> getBestParseTreeForAdjustedState(String state, HashMap<String, GDef> master, int gRuleC, int elemC, String created_string) {
		try {
			ParserLib pl_adjusted = null;
			EarleyParser ep_adjusted = null;
			pl_adjusted = new ParserLib(grammar);
			// Replace the rule with <anychars>
			pl_adjusted.grammar.get(state).get(gRuleC).set(elemC, "<anychars>");
			if(this.log) {
				System.out.printf("Adjusted %s[%d][%d]: %s => %s\n", 
						state, gRuleC, elemC, master.get(state).get(gRuleC).get(elemC), pl_adjusted.grammar.get(state).get(gRuleC).get(elemC));
			}
			ep_adjusted = new EarleyParser(pl_adjusted.grammar);
			this.setCurr_pl(pl_adjusted);
			this.setCurr_ep(ep_adjusted);
			return parseStringUsingLazyExtractor(created_string, this.getCurr_ep(), 100);
		} catch (Exception e) {
			if(this.log) {
				System.out.println("Failed to parse the string using the adjusted grammar; error: " + e.toString());
			}
			return null;
		}
	
	}

	/*
	 * Checks the adjusted Grammar
	 * Returns the best ParseTree (less amounts of characters that
	 * are being represented using <anychar> blocks)
	 * Otherwise null
	 * */
	private List<Object> getBestParseTreeForAdjustedState(String state, String created_string, GRule anycharsp) {
		try {
			// Load the original master grammar
        	ParserLib pl_adjusted = null;
    		EarleyParser ep_adjusted = null;
			pl_adjusted = new ParserLib(grammar);
			
			// Replace the rule with <anychars>
			pl_adjusted.grammar.get(state).add(anycharsp);
			if(this.log) {
				System.out.printf("Added <anycharsp> to %s\n", state);
			}
			ep_adjusted = new EarleyParser(pl_adjusted.grammar);
			this.setCurr_pl(pl_adjusted);
    		this.setCurr_ep(ep_adjusted);
			return parseStringUsingLazyExtractor(created_string, this.getCurr_ep(), 100);
		} catch (Exception e) {
  			if(this.log) {
				System.out.println("Failed to parse the string using the adjusted grammar; error: " + e.toString());
			}
			return null;
		}
	
	}

	private String generate(boolean log_level) {
		/*
		 * Feed it one character at a time, and see if the parser rejects it. 
    	 * If it does not, then append one more character and continue. 
    	 * If it rejects, replace with another character in the set. 
     	 * :returns completed string
		 * */
		// Reset everything
		refill_character_list(log_level); // Refill the character list
		setPrevious_str("");
		setN(0);
		setC(null);
		setRv("");
		while(true) {
			char character = get_next_char(log_level); // Get a new character from the list "char_set" 
			setGeneratedChar(character);
			setCurrent_str(getPrevious_str() + character); // Concatenate the previous string with the generated character
			// Check if the string is about to become an array (starting with '[') or an object (starting with '{')
			if(getCurrent_str().startsWith("[")) {
				this.setParsed_data_type("ARRAY");
				validate_json_array(getCurrent_str(), log_level); // Try to validate the current string (current_str)
			} else {
				this.setParsed_data_type("OBJECT");
				validate_json_object(getCurrent_str(), log_level); // Try to validate the current string (current_str)
			}
			// System.exit(0);
			if(log_level) {
				System.out.format("%s n=%d, c=%s. Input string was %s\n", rv, n, c, getCurrent_str());
			}
			if(getRv().equals(complete)) { // Return if complete
				return getCurrent_str();
			}
			else if(getCurrent_str().length() >= MAX_INPUT_LENGTH) { // Unlikely that a string with a size of >MAX_INPUT_LENGTH characters will end in a valid string
				// => reset 
				setCurrent_str(""); //TODO Code duplication => create a new method
				setPrevious_str("");
				setN(0);
				setC(null);
				setRv("");
				refill_character_list(log_level);
				continue;
			}
			else if(getRv().equals(incomplete)) { // The current string was incomplete; something is missing
				setPrevious_str(getCurrent_str());
				refill_character_list(log_level);
				continue;
			}
			else if(getRv().equals(wrong)) { // The current string was wrong; don't concatenate the generated character to the previous string
				continue;
			}
			else {
				System.out.println("ERROR");
				break;
			}
		}
		return null;
	}

	private void validate_json_object(String input_str, boolean log_level) {
		/* Tries to validate the given input_str; handles error accordingly
		 * sets:
		 * rv: "complete", "incomplete" or "wrong", 
		 * n: the index of the character -1 if not applicable
		 * c: the character where error happened  "" if not applicable
		 * */
		try {
			// input_str = "";
			if(log_level) {
				System.out.println("Try input: " + input_str);	
			}
			JSONObject obj = new JSONObject(input_str); // Try to parse the input_str
			if(log_level) {
				System.out.println("Found valid input: " + input_str);	
			}
			setRv(complete); // Set the return value to complete
		} catch (Exception e) {
			// System.out.printf("Error: %s\n", e.toString());
			String msg = e.toString();			
			ArrayList<Integer> numbers = getNumbersFromErrorMessage(msg);
			if(msg.contains(text_must_begin_with)) { // Should never be the case after the program flow adjustment
				rv = "wrong";
				if(input_str.equals("")) {
					c = null;
				}
				else if(special_char.contains((int) getGeneratedChar())) { // Check if the last generated character is a special character
					n = numbers.get(0)-2; // 
					c = input_str.charAt(n);
				}
				else {
					n = numbers.get(0)-1;
					c = input_str.charAt(n);
				}
				
				
			}
			else if (msg.contains(text_must_end_with) || msg.contains(text_must_end_with_array)) {
				n = numbers.get(0) - 1;
				if(getGeneratedChar() == 0) {
					n++;
				}
				rv = "incomplete";
				c = null;
			}
			else if (msg.contains(expected_a)) {
				n = numbers.get(0)-1;
				if(n >= input_str.length()) {
					rv = incomplete;
					c = null;
				}
				else {
					rv = wrong;
					c = input_str.charAt(n);
				}
				
			}
			else if (msg.contains(unterminated_string)) {
				n = -1;
				rv = incomplete;
				c = null;
				
			}
			else if (msg.contains(missing_value)) {
				n = numbers.get(0);
				if(n >= input_str.length()) {
					rv = incomplete;
					c = null;
				}
				else {
					rv = wrong;
					c = input_str.charAt(n);
				}
				
			}
			else if (msg.contains(illegal_escape)) {
				n = -1;
				rv = wrong;
				c = null; 
				
			}
			else if (msg.contains(duplicate_key)) {
				n = -1;
				rv = wrong;
				c = null;
				
			}
			
			else {
				// Something went wrong
				System.out.println("Unknown error; exit program: " + msg);
				System.exit(0);
			}
		}
	}
	
	private void validate_json_array(String input_str, boolean log_level) {
		/* Tries to validate the given input_str; handles error accordingly
		 * sets:
		 * rv: "complete", "incomplete" or "wrong", 
		 * n: the index of the character -1 if not applicable
		 * c: the character where error happened  "" if not applicable
		 * */
		try {
			// input_str = "";
			if(log_level) {
				System.out.println("Try input: " + input_str);	
			}
			JSONArray obj = new JSONArray(input_str); // Try to parse the input_str
			if(log_level) {
				System.out.println("Found valid input: " + input_str);	
			}
			setRv(complete); // Set the return value to complete
		} catch (Exception e) {
			// System.out.printf("Error: %s\n", e.toString());
			String msg = e.toString();			
			ArrayList<Integer> numbers = getNumbersFromErrorMessage(msg);
			if(msg.contains(text_must_begin_with_array)) {
				rv = "wrong";
				if(input_str.equals("")) {
					c = null;
				}
				else if(special_char.contains((int) getGeneratedChar())) { // Check if the last generated character is a special character
					n = numbers.get(0)-2; // 
					c = input_str.charAt(n);
				}
				else {
					n = numbers.get(0)-1;
					c = input_str.charAt(n);
				}
				
				
			}
			else if (msg.contains(text_must_end_with_array) || msg.contains(text_must_end_with)) {
				n = numbers.get(0) - 1;
				if(getGeneratedChar() == 0) {
					n++;
				}
				rv = "incomplete";
				c = null;
			}
			else if (msg.contains(expected_a)) {
				n = numbers.get(0)-1;
				if(n >= input_str.length()) {
					rv = incomplete;
					c = null;
				}
				else {
					rv = wrong;
					c = input_str.charAt(n);
				}
				
			}
			else if (msg.contains(unterminated_string)) {
				n = -1;
				rv = incomplete;
				c = null;
				
			}
			else if (msg.contains(missing_value)) {
				n = numbers.get(0);
				if(n >= input_str.length()) {
					rv = incomplete;
					c = null;
				}
				else {
					rv = wrong;
					c = input_str.charAt(n);
				}
				
			}
			else if (msg.contains(illegal_escape)) {
				n = -1;
				rv = wrong;
				c = null; 
				
			}
			else if (msg.contains(duplicate_key)) {
				n = -1;
				rv = wrong;
				c = null;
				
			}
			
			else {
				// Something went wrong
				System.out.println("Unknown error; exit program: " + msg);
				System.exit(0);
			}
		}
	}
	private ArrayList<Integer> getNumbersFromErrorMessage(String msg) {
		/*
		 * Given a String getNumberFromErrorMessage will 
		 * remove any character except numbers (0-9) and 
		 * return a list of numbers that are left in the list 
		 * */
		ArrayList<Integer> result = new ArrayList<Integer>();
		msg = msg.replaceAll("[^\\d]", " "); // Replace and character except from numbers with a space
		String[] splitted = msg.split(" ");
		for(String s : splitted) {
			if(!s.equals("")) {
				result.add(Integer.parseInt(s));
			}
		}		
		return result;
	}
	
	
	private void refill_character_list(boolean log_level) {
		/*
		 * Refills the char_set with all ASCII characters
		 * (depending on which printable is used)
		 * */
		ArrayList<Character> tmp_char_set = new ArrayList<Character>();
		char[] set_of_chars = printable_with_special_characters();
		// char[] set_of_chars = printable();
		for(char c : set_of_chars) {
			tmp_char_set.add(c);
		}
		setChar_set(tmp_char_set);
	}

	private char get_next_char(boolean log_level) {
		/*
		 * Returns a random character from char_set
		 * and removes the returned character from the 
		 * list. This has the advantage that if 
		 * validate_json returns "wrong" the same 
		 * character can not be returned multiple times 
		 * from get_next_char
		 * */
		ArrayList<Character> set_of_chars = getChar_set();
		int idx = ThreadLocalRandom.current().nextInt(0, set_of_chars.size()); // Get a random character between 0 and the size of the set
		// Random rand = new Random();
		// int idx = rand.nextInt(set_of_chars.size());
		char input_char = set_of_chars.get(idx); // Set the return value to the character
		getChar_set().remove(idx); // And remove the character from the list
		if(log_level) {
			System.out.println("\nnext character: " + input_char);
		}
		return input_char;
	}
	
    public static char[] printable(){ 
    	/*
    	 * Returns a list of ASCII characters without 
    	 * special characters
    	 * */ 
        char[] result = new char[95]; 
        for(int i = 32; i<=126; i++){
            result[i-32] = (char) i;
        }
        return result;
    }
    
    public static char[] printable_with_special_characters(){
    	/*
    	 * Returns a list of ASCII characters with
    	 * special characters
    	 * */
    	char[] result = new char[128]; 
        for(int i = 0; i<=127; i++){
        	// Exclude special characters json can not handle (00, 0D, 0A)
        	if (!(i == 0 || i == 13 || i == 10)) { // TODO Limitation
        		result[i] = (char) i;
        	}
        }
        return result;
    }
    
    
    // Getter and Setters
    
	public Character getGeneratedChar() {
		return generatedChar;
	}
	public void setGeneratedChar(Character generatedChar) {
		this.generatedChar = generatedChar;
	}
	public String getRv() {
		return rv;
	}
	public void setRv(String rv) {
		this.rv = rv;
	}
	public int getN() {
		return n;
	}
	public void setN(int n) {
		this.n = n;
	}
	public Character getC() {
		return c;
	}
	public void setC(Character c) {
		this.c = c;
	}

	public String getGrammar() {
		return grammar;
	}
	public void setGrammar(String grammar) {
		this.grammar = grammar;
	}
	public String getJson_file() {
		return json_file;
	}
	public void setJson_file(String json_file) {
		this.json_file = json_file;
	}
	public String getCurrent_str() {
		return current_str;
	}
	public void setCurrent_str(String current_str) {
		this.current_str = current_str;
	}
	public String getPrevious_str() {
		return previous_str;
	}
	public void setPrevious_str(String previous_str) {
		this.previous_str = previous_str;
	}
	public String getText_must_begin_with() {
		return text_must_begin_with;
	}
	public ArrayList<Character> getChar_set() {
		return char_set;
	}
	public void setChar_set(ArrayList<Character> char_set) {
		this.char_set = char_set;
	}
	public Map<String, ArrayList<String>> getValid_strings() {
		return valid_strings;
	}
	public void setValid_strings(Map<String, ArrayList<String>> valid_strings) {
		this.valid_strings = valid_strings;
	}	
	public List<Column> getCurrTable() {
		return curr_table;
	}
	public void setCurrTable(List<Column> table) {
		this.curr_table = table;
	}
	public List<Column> getOldTable() {
		return old_table;
	}
	public void setOldTable(List<Column> table) {
		this.old_table = table;
	}
	public HashMap<String, GDef> getAdjusted_rule() {
		return adjusted_rule;
	}
	public void setAdjusted_rule(HashMap<String, GDef> adjusted_rule) {
		this.adjusted_rule = adjusted_rule;
	}

	public ParserLib getCurr_pl() {
		return curr_pl;
	}

	public void setCurr_pl(ParserLib curr_pl) {
		this.curr_pl = curr_pl;
	}

	public EarleyParser getCurr_ep() {
		return curr_ep;
	}

	public void setCurr_ep(EarleyParser curr_ep) {
		this.curr_ep = curr_ep;
	}

	public ParserLib getOld_pl() {
		return old_pl;
	}

	public void setOld_pl(ParserLib old_pl) {
		this.old_pl = old_pl;
	}

	public EarleyParser getOld_ep() {
		return old_ep;
	}

	public void setOld_ep(EarleyParser old_ep) {
		this.old_ep = old_ep;
	}

	public String getTree_key() {
		return tree_key;
	}

	public void setTree_key(String tree_key) {
		this.tree_key = tree_key;
	}

	public Grammar getAnychar_grammar() {
		return anychar_grammar;
	}

	public void setAnychar_grammar(Grammar anychar_grammar) {
		this.anychar_grammar = anychar_grammar;
	}
	public ArrayList<ParsedStringSettings> getListForEasiestMod() {
		return listForEasiestMod;
	}

	public void setListForEasiestMod(ArrayList<ParsedStringSettings> listForEasiestMod) {
		this.listForEasiestMod = listForEasiestMod;
	}
	public HashSet<String> getExclude_grammars() {
		return exclude_grammars;
	}
	public void setExclude_grammars(HashSet<String> exclude_grammars) {
		this.exclude_grammars = exclude_grammars;
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
	public String getParsed_data_type() {
		return parsed_data_type;
	}
	public void setParsed_data_type(String parsed_data_type) {
		this.parsed_data_type = parsed_data_type;
	}
	public HashMap<String, ArrayList<ParseTree>> getDd_random_trees() {
		return dd_random_trees;
	}
	public void setDd_random_trees(HashMap<String, ArrayList<ParseTree>> dd_random_trees) {
		this.dd_random_trees = dd_random_trees;
	}
	public HashSet<ParseTree> getDd_random_trees_set_check() {
		return dd_random_trees_set_check;
	}
	public void setDd_random_trees_set_check(HashSet<ParseTree> dd_random_trees_set_check) {
		this.dd_random_trees_set_check = dd_random_trees_set_check;
	}
}
