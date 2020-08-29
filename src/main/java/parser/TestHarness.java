package parser;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface to implement a test harness for evaluating input sets
 * for passing or failing/error conditions.
 * 
 * @author Ben Holland
 */
public abstract class TestHarness<E> {

	public static final int PASS = 1;
	public static final int UNRESOLVED = 0;
	public static final int FAIL = -1;
	
	
	private ParserLib golden_grammar_PL;
	private EarleyParser golden_grammar_EP;	
	private ArrayList<String> hdd_string_pos;
	private ArrayList<Integer> hdd_string_non_anychar_pos;
	
	public ArrayList<Integer> getHdd_string_non_anychar_pos() {
		return hdd_string_non_anychar_pos;
	}
	public void setHdd_string_non_anychar_pos(ArrayList<Integer> hdd_string_non_anychar_pos) {
		this.hdd_string_non_anychar_pos = hdd_string_non_anychar_pos;
	}
	public ArrayList<String> getHdd_string_pos() {
		return hdd_string_pos;
	}
	public void setHdd_string_pos(ArrayList<String> hdd_string_pos) {
		this.hdd_string_pos = hdd_string_pos;
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


	public boolean parsedByGG(EarleyParser ep, String input_string) {
		return Fuzzer.checkIfStringCanBeParsedWithGivenGrammar(ep, input_string);
	}

	/**
	 * Returns true if the test passes and false if the test fails
	 * @param <E>
	 * @param complement
	 * @return
	 */
	public abstract int run(List<E> input);

}
