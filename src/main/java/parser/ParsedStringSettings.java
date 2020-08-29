package parser;

public class ParsedStringSettings {
	private String created_string;
	private String string_after_removing_anychars;
	private String hdd_string;
	private String dd_string;
	private int tree_size;
	private int leaf_size;
	private String changed_rule;
	private GRule changed_token;
	private String changed_elem;
	private ParseTree original_tree;
	private ParseTree hdd_tree;
	private ParseTree dd_tree;
	private ParseTree abstracted_tree;
	private ParserLib pl;
	private String data_type;
	
	public ParsedStringSettings(
			String created_string,
			String string_after_removing_anychars,
			String hdd_string,
			String dd_string,
			int tree_size,
			int leaf_size,
			String changed_rule, 
			GRule token,
			String changed_elem,
			ParseTree original_tree,
			ParseTree hdd_tree,
			ParseTree dd_tree,
			ParseTree abstracted_tree,
			ParserLib pl,
			String data_type) {
		super();
		this.created_string = created_string;
		this.string_after_removing_anychars = string_after_removing_anychars;
		this.hdd_string = hdd_string;
		this.dd_string = dd_string;
		this.tree_size = tree_size;
		this.leaf_size = leaf_size;
		this.changed_rule = changed_rule;
		this.changed_token = token;
		this.changed_elem = changed_elem;
		this.original_tree = original_tree;
		this.hdd_tree = hdd_tree;
		this.dd_tree = dd_tree;
		this.abstracted_tree = abstracted_tree;
		this.pl = pl;
		this.data_type = data_type;
	}
	
	/*
	 * Copy
	 * */
	public ParsedStringSettings(ParsedStringSettings source) {
		this.created_string = source.created_string;
		this.string_after_removing_anychars = source.string_after_removing_anychars;
		this.hdd_string = source.hdd_string;
		this.dd_string = source.dd_string;
		this.tree_size = source.tree_size;
		this.leaf_size = source.leaf_size;
		this.changed_rule = source.changed_rule;
		this.changed_token = source.changed_token;
		this.changed_elem = source.changed_elem;
		if(source.original_tree != null) {
			this.original_tree = new ParseTree(source.original_tree);
		}
		if(source.hdd_tree != null) {
			this.hdd_tree = new ParseTree(source.hdd_tree);
		}
		if(source.dd_tree != null) {
			this.dd_tree = new ParseTree(source.dd_tree);
		}
		if(source.abstracted_tree != null) {
			this.abstracted_tree = new ParseTree(source.abstracted_tree);
		}
		this.pl = source.pl;
		this.data_type = source.data_type;
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
	public String getString_after_removing_anychars() {
		return string_after_removing_anychars;
	}
	public void setString_after_removing_anychars(String string_after_removing_anychars) {
		this.string_after_removing_anychars = string_after_removing_anychars;
	}
	public String getData_type() {
		return data_type;
	}
	public void setData_type(String data_type) {
		this.data_type = data_type;
	}
	public String getDd_string() {
		return dd_string;
	}
	public void setDd_string(String dd_string) {
		this.dd_string = dd_string;
	}
	public ParseTree getOriginal_tree() {
		return original_tree;
	}
	public void setOriginal_tree(ParseTree original_tree) {
		this.original_tree = original_tree;
	}
	public ParseTree getHdd_tree() {
		return hdd_tree;
	}
	public void setHdd_tree(ParseTree hdd_tree) {
		this.hdd_tree = hdd_tree;
	}
	public ParseTree getDd_tree() {
		return dd_tree;
	}
	public void setDd_tree(ParseTree dd_tree) {
		this.dd_tree = dd_tree;
	}
	public ParseTree getAbstracted_tree() {
		return abstracted_tree;
	}
	public void setAbstracted_tree(ParseTree abstracted_tree) {
		this.abstracted_tree = abstracted_tree;
	}

	@Override
	public String toString() {
		String ras = this.getString_after_removing_anychars().equals("") ? "Empty string" : this.getString_after_removing_anychars();
		// Preventing null pointer exception
		String dd_tree_np = this.getDd_tree() == null ? "Tree not set yet\n" : this.getDd_tree().tree_to_string();
		String abstracted_tree_np = this.getAbstracted_tree() == null ? "Tree not abstracted yet\n" : this.getAbstracted_tree().abstract_tree_to_string();
		String result = String.format("ID: %s\nObject: %s\n"
				+ "String: %s\n"
				+ "String after removing characters represented using <anychar>: %s\n"
				+ "Minimized string using HDD: %s\n"
				+ "String after minimizing using DD: %s\n"
				+ "Adjusted rule: %s\n"
				+ "Adjusted token: %s\n"
				+ "Adjusted element: %s\n"
				+ "Tree size: %d\n"
				+ "Original tree:\n%s\n"
				+ "Tree after applying HDD:\n%s\n"
				+ "Tree after applying DD:\n%s\n"
				+ "Abstracted Tree:\n%s\n"
				+ "Leaf size: %d\n",
				this.hashCode(),
				this.getData_type(),
				this.getCreated_string(), // Created string
				ras, // String after removing <anychar> parts
				this.getHdd_string(), // Minimized string using hdd
				this.getDd_string(),
				this.getChanged_rule(), // Adjusted rule 
				this.getChanged_token(), // Adjusted Token 
				this.getChanged_elem(), // Adjusted Element
				this.getTree_size(), // Tree size
				this.getOriginal_tree(),
				this.getHdd_tree().tree_to_string(), // Original Tree
				dd_tree_np,
				abstracted_tree_np,
				this.getLeaf_size()); // Leaf size
		
		return result;
	}
	
	
}
