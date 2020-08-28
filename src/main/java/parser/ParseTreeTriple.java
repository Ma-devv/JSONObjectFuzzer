package parser;

import java.util.ArrayList;
import java.util.HashMap;

public class ParseTreeTriple {
	// Credits to https://stackoverflow.com/questions/521171/a-java-collection-of-value-pairs-tuples
	private int tree_size;
	private ParseTree pt;
	private HashMap<Integer, ArrayList<Integer>> tree_path_with_depth;
	
	public ParseTreeTriple(int tree_size, ParseTree pt, HashMap<Integer, ArrayList<Integer>> tree_path_with_depth) {
		this.tree_size = tree_size;
		this.pt = pt;
		this.tree_path_with_depth = tree_path_with_depth;
	}


	public HashMap<Integer, ArrayList<Integer>> getTree_path_with_depth() {
		return tree_path_with_depth;
	}


	public void setTree_path_with_depth(HashMap<Integer, ArrayList<Integer>> tree_path_with_depth) {
		this.tree_path_with_depth = tree_path_with_depth;
	}


	public int getTree_size() {
		return tree_size;
	}

	public void setTree_size(int tree_size) {
		this.tree_size = tree_size;
	}

	public ParseTree getPt() {
		return pt;
	}

	public void setPt(ParseTree pt) {
		this.pt = pt;
	}

	@Override
	public int hashCode() {
		return this.getTree_size() ^ this.getPt().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParseTreeTriple)) {
			return false;
		}
		ParseTreeTriple ptt = (ParseTreeTriple) obj;
		return (this.getPt().equals(ptt.getPt()) && this.getTree_size() == ptt.getTree_size());
	}
	
	
}
