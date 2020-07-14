package parser;

public class ParsedStringSettings {
	private int tree_size;
	private GRule changed_rule;
	private String changed_elem;
	private ParseTree tree;
	private ParserLib pl;
	
	public ParsedStringSettings(int tree_size, GRule changed_rule, String changed_elem, ParseTree tree, ParserLib pl) {
		super();
		this.tree_size = tree_size;
		this.changed_rule = changed_rule;
		this.changed_elem = changed_elem;
		this.tree = tree;
		this.pl = pl;
	}
	
	public int getTree_size() {
		return tree_size;
	}
	public void setTree_size(int tree_size) {
		this.tree_size = tree_size;
	}
	public GRule getChanged_rule() {
		return changed_rule;
	}
	public void setChanged_rule(GRule changed_rule) {
		this.changed_rule = changed_rule;
	}
	public String getChanged_elem() {
		return changed_elem;
	}
	public void setChanged_elem(String changed_elem) {
		this.changed_elem = changed_elem;
	}
	public ParseTree getTree() {
		return tree;
	}
	public void setTree(ParseTree tree) {
		this.tree = tree;
	}
	@Override
	public String toString() {
		String result = String.format("Adjusted rule: %s\nAdjusted Token: %s\nTree size: %d\nTree: \n%s", 
				this.getChanged_rule(), this.getChanged_elem(), this.getTree_size(), ParserLib._save_tree(pl, this.getTree()));
		return result;
	}
	
	
}
