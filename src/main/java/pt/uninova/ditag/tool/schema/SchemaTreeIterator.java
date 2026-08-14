package pt.uninova.ditag.tool.schema;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import pt.uninova.ditag.tool.node.SchemaNode;

public class SchemaTreeIterator implements Iterator<SchemaNode> {
    private Stack<SchemaNode> stack = new Stack<>();

    public SchemaTreeIterator(SchemaNode root) {
        if (root != null) {
            stack.push(root);
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public SchemaNode next() {
        if (!hasNext()) {
            throw new java.util.NoSuchElementException();
        }
        SchemaNode node = stack.pop();
        if (node.getChildren() != null) {
            List<SchemaNode> children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return node;
    }
}
