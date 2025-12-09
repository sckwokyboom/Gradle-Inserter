package com.github.gradleinserter.merge;

import com.github.gradleinserter.api.IInsertionEdit;
import com.github.gradleinserter.api.ReplaceEdit;
import com.github.gradleinserter.ir.BlockNode;
import com.github.gradleinserter.view.PluginItem;
import com.github.gradleinserter.view.PluginsView;
import com.github.gradleinserter.view.SemanticView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Strategy for merging plugins blocks.
 *
 * Behavior:
 * - Updates versions of existing plugins
 * - Adds new plugins that don't exist
 * - Preserves original formatting
 */
public class PluginsMergeStrategy implements MergeStrategy<PluginsView> {

    @Override
    public SemanticView.ViewType getViewType() {
        return SemanticView.ViewType.PLUGINS;
    }

    @Override
    public List<IInsertionEdit> merge(PluginsView original, PluginsView snippet,
                                      String originalSource, MergeContext context) {
        List<IInsertionEdit> edits = new ArrayList<>();

        if (original == null || original.getBlockNode() == null) {
            // No plugins block in original - need to create one at the start
            String newBlock = generatePluginsBlock(snippet, context);
            // Plugins block should be at the beginning of the file
            edits.add(new ReplaceEdit(0, 0, newBlock + "\n\n",
                    "Add plugins block"));
            return edits;
        }

        BlockNode originalBlock = original.getBlockNode();

        // Process each snippet plugin
        for (PluginItem snippetPlugin : snippet.getPlugins()) {
            Optional<PluginItem> existingPlugin = original.findById(snippetPlugin.getId());

            if (existingPlugin.isEmpty()) {
                // New plugin - add it
                String newLine = formatPluginLine(snippetPlugin, context);
                int insertPos = originalBlock.getBodyEndOffset();
                edits.add(new ReplaceEdit(insertPos, insertPos, newLine,
                        "Add plugin: " + snippetPlugin.getId()));
            } else {
                // Update existing plugin version if needed
                PluginItem existing = existingPlugin.get();
                if (needsVersionUpdate(existing, snippetPlugin)) {
                    IInsertionEdit edit = createVersionUpdateEdit(existing, snippetPlugin, originalSource);
                    if (edit != null) {
                        edits.add(edit);
                    }
                }
            }
        }

        return edits;
    }

    private boolean needsVersionUpdate(PluginItem existing, PluginItem snippet) {
        String snippetVersion = snippet.getVersion();
        if (snippetVersion == null || snippetVersion.isEmpty()) {
            return false;
        }

        String existingVersion = existing.getVersion();
        return !snippetVersion.equals(existingVersion);
    }

    private IInsertionEdit createVersionUpdateEdit(PluginItem existing,
                                                    PluginItem snippet,
                                                    String originalSource) {
        if (existing.getSourceNode() == null) {
            return null;
        }

        String existingSource = existing.getSourceNode().getSourceText();
        String existingVersion = existing.getVersion();
        String newVersion = snippet.getVersion();

        if (existingVersion != null) {
            // Find and replace existing version
            int versionIndex = existingSource.indexOf(existingVersion);
            if (versionIndex >= 0) {
                int absoluteStart = existing.getSourceNode().getStartOffset() + versionIndex;
                int absoluteEnd = absoluteStart + existingVersion.length();
                return new ReplaceEdit(absoluteStart, absoluteEnd, newVersion,
                        "Update plugin version: " + existing.getId()
                                + " " + existingVersion + " -> " + newVersion);
            }
        } else {
            // No existing version - need to add it
            // Insert " version 'x.y.z'" after the id declaration
            int endPos = existing.getSourceNode().getEndOffset();
            String versionSuffix = " version '" + newVersion + "'";
            return new ReplaceEdit(endPos, endPos, versionSuffix,
                    "Add version to plugin: " + existing.getId());
        }

        return null;
    }

    private String formatPluginLine(PluginItem plugin, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndentation());
        sb.append("id '").append(plugin.getId()).append("'");
        if (plugin.getVersion() != null) {
            sb.append(" version '").append(plugin.getVersion()).append("'");
        }
        if (!plugin.isApply()) {
            sb.append(" apply false");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String generatePluginsBlock(PluginsView snippet, MergeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("plugins {");

        for (PluginItem plugin : snippet.getPlugins()) {
            sb.append("\n").append(context.getIndentation());
            sb.append("id '").append(plugin.getId()).append("'");
            if (plugin.getVersion() != null) {
                sb.append(" version '").append(plugin.getVersion()).append("'");
            }
            if (!plugin.isApply()) {
                sb.append(" apply false");
            }
        }

        sb.append("\n}");
        return sb.toString();
    }
}
