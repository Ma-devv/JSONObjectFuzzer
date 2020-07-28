package parser;

public class ParseTreeTuple {
	// Credits to https://stackoverflow.com/questions/521171/a-java-collection-of-value-pairs-tuples
	private int tree_size;
	private ParseTree pt;
	
	public ParseTreeTuple(int tree_size, ParseTree pt) {
		this.tree_size = tree_size;
		this.pt = pt;
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
		if (!(obj instanceof ParseTreeTuple)) {
			return false;
		}
		ParseTreeTuple ptt = (ParseTreeTuple) obj;
		return (this.getPt().equals(ptt.getPt()) && this.getTree_size() == ptt.getTree_size());
	}
	
	
}
