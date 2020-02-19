package bot;

import java.util.function.Function;

public class DecisionNode {
    
    private Function<DecisionBlackboard, Boolean> precondition;
    private List<DecisionNode> children;

    public DecisionNode(Function<DecisionBlackboard, Boolean> precondition, List<DecisionNode> children) {
        this.precondition = precondition;
        this.children     = children;
    }

    // Check precondition for first matching child
    public void evaluate(DecisionBlackboard decisionBlackboard) {
        this.children.stream()
                     .filter(child->child.precondition(decisionBlackboard))
                     .findFirst()
                     .ifPresent(child->child.evaluate(decisionBlackboard));
    }
}
