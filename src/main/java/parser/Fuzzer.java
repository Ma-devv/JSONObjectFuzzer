package parser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
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
	
	private Map<String, ArrayList<String>> errors = new HashMap<String, ArrayList<String>>(); // create a dictionary for the errors
	
	// Error handling
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
	private Set<String> already_adjusted = new HashSet<String>();
	private Grammar anychar_grammar;
	private ArrayList<ParsedStringSettings> listForEasiestMod = new ArrayList<ParsedStringSettings>();
	
	private boolean log = false;
	private final int MAX_INPUT_LENGTH = 100; // Sets the maximal input length when creating a string
	private HashSet<String> exclude_grammars = new HashSet<>(Arrays.asList("<anychar>", "<anychars>", "<anycharsp>", "<anycharp>"));
	
	public static void main(String[] args) {		
		Fuzzer fuzzer = new Fuzzer("", 0, null, args[0], null); // Create new Fuzzer; initialize grammar
		try {
			fuzzer.create_valid_strings(50, fuzzer.log); // Create 20 valid strings; no log level enabled
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
		while (true) {
			initializeGoldenGrammar();
			String created_string = generate(log_level); // Generate a new valid JSON Object, according to org.json.JSONObject
			if(created_string != null) {
				// Check if the created string is also valid according to the "golden grammar"
		        try {
		            // created_string = "{H}";
		        	// created_string = "{7w\f\b	:e	,A- 'm \f:y 	\b}";
//		        	created_string = "{\"abcd\": [{\"pqrs\":}]}";
		        	created_string = "{\"ab\": [{\"cd\": {\"ef\":}}]}";
		        	// created_string = "\"abcd\"";
		        	System.out.println("\nCreated string [" + (i + 1) + "]: " + created_string);
	        		for(ParseTree pt : this.getCurr_pl().parse_string(created_string, this.getCurr_ep())) {
	        			if(pt != null) {
	        				// Parsed successfully using the golden grammar
	        				// There is no need to loop over the rest of the loop as we have one valid result
	        				if(this.log) {
	        					System.out.println("String " + created_string + " successfully parsed using the original golden grammar... Continuing");
	        					System.out.println("Tree: \n");
	        					this.getCurr_pl().show_tree(pt);
	        				}
	        				// Break and generate a new string
	        				break;
	        			}
	        		}
		            // ParseTree result = this.getCurr_pl().parse_string(created_string, this.getCurr_ep()); // Try to parse the string
		            // this.getCurr_pl().show_tree(result);
		            // this.setTree_key(this.getCurr_pl().save_tree(result, 0));
		            // The string has been parsed successfully and thus we just continue...
		            // System.exit(0);
		        } catch (Exception e) { 
		        	// The string has not been parsed successfully
		        	// Hence the string is valid for org.json.JSONObject 
		        	// but is not according to the golden grammar
		            // Change the golden grammar until the string can be parsed
		        	change_everything_except_anychar_one_after_another(created_string);
		        	// Now we try to get the smallest input that is causing the error/exception
		        	// TODO
		        	System.out.println("\n");
		        	// System.exit(0);
		        }
				if(i >= n) { // Did we create enough valid strings?
					break;
				}
			}
		}
	}
	
	
	
	
	private void initializeGoldenGrammar() {
		try {
			if(this.log) {
				System.out.println("\n\n------------New run------------");					
			}
//			if(this.log) {
//				System.out.println("Import ParserLib grammar");
//			}
			this.setCurr_pl(new ParserLib(this.getGrammar()));
//			if(this.log) {
//				System.out.println("ParserLib grammar successfully imported in " + difference / 1000 + " seconds");
//			}
//			if(this.log) {
//				System.out.println("Create EarleyParser with ParserLib grammar");
//			}
			this.setCurr_ep(new EarleyParser(this.getCurr_pl().grammar));
//			if(this.log) {
//				System.out.println("Successfully created EarleyParser with ParserLib grammar in " + difference / 1000 + " seconds");
//			}
			// Set golden grammar ParserLib as well as EarleyParser
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
    	HashMap<String, GDef> master = this.getCurr_pl().grammar; // First rule is unchanged and acts as master grammar
    	for(Map.Entry<String, GDef> entry : master.entrySet()) { // 
    		String state = entry.getKey().toString();
    		if(this.log) {
    			System.out.println("\n\n----STATE: " + state + "----");
    		}
    		
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
						// Try to parse the string using the adjusted golden grammar. If successful, try to get (multiple) ParseTrees
                		ArrayList<ParseTree> result_lst = getMultipleParseTreesUsingAdjustedGrammar(state, master, gRuleC, elemC, created_string);
                		// ParseTree result = this.getCurr_pl().parse_string(created_string, this.getCurr_ep()); // Try to parse the string
                		if(result_lst != null) {
                    		for(ParseTree result : result_lst) {
                        		if(result != null) {
                        			ParsedStringSettings pss = identifiyAndRemoveAnycharChars(result, created_string, state, master, gRuleC, elemC, entry, null);
                        			if(pss != null) {
                        				// System.out.println(pss.toString());
                        	            HDD hdd = new HDD();
                        	            ParsedStringSettings minimal_input_for_rule = hdd.startHDD(pss, this.getExclude_grammars(), this.getGolden_grammar_PL(), this.getGolden_grammar_EP());
                        	            if(minimal_input_for_rule == null) {
                        	            	System.out.println("Could not minimize the string " + pss.getCreated_string() + " any further.");
                        	            }
                        	            addMinimalInputToList(pss);
                        			}
                        			else {
                            			if(this.log) {
                            				System.out.println("Returned ParsedStringSettings is null. Continue with the next returned ParseTree or with the adjustment of the next rule");
                            			}
                        			}
                        		}
                        		else {
                        			if(this.log) {
                        				System.out.println("Continue with the next returned ParseTree or with the adjustment of the next rule");
                        			}
                        		}
                    		}
                		}
    				} catch (Exception e) {
    					if(this.log) {
    						System.out.println("change_everything_except_anychar_one_after_another: " + e.toString());
    					}
    					
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
        		ArrayList<ParseTree> result_lst = getMultipleParseTreesUsingAdjustedGrammar(state, created_string, anycharsp);
        		if(result_lst != null) {
            		for(ParseTree result : result_lst) {
            			if(result != null) {
                			// ParseTree result = this.getCurr_pl().parse_string(created_string, this.getCurr_ep()); // Try to parse the string
                    		ParsedStringSettings pss = identifiyAndRemoveAnycharChars(result, created_string, state, master, -1, -1, entry, anycharsp);
                    		if(pss != null) {
                	            HDD hdd = new HDD();
                	            ParsedStringSettings minimal_input_for_rule = hdd.startHDD(pss, this.getExclude_grammars(), this.getGolden_grammar_PL(), this.getGolden_grammar_EP());
                	            if(minimal_input_for_rule == null) {
                	            	System.out.println("Could not minimize the string " + pss.getCreated_string() + " any further.");
                	            }
                	            addMinimalInputToList(pss);
                    		}
                    		else {
                    			if(this.log) {
                    				System.out.println("Returned ParsedStringSettings is null. Continue with the next returned ParseTree or with the adjustment of the next rule");
                    			}
                    		}
                		}
                 		else {
                			if(this.log) {
                				System.out.println("Continue with the next returned ParseTree or with the adjustment of the next rule");
                			}
                		}
            		}
        		}
			} catch (Exception e) {
				
			}
    		
    	}
    	if(this.log) {
    		System.out.println("\n\nDone with adjusting the grammar for " + created_string + "\n\n");
		}
    	if(true) {
    		// Sort the list
    		Collections.sort(this.getListForEasiestMod(), new ParsedStringSettingsComparator());
    		for(ParsedStringSettings pss : this.getListForEasiestMod()) {
    			System.out.println(pss);
    		}
    	}
    	System.exit(0);
	}
	
	private void addMinimalInputToList(ParsedStringSettings pss) {
		if(pss != null) {
			this.getListForEasiestMod().add(pss);
		}
	}

	/*
	 * Checks if the given string can be parsed using the adjusted golden grammar
	 * Returns true if the string was parsed successfully using the adjusted golden grammar
	 * Otherwise false
	 * */
	private ParseTree checkStringWithAdjustedGrammar(String created_string, ParserLib pl, EarleyParser ep) {
		try {
			if(this.log) {
				System.out.printf("Check if we can parse %s successfully using the adjusted golden grammar\n", created_string);
			}
			ParseTree pt = pl.check_string(created_string, ep);
			if(pt != null) {
				return pt;
			}
		} catch (Exception e) {
  			if(this.log) {
				System.out.println("Failed to parse the string using the adjusted grammar");
			}
			return null;
		}
		return null;
	}
	
	/*
	 * Checks the adjusted Grammar for adding <anycharsp>
	 * Returns a ParseTree if the string could be parsed successfully
	 * Otherwise null
	 * */
	private ArrayList<ParseTree> getMultipleParseTreesUsingAdjustedGrammar(String state, String created_string, GRule anycharsp) {
		ArrayList<ParseTree> pt_list = new ArrayList<ParseTree>();
		try {
			// Load the original master grammar
        	ParserLib pl_adjusted = null;
    		EarleyParser ep_adjusted = null;
    		Date d_start = new Date();
//    		if(this.log) {
//    			System.out.println("Import ParserLib grammar");
//			}
			pl_adjusted = new ParserLib(grammar);
			Date d_end = new Date();
			long difference = d_end.getTime() - d_start.getTime();
//			if(this.log) {
//				System.out.println("ParserLib grammar successfully imported in " + difference / 1000 + " seconds");
//			}
			
			// Replace the rule with <anychars>
			pl_adjusted.grammar.get(state).add(anycharsp);
			if(this.log) {
				System.out.printf("Added <anycharsp> to %s\n", state);
			}
			
			d_start = new Date();
//			if(this.log) {
//				System.out.println("Create EarleyParser with ParserLib grammar");
//			}
			ep_adjusted = new EarleyParser(pl_adjusted.grammar);
			d_end = new Date();
			difference = d_end.getTime() - d_start.getTime();
    		this.setCurr_pl(pl_adjusted);
    		this.setCurr_ep(ep_adjusted);
			// ParseTree pt = this.getCurr_pl().parse_string(created_string, this.getCurr_ep());
			// Loop over the returned ArrayList containing ParseTrees that have been created using the adjusted golden grammar
			ArrayList<ParseTree> lst = this.getCurr_pl().parse_string(created_string, this.getCurr_ep());
//			System.out.printf("Created %d different ParseTrees for %s", lst.size(), created_string);
    		for(ParseTree pt : lst) {
    			if(pt != null) {
    				// Parsed successfully using the adjusted grammar
    	    		if(this.log) {
    	    			System.out.println("String " + created_string + " successfully parsed using the adjusted golden grammar");
    				}
    	    		// Add to return list
    	    		pt_list.add(pt);
    			}
    		}
        	return pt_list;
		} catch (Exception e) {
  			if(this.log) {
				System.out.println("Failed to parse the string using the adjusted grammar");
			}
			return null;
		}
	}

	
	
	/*
	 * Checks the adjusted Grammar
	 * Returns a list of ParseTree's if the string could be parsed successfully
	 * Otherwise null
	 * */
	private ArrayList<ParseTree> getMultipleParseTreesUsingAdjustedGrammar(String state, HashMap<String, GDef> master, int gRuleC, int elemC, String created_string) {
		ArrayList<ParseTree> pt_list = new ArrayList<ParseTree>();
		try {
			ParserLib pl_adjusted = null;
			EarleyParser ep_adjusted = null;
			Date d_start = new Date();
//			if(this.log) {
//				System.out.println("Import ParserLib grammar");
//			}
			pl_adjusted = new ParserLib(grammar);
			Date d_end = new Date();
			long difference = d_end.getTime() - d_start.getTime();
//			if(this.log) {
//				System.out.println("ParserLib grammar successfully imported in " + difference / 1000 + " seconds");
//			}
			
			// Replace the rule with <anychars>
			pl_adjusted.grammar.get(state).get(gRuleC).set(elemC, "<anychars>");
			if(this.log) {
				System.out.printf("Adjusted %s[%d][%d]: %s => %s\n", 
						state, gRuleC, elemC, master.get(state).get(gRuleC).get(elemC), pl_adjusted.grammar.get(state).get(gRuleC).get(elemC));
			}
			
			
			d_start = new Date();
//			if(this.log) {
//				System.out.println("Create EarleyParser with ParserLib grammar");
//			}
			ep_adjusted = new EarleyParser(pl_adjusted.grammar);
			d_end = new Date();
			difference = d_end.getTime() - d_start.getTime();
			this.setCurr_pl(pl_adjusted);
			this.setCurr_ep(ep_adjusted);
			// ParseTree pt = this.getCurr_pl().parse_string(created_string, this.getCurr_ep());
			// Loop over the returned ArrayList containing ParseTrees that have been created using the adjusted golden grammar
			ArrayList<ParseTree> lst = this.getCurr_pl().parse_string(created_string, this.getCurr_ep());
//			System.out.printf("Created %d different ParseTrees for %s", lst.size(), created_string);
    		for(ParseTree pt : lst) {
    			if(pt != null) {
    				// Parsed successfully using the adjusted grammar

    	    		// Add to return list
    	    		pt_list.add(pt);
    			}
    		}
    		if(this.log && lst.size() > 0) {
    			System.out.println("String " + created_string + " successfully parsed using the adjusted golden grammar");
			}
        	return pt_list;
		} catch (Exception e) {
  			if(this.log) {
				System.out.println("Failed to parse the string using the adjusted grammar");
			}
			return null;
		}
	
	}

	/*
	 * Given a ParseTree, this method will identify and remove the
	 * <anychar/s/p> characters within the string. The thus minimized 
	 * string will then be checked again (can we still parse the string
	 * using the adjusted grammar rule?). 
	 * Returns: A new minimized ParseTree
	 * 
	 * */
	private ParsedStringSettings identifiyAndRemoveAnycharChars(ParseTree result, String created_string, String state, HashMap<String, GDef> master, int gRuleC, int elemC, Entry<String, GDef> entry, GRule anycharsp) {
		ParsedStringSettings pss = null;
		StringBuilder sb_created_string = new StringBuilder(created_string);
		// As the given tree might have multiple sequences of <anychars>, we have to loop over until there is no character left that has been represented by an anychar tag
		String anychars = result.print_tree_anychar_chars();
		int anychars_length;
		int pos;
		int removed_chars = 0;
		StringBuilder removed_chars_from_string = new StringBuilder("");
		// Initialize for the first iteration in while(){...}
		HashMap<Integer, Integer> pos_length_lst = result.getMapOfPosStringAnychar();
		Map<Integer, Integer> sorted_pos_length_lst = new TreeMap<Integer, Integer>(pos_length_lst);
		int counter = 0;
		for(Map.Entry<Integer, Integer> pos_length : sorted_pos_length_lst.entrySet()) {
//			System.out.println("Key: " + pos_length.getKey() + " Value: " + pos_length.getValue());
			pos = pos_length.getKey();
			pos -= removed_chars; // We take the length of characters, that have already been removed into account
			anychars_length = pos_length.getValue();
			if(this.log) {
				System.out.printf("Starting position to remove characters from: %d\nLength: %d\n", pos, anychars_length);
			}
			for(int i = 0; i < anychars_length; i++) { // Can not use normal delete as there a strange behavior when start = end (then nothing will be deleted))
				try {
//					System.out.println("Lenght of the current string: " + sb_created_string.length());
					removed_chars_from_string.append(sb_created_string.charAt(pos));
//					System.out.println("Removed char");
					sb_created_string.deleteCharAt(pos); // Delete the character at position POS anychars_length-times
					if(this.log) {
						System.out.printf("Removed %s from %s\n", removed_chars_from_string.charAt(counter), sb_created_string);
					}
					counter += 1;
				} catch (Exception e) {
					System.out.printf("Exception: %s\nPos: %d, Removed chars: %d, anychars_length: %d\nremoved_chars_from_string: %s\nsb_created_string: %s",
							e.toString(), pos, removed_chars, anychars_length, removed_chars_from_string.toString(), sb_created_string.toString());
				}
				
			}
			removed_chars += anychars_length;
			// Get new data 
			if(this.log) {
				System.out.println(String.format("String representing through <anychar>: %s\n"
						+ "Anychar string starting at position: %d\nLength of removed sequence: %d\n"
						+ "String after removing those characters: %s\n", 
						removed_chars_from_string, pos, anychars_length, sb_created_string));
			}
		}
		if(this.log) {
			if(sb_created_string.toString().equals("")) {
				System.out.printf("Finished removing characters that have been displayed using <anychar> blocks.\nString before removing: %s\nString after removing those characters: %s\n",
						created_string, "EMPTY STRING");
			}
			else {
				System.out.printf("Finished removing characters that have been displayed using <anychar> blocks.\nString before removing: %s\nString after removing those characters: %s\n",
						created_string, sb_created_string);
			}
		}
		// Check if the string is an empty string
		if(sb_created_string.toString().equals("")) {
			return null;
		}
		
		// Check if the string without anychar-chars will be rejected by the golden grammar and accepted by the adjusted golden grammar
		// Differentiate between anychar and anycharsp (gRuleC and elemC = -1)
		if(!(gRuleC == -1 && elemC == -1)) {
			    pss = new ParsedStringSettings(
	            		created_string,
	            		sb_created_string.toString(),
	            		"",
	            		result.count_nodes(0, this.getExclude_grammars()),
	            		result.count_leafes(),
	            		state, 
	            		entry.getValue().get(gRuleC), 
	            		entry.getValue().get(gRuleC).get(elemC), 
	            		result, 
	            		this.getCurr_pl());
		}
		else { // <Anycharsp>
	            pss = new ParsedStringSettings(
	            		created_string,
	            		sb_created_string.toString(),
	            		"",
	            		result.count_nodes(0, this.getExclude_grammars()),
	            		result.count_leafes(),
	            		state, 
	            		null, 
	            		"ADDED RULE <ANYCHARSP>", 
	            		result, 
	            		this.getCurr_pl());
		}
		if(this.log) {
			System.out.println("Finished removing characters that are being represented by <anychar> blocks for " + pss.hashCode());
		}
		return pss;
	}
	
	/*
	 * Checks if the given string could be parsed using the golden grammar
	 * Returns true if the string was parsed successfully
	 * Otherwise returns false
	 * */
	private boolean successfullyParsedUsingGoldenGrammar(String adjusted_string) {
		try {
			// ParseTree result = this.getGolden_grammar_PL().parse_string(adjusted_string, this.getGolden_grammar_EP());
			ParseTree pt =  this.getGolden_grammar_PL().check_string(adjusted_string, this.getGolden_grammar_EP());
			if(pt != null) {
				System.out.println("Successfully parsed the string " + adjusted_string + " using the golden grammar...");
				return true;
			}
			return false;
		} catch (Exception e) {
			if(this.log) {
				System.out.println("Golden grammar could not parse " + adjusted_string + " successfully");
			}
			return false;
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
			validate_json(getCurrent_str(), log_level); // Try to validate the current string (current_str)
			// System.exit(0);
			if(log_level) {
				System.out.format("%s n=%d, c=%s. Input string was %s", rv, n, c, getCurrent_str());
			}
			if(getRv().equals(complete)) { // Return if complete
				return getCurrent_str();
			}
			else if(getCurrent_str().length() >= MAX_INPUT_LENGTH) { // Unlikely that a string with a size of >1500 characters will end in a valid string
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
				// prev_str = curr_str;
				continue;
			}
			else if(getRv().equals(wrong)) { // The current string was wrong; don't concatenate the generated character to the previous string
				continue;
			}
			else {
				System.out.println("ERROR What is this I dont know !!!");
				break;
			}
		}
		return null;
	}
	private void validate_json(String input_str, boolean log_level) {
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
			String msg = e.toString();			
			// Optional: Save the error message as well as the input_str that produced the error message
			ArrayList<Integer> numbers = getNumbersFromErrorMessage(msg);
			if(msg.contains(text_must_begin_with)) {
				ArrayList<String> value = errors.get(text_must_begin_with);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(text_must_begin_with, new_value);
				}
				else { // Error already exists
					// Insert new element in ArrayList
					// but only if the input_str is not already within the ArrayList
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
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
			else if (msg.contains(text_must_end_with)) {
				ArrayList<String> value = errors.get(text_must_end_with);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(text_must_end_with, new_value);
				}
				else {
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
				n = numbers.get(0) - 1;
				if(getGeneratedChar() == 0) {
					n++;
				}
				rv = "incomplete";
				c = null;
//				if(n >= input_str.length()) {
//					rv = "incomplete";
//					c = null;
//				}
//				else {
//					System.out.println(e.toString());
//					System.out.println("SOMETHING WENT WRONG");
//					System.exit(0);
//				}
				
			}
			else if (msg.contains(expected_a)) {
				ArrayList<String> value = errors.get(expected_a);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(expected_a, new_value);
				}
				else {
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
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
				ArrayList<String> value = errors.get(unterminated_string);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(unterminated_string, new_value);
				}
				else {
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
				n = -1;
				rv = incomplete;
				c = null;
				
			}
			else if (msg.contains(missing_value)) {
				ArrayList<String> value = errors.get(missing_value);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(missing_value, new_value);
				}
				else { 
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
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
				ArrayList<String> value = errors.get(illegal_escape);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(illegal_escape, new_value);
				}
				else {
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
				n = -1;
				rv = wrong;
				c = null; 
				
			}
			else if (msg.contains(duplicate_key)) {
				ArrayList<String> value = errors.get(duplicate_key);
				if(value == null) {
					ArrayList<String> new_value = new ArrayList<String>();
					new_value.add(input_str);
					errors.put(duplicate_key, new_value);
				}
				else {
					if(!value.contains(input_str)) {
						value.add(input_str);
					}
				}
				n = -1;
				rv = wrong;
				c = null;
				
			}
			
			else {
				// Something went wrong
				System.out.println("Unknown error; exit program");
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
        	if (!(i == 0 || i == 13 || i == 10)) {
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
	public Map<String, ArrayList<String>> getErrors() {
		return errors;
	}
	public void setErrors(Map<String, ArrayList<String>> errors) {
		this.errors = errors;
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
}
