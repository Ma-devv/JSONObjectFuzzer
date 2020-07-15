package parser;

public class ParsedStringSettings {
	private int tree_size;
	private String changed_rule;
	private GRule changed_token;
	private String changed_elem;
	private ParseTree tree;
	private ParserLib pl;
	
	public ParsedStringSettings(int tree_size, String changed_rule, GRule token, String changed_elem, ParseTree tree, ParserLib pl) {
		super();
		this.tree_size = tree_size;
		this.changed_rule = changed_rule;
		this.changed_token = token;
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
	public String getChanged_rule() {
		return changed_rule;
	}
	public void setChanged_rule(String changed_rule) {
		this.changed_rule = changed_rule;
	}
	public GRule getChanged_token() {
		return changed_token;
	}
	public void setChanged_token(GRule changed_token) {
		this.changed_token = changed_token;
	}

	@Override
	public String toString() {
		String result = String.format("Adjusted rule: %s\nAdjusted Token: %s\nAdjusted Element: %s\nTree size: %d\nTree: \n%s", 
				this.getChanged_rule(), this.getChanged_token(), this.getChanged_elem(), this.getTree_size(), ParserLib._save_tree(pl, this.getTree()));
		return result;
	}
	
	
}
