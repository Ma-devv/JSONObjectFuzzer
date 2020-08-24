package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class MinimizeAnychar {
	private ParserLib golden_grammar_PL;
	private EarleyParser golden_grammar_EP;
	

	public void startDD(ParsedStringSettings pss, EarleyParser ep, ParserLib pl) {
		try {
			System.out.printf("%s", pss.toString());
			ArrayList<Integer> positions = new ArrayList<Integer>();
			// Delta Debugging should only be applied on the <anychar> part
			ArrayList<String> hdd_string = new ArrayList<String>();
			for(char c : pss.getHdd_string().toCharArray()) {
				hdd_string.add(Character.toString(c));
			}
			harness.setHdd_string_pos(hdd_string);
			harness.setGolden_grammar_EP(ep);
			harness.setGolden_grammar_PL(pl);
			HashMap<Integer, Integer> pos_length_lst = pss.getTree().getMapOfPosStringAnychar();
			Map<Integer, Integer> sorted_pos_length_lst = new TreeMap<Integer, Integer>(pos_length_lst);
			for(Map.Entry<Integer, Integer> pos_length : sorted_pos_length_lst.entrySet()) {
				int start_pos = pos_length.getKey();
				int len = pos_length.getValue();
				System.out.printf("Start: %d, Length: %d\n", start_pos, len);
				for(int i = 0; i < len; i++) {
					positions.add(i + start_pos);
				}
			}
			ArrayList<Integer> hdd_string_non_anychar_pos = new ArrayList<Integer>();
			for(int i = 0; i < hdd_string.size(); i++) {
				if(!positions.contains(i)) {
					hdd_string_non_anychar_pos.add(i);
				}
			}
			harness.setHdd_string_non_anychar_pos(hdd_string_non_anychar_pos);
			List<Integer> result = DeltaDebug.ddmin(positions, harness);
			result.addAll(hdd_string_non_anychar_pos);
			Collections.sort(result);
			StringBuilder sb = new StringBuilder();
			for(int i : result) {
				sb.append(hdd_string.get(i));
			}
			System.out.printf("String before reducing <anychar> block to its minimum: %s"
					+ "\nString after reducing <anychar> block using DD: %s\n", pss.getHdd_string(), sb.toString());
		} catch (Exception e) {
			System.out.printf("Exception during DD: %s\n", e.toString());
		}
		
	}

	private TestHarness<Integer> harness = new TestHarness<Integer>() {
		@Override
		public int run(List<Integer> input) {
			// As the list does only contain anychar characters, we need to build the rest of the string around this characters
			// input declares (partly) the positions of the characters that have been represented using anychar
			ArrayList<Integer> rebuild = new ArrayList<Integer>();
			rebuild.addAll(input);
			rebuild.addAll(this.getHdd_string_non_anychar_pos());
			Collections.sort(rebuild);
			StringBuilder sb = new StringBuilder();
			for(int i : rebuild) {
				sb.append(this.getHdd_string_pos().get(i));
			}
			System.out.printf("\nReceived input list by DD: %s\nComplete hdd string: %s\nString that has been rebuilt: %s\n", input.toString(), this.getHdd_string_pos(), sb.toString());
			if(!parsedByGG(sb.toString()) && parsedByBinary(sb.toString())) {
				return FAIL;
			}
			return PASS;
		}

		private boolean parsedByBinary(String input) {
			if (input.startsWith("{")) {
				try {
					JSONObject obj = new JSONObject(input);
					return true;
				} catch (Exception e) {
					return false;
				}
			} else {
				try {
					JSONArray arr = new JSONArray(input);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		}
	};
}