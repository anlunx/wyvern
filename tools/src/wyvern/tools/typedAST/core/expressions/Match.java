package wyvern.tools.typedAST.core.expressions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.CachingTypedAST;
import wyvern.tools.typedAST.core.binding.Binding;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.TypeBinding;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.Bool;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.TreeWriter;

/**
 * Represents a match statement in Wyvern.
 * 
 * @author Troy Shaw
 */
public class Match extends CachingTypedAST implements CoreAST {

	private TypedAST matchingOver;
	
	private List<Case> cases;
	private Case defaultCase;
	
	private FileLocation location;
	
	public String toString() {
		return "Match: " + matchingOver + " with " + cases.size() + " cases and default: " + defaultCase;
	}
	
	public Match(TypedAST matchingOver, List<Case> cases, FileLocation location) {		
		
		this.matchingOver = matchingOver;
		
		this.cases = cases;
		
		//find the default case and remove it from the typed cases
		for (Case c : cases) {
			if (c.isDefault()) {
				defaultCase = c;
				break;
			}
		}
		
		cases.remove(defaultCase);
		
		this.location = location;
	}
	
	/**
	 * Internal constructor to save from finding the default case again.
	 * 
	 * @param matchingOver
	 * @param cases
	 * @param defaultCase
	 * @param location
	 */
	private Match(TypedAST matchingOver, List<Case> cases, Case defaultCase, FileLocation location) {		
		this.matchingOver = matchingOver;
		this.cases = cases;
		this.defaultCase = defaultCase;
		this.location = location;
	}
	
	@Override
	public Value evaluate(Environment env) {
		System.out.println("Evaluating match: " + this);
		
		for (Case c : cases) {
			//TODO: this actually needs to check against the type of what we're matching over
			
			// System.out.println("Processing case: " + c);
			
			// System.out.println("Matching over: " + matchingOver.getType());
			ClassType ct = (ClassType) matchingOver.getType();
			System.out.println(ct.getName() + " matches of not: " + c.getTaggedTypeMatch());
			
			if (c.getTaggedTypeMatch().equals(matchingOver)) return c.getAST().evaluate(env);
		}
		
		//no match, evaluate the default case
		return defaultCase.getAST().evaluate(env);
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Map<String, TypedAST> children = new HashMap<>();
		
		for (Case c : cases) {
			//is there a proper convention for names in children?
			children.put("match case: " + c.getTaggedTypeMatch(), c.getAST());
		}
		
		children.put("match default-case: " + defaultCase.getTaggedTypeMatch(), defaultCase.getAST());
		
		return children;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new Match(matchingOver, cases, defaultCase, location);
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		//TODO: is this meant to be empty?
	}

	@Override
	public FileLocation getLocation() {
		return location;
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	protected Type doTypecheck(Environment env, Optional<Type> expected) {
		//Note: currently all errors given use matchingOver because it has a location
		//TODO: use the actual entity that is responsible for the error
		
		this.matchingOver.typecheck(env, expected);
		
		// Variable we're matching must exist and be a tagged type
		for (Case c : cases) {
			if (c.isDefault()) continue;
			
			String tagName = c.getTaggedTypeMatch();
			
			Optional<ClassType> type = env.lookupBinding(tagName, ClassType.class);
			
			NameBinding binding = env.lookup(tagName);
			
			if (binding == null) {
				// type wasn't declared...
				ToolError.reportError(ErrorMessage.UNKNOWN_TAG, matchingOver);
			}
			
			Type t = binding.getType();
			
			if (t instanceof ClassType) {
				ClassType classType = (ClassType) t;
				
				//TODO: check this is tagged. Currently can't because information not accessible in object.
			}
		}
		
		// All tagged types must be unique
		Set<String> caseSet = new HashSet<String>();
		
		for (Case c : cases) {
			if (c.isTyped()) caseSet.add(c.getTaggedTypeMatch());
		}
		
		if (caseSet.size() != cases.size()) {

			ToolError.reportError(ErrorMessage.DUPLICATE_TAG, matchingOver);
		}
			
		// If we've omitted default, we must included all possible tags
		// TODO
		
		// If we've included default, we can't have included all possible tags
		// TODO
		
		return Unit.getInstance();
	}
}
