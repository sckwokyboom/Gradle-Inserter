package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.PropertyNode;
import com.github.gradleinserter.view.PropertyView;
import com.github.gradleinserter.view.SemanticView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for merging property assignments.
 *
 * Behavior:
 * - Updates values of existing properties
 * - Adds new properties at the appropriate position (after plugins block if exists)
 */
public class PropertyMergeStrategy implements MergeStrategy<PropertyView> {

    @NotNull
    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.PROPERTY;
    }

    @NotNull
    @Override
    public List<IInsertionEdit> merge(@Nullable PropertyView original, @NotNull PropertyView snippet,
                                      @NotNull String originalSource, @NotNull MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null) {
            // No matching property in original - add it
            int insertPos = findPropertyInsertionPoint(context);
            String newProperty = formatProperty(snippet, context, insertPos == 0);
            edits.add(new ReplaceEdit(insertPos, insertPos, newProperty,
                    "Add property: " + snippet.getName()));
        } else {
            // Update existing property value
            PropertyNode originalNode = original.getPropertyNode();
            String snippetValue = snippet.getValue();
            String originalValue = original.getValue();

            if (!snippetValue.equals(originalValue)) {
                edits.add(new ReplaceEdit(
                        originalNode.getValueStartOffset(),
                        originalNode.getValueEndOffset(),
                        snippetValue,
                        "Update property: " + original.getName() + " = " + snippetValue));
            }
        }

        return edits;
    }

    /**
     * Check if this strategy can handle the given views.
     * For properties, we match by property name.
     */
    @Override
    public boolean canHandle(@Nullable SemanticView original, @NotNull SemanticView snippet) {
        if (snippet.getType() != SemanticView.ViewType.PROPERTY) {
            return false;
        }
        if (original == null) {
            return true;
        }
        if (original.getType() != SemanticView.ViewType.PROPERTY) {
            return false;
        }
        // Match by property name
        PropertyView origProp = (PropertyView) original;
        PropertyView snippetProp = (PropertyView) snippet;
        return origProp.getName().equals(snippetProp.getName());
    }

    private int findPropertyInsertionPoint(@NotNull MergeContext context) {
        return context.getSemanticInsertionPoint(SemanticView.ViewType.PROPERTY);
    }

    @NotNull
    private String formatProperty(@NotNull PropertyView property, @NotNull MergeContext context, boolean atStart) {
        StringBuilder sb = new StringBuilder();
        if (!atStart) {
            sb.append("\n\n");
        }
        sb.append(property.getName()).append(" = ").append(property.getValue());
        if (atStart) {
            sb.append("\n");
        }
        return sb.toString();
    }
}
