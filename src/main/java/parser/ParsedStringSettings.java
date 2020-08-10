package parser;

public class ParsedStringSettings {
	private String created_string;
	private String removed_anychar_string;
	private String hdd_string;
	private int tree_size;
	private int leaf_size;
	private String changed_rule;
	private GRule changed_token;
	private String changed_elem;
	private ParseTree tree;
	private ParserLib pl;
	
	public ParsedStringSettings(String created_string, String removed_anychar_string, String hdd_string, int tree_size, int leaf_size, String changed_rule, GRule token, String changed_elem, ParseTree tree, ParserLib pl) {
		super();
		this.created_string = created_string;
		this.removed_anychar_string = removed_anychar_string;
		this.hdd_string = hdd_string;
		this.tree_size = tree_size;
		this.leaf_size = leaf_size;
		this.changed_rule = changed_rule;
		this.changed_token = token;
		this.changed_elem = changed_elem;
		this.tree = tree;
		this.pl = pl;
	}
	
	/*
	 * Copy
	 * */
	public ParsedStringSettings(ParsedStringSettings source) {
		this.created_string = source.created_string;
		this.removed_anychar_string = source.removed_anychar_string;
		this.hdd_string = source.hdd_string;
		this.tree_size = source.tree_size;
		this.leaf_size = source.leaf_size;
		this.changed_rule = source.changed_rule;
		this.changed_token = source.changed_token;
		this.changed_elem = source.changed_elem;
		this.tree = new ParseTree(source.tree);
		this.pl = source.pl;
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
	public int getLeaf_size() {
		return leaf_size;
	}
	public void setLeaf_size(int leaf_size) {
		this.leaf_size = leaf_size;
	}
	public ParserLib getPl() {
		return pl;
	}
	public void setPl(ParserLib pl) {
		this.pl = pl;
	}
	public String getCreated_string() {
		return created_string;
	}
	public void setCreated_string(String created_string) {
		this.created_string = created_string;
	}
	public String getHdd_string() {
		return hdd_string;
	}
	public void setHdd_string(String hdd_string) {
		this.hdd_string = hdd_string;
	}
	public String getRemoved_anychar_string() {
		return removed_anychar_string;
	}
	public void setRemoved_anychar_string(String removed_anychar_string) {
		this.removed_anychar_string = removed_anychar_string;
	}

	@Override
	public String toString() {
		if(this.getChanged_rule().equals("<elements>")) {
			System.out.println("");
		}
		String ras = this.getRemoved_anychar_string().equals("") ? "Empty string" : this.getRemoved_anychar_string(); 
		String result = String.format("ID: %s\nString: %s\nString after removing characters represented using <anychar>: %s\nMinimized string using HDD: %s\nAdjusted rule: %s\n"
									+ "Adjusted token: %s\nAdjusted element: %s\nTree size: %d\nTree:\n%s\nLeaf size: %d\n",
				this.hashCode(),
				this.getCreated_string(), // Created string
				ras, // String after removing <anychar> parts
				this.getHdd_string(), // Minimized string using hdd
				this.getChanged_rule(), // Adjusted rule 
				this.getChanged_token(), // Adjusted Token 
				this.getChanged_elem(), // Adjusted Element
				this.getTree_size(), // Tree size 
				this.getTree().tree_to_string(), // Tree
				this.getLeaf_size()); // Leaf size
//		String result = String.format("String: %s\nString after removing characters represented using <anychar>: %s\nMinimized string using HDD: %s\n",
//				this.getCreated_string(), // Created string
//				this.getRemoved_anychar_string(), // String after removing <anychar> parts
//				this.getHdd_string()); // Minimized string using hdd
		
		return result;
	}
	
	
}
