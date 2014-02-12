package wyvern.tools.typedAST.extensions;

import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.parsing.LineParser;
import wyvern.tools.parsing.LineSequenceParser;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben Chung on 2/7/14.
 */
public class TypeAsc implements TypedAST {
	private final TypedAST inner;
	private final UnparsedType.CacheAsc asc;


	public TypeAsc(TypedAST inner, UnparsedType asc) {
		this.inner = inner;
		this.asc = UnparsedType.cacheAsc(asc);
	}

	@Override
	public Type getType() {
		return asc.getType();
	}

	@Override
	public Type typecheck(Environment env) {
		Type typecheck = inner.typecheck(env);
		if (!typecheck.subtype(asc.getAsc(env)))
			ToolError.reportError(ErrorMessage.ACTUAL_FORMAL_TYPE_MISMATCH, inner, asc.getType().toString(), typecheck.toString());
		return typecheck;
	}

	@Override
	public Value evaluate(Environment env) {
		throw new RuntimeException();
	}

	@Override
	public LineParser getLineParser() {
		return null;
	}

	@Override
	public LineSequenceParser getLineSequenceParser() {
		return null;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		HashMap<String, TypedAST> children = new HashMap<>();
		children.put("inner", inner);
		return children;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new TypeAsc(newChildren.get("inner"), asc);
	}

	@Override
	public FileLocation getLocation() {
		return inner.getLocation();
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {

	}
}