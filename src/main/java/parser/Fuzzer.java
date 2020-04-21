package parser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	
	public static void main(String[] args) {
		Fuzzer fuzzer = new Fuzzer("", 0, null, args[0], null); // Create new Fuzzer
		fuzzer.create_valid_strings(100, false); // Create 20 valid strings; no log level enabled
		// Print out the found strings
		for(Map.Entry<String, ArrayList<String>> entry : fuzzer.getValid_strings().entrySet()) {
			String key = entry.getKey();
			ArrayList<String> value = entry.getValue();
			System.out.println("Found " + value.size() + " inputs for the exception " + key);
			for(String s : value) {
				System.out.println("Valid String: " + s + "\n");
			}
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
			String created_string = generate(log_level); // Generate a new valid JSON Object, according to org.json.JSONObject
			if(created_string != null) {
				// Check if the created string is also valid according to the "golden grammar"
				ParserLib pl;
		        try {
		            pl = new ParserLib(getGrammar());
		            pl.parse_string(created_string); // Try to parse the string
		            // The string has been parsed successfully and thus we just continue... 
		            System.out.println("String " + created_string + ". successfully parsed using the golden grammar... Continuing");
		        } catch (Exception e) { 
		        	// The string has not been parsed successfully
		        	// Hence the string is valid for org.json.JSONObject 
		        	// but is not according to the golden grammar
		        	System.out.println("Found the " + (i+1) + ". valid string");
		        	ArrayList<String> valid_string_list = getValid_strings().get(e.toString()); // Get the list with valid strings
		        	// null if empty for the error message
		        	if(valid_string_list != null) { // Does a valid string for this error already exists?
		        		// If so, then add the element (valid string) to the list for this key (error message)
		        		if(!valid_string_list.contains(created_string)) {
		        			valid_string_list.add(created_string);
							i++; // Increase the counter
		        		}
		        	}
		        	else { // Key does not exist yet
		        		// create a new key
		        		ArrayList<String> new_valid_string_list = new ArrayList<String>();
		        		new_valid_string_list.add(created_string); // Add the created, "valid" string
		        		getValid_strings().put(e.toString(), new_valid_string_list); // Add the key-value pair to the valid_strings hashmap
		        		i++;
		        	}
		        }
				if(i >= n) { // Did we create enough valid strings?
					break;
				}
			}
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
			else if(getCurrent_str().length() >= 1500) { // Unlikely that a string with a size of >1500 characters will end in a valid string
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
            result[i] = (char) i;
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

}
