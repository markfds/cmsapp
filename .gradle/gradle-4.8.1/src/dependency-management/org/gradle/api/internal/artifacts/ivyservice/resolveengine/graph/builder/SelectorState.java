/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.builder;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.internal.artifacts.ResolvedVersionConstraint;
import org.gradle.api.internal.artifacts.dependencies.DefaultResolvedVersionConstraint;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphSelector;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.selectors.ResolvableSelectorState;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentSelectionDescriptorInternal;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentSelectionReasonInternal;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.LocalOriginDependencyMetadata;
import org.gradle.internal.resolve.ModuleVersionResolveException;
import org.gradle.internal.resolve.resolver.DependencyToComponentIdResolver;
import org.gradle.internal.resolve.result.BuildableComponentIdResolveResult;
import org.gradle.internal.resolve.result.ComponentIdResolveResult;
import org.gradle.internal.resolve.result.DefaultBuildableComponentIdResolveResult;

import static org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons.CONSTRAINT;
import static org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons.REQUESTED;

/**
 * Resolution state for a given module version selector.
 *
 * There are 3 possible states:
 * 1. The selector has been newly added to a `ModuleResolveState`. In this case {@link #resolved} will be `false`.
 * 2. The selector failed to resolve. In this case {@link #failure} will be `!= null`.
 * 3. The selector was part of resolution to a particular module version.
 *    In this case {@link #resolved} will be `true` and {@link ModuleResolveState#selected} will point to the selected component.
 */
class SelectorState implements DependencyGraphSelector, ResolvableSelectorState {
    private final Long id;
    private final DependencyState dependencyState;
    private final DependencyMetadata dependencyMetadata;
    private final DependencyToComponentIdResolver resolver;
    private final ResolvedVersionConstraint versionConstraint;
    private final VersionSelectorScheme versionSelectorScheme;
    private final ImmutableAttributesFactory attributesFactory;

    private ComponentIdResolveResult idResolveResult;
    private ModuleVersionResolveException failure;
    private ModuleResolveState targetModule;
    private boolean resolved;
    private boolean forced;

    SelectorState(Long id, DependencyState dependencyState, DependencyToComponentIdResolver resolver, VersionSelectorScheme versionSelectorScheme, ResolveState resolveState, ModuleIdentifier targetModuleId) {
        this.id = id;
        this.dependencyState = dependencyState;
        this.dependencyMetadata = dependencyState.getDependency();
        this.resolver = resolver;
        this.versionSelectorScheme = versionSelectorScheme;
        this.targetModule = resolveState.getModule(targetModuleId);
        this.versionConstraint = resolveVersionConstraint(dependencyMetadata.getSelector());
        this.attributesFactory = resolveState.getAttributesFactory();
        this.forced = isForced(dependencyMetadata);
        targetModule.addSelector(this);
    }

    private ResolvedVersionConstraint resolveVersionConstraint(ComponentSelector selector) {
        if (selector instanceof ModuleComponentSelector) {
            return new DefaultResolvedVersionConstraint(((ModuleComponentSelector) selector).getVersionConstraint(), versionSelectorScheme);
        }
        return null;
    }

    @Override
    public Long getResultId() {
        return id;
    }

    @Override
    public String toString() {
        return dependencyMetadata.toString();
    }

    @Override
    public ComponentSelector getRequested() {
        return selectorWithDesugaredAttributes(dependencyState.getRequested());
    }

    public ModuleResolveState getTargetModule() {
        return targetModule;
    }

    /**
     * Return any failure to resolve the component selector to id, or failure to resolve component metadata for id.
     */
    ModuleVersionResolveException getFailure() {
        return failure;
    }

    /**
     * Does the work of actually resolving a component selector to a component identifier.
     */
    public ComponentIdResolveResult resolve(VersionSelector allRejects) {
        if (!requiresResolve(allRejects)) {
            return idResolveResult;
        }

        BuildableComponentIdResolveResult idResolveResult = new DefaultBuildableComponentIdResolveResult();
        if (dependencyState.failure != null) {
            idResolveResult.failed(dependencyState.failure);
        } else {
            ResolvedVersionConstraint mergedConstraint = versionConstraint == null ? null : new DefaultResolvedVersionConstraint(versionConstraint.getPreferredSelector(), allRejects);
            resolver.resolve(dependencyMetadata, mergedConstraint, idResolveResult);
        }

        if (idResolveResult.getFailure() != null) {
            failure = idResolveResult.getFailure();
        }

        this.idResolveResult = idResolveResult;
        this.resolved = true;
        return idResolveResult;
    }

    private boolean requiresResolve(VersionSelector allRejects) {
        // If we've never resolved, must resolve
        if (idResolveResult == null) {
            return true;
        }

        // If previous resolve failed, no point in re-resolving
        if (idResolveResult.getFailure() != null) {
            return false;
        }

        // If the previous result was rejected, do not need to re-resolve (new rejects will be a superset of previous rejects)
        if (idResolveResult.isRejected()) {
            return false;
        }

        // If the previous result is still not rejected, do not need to re-resolve. The previous result is still good.
        if (allRejects == null || !allRejects.accept(idResolveResult.getModuleVersionId().getVersion())) {
            return false;
        }

        return true;
    }

    @Override
    public void markResolved() {
        this.resolved = true;
    }

    public boolean isResolved() {
        return resolved;
    }

    /**
     * Overrides the component that is the chosen for this selector.
     * This happens when the `ModuleResolveState` is restarted, during conflict resolution or version range merging.
     */
    public void overrideSelection(ComponentState selected) {
        this.resolved = true;

        // Target module can change, if this is called as the result of a module replacement conflict.
        this.targetModule = selected.getModule();

        // TODO:DAZ It's not clear that we're setting up the correct state here:
        // - If the target module changed, we are not updating the set of selectors on the target modules (both current and new)
    }

    public ComponentSelectionReasonInternal getSelectionReason() {
        // Create a component selection reason specific to this selector.
        return addReasonsForSelector(VersionSelectionReasons.empty());
    }

    ComponentSelectionReasonInternal addReasonsForSelector(ComponentSelectionReasonInternal selectionReason) {
        ComponentSelectionDescriptorInternal dependencyDescriptor = dependencyMetadata.isPending() ? CONSTRAINT : REQUESTED;
        if (dependencyMetadata.getReason() != null) {
            dependencyDescriptor = dependencyDescriptor.withReason(dependencyMetadata.getReason());
        }
        selectionReason.addCause(dependencyDescriptor);

        if (dependencyState.getRuleDescriptor() != null) {
            selectionReason.addCause(dependencyState.getRuleDescriptor());
        }
        return selectionReason;
    }

    public DependencyMetadata getDependencyMetadata() {
        return dependencyMetadata;
    }

    public ResolvedVersionConstraint getVersionConstraint() {
        return versionConstraint;
    }

    @Override
    public boolean isForce() {
        return forced;
    }

    private ComponentSelector selectorWithDesugaredAttributes(ComponentSelector selector) {
        return AttributeDesugaring.desugarSelector(selector, attributesFactory);
    }

    private static boolean isForced(DependencyMetadata dependencyMetadata) {
        return dependencyMetadata instanceof LocalOriginDependencyMetadata
            && ((LocalOriginDependencyMetadata) dependencyMetadata).isForce();
    }

    public void update(DependencyState dependencyState) {
        if (dependencyState != this.dependencyState) {
            forced |= isForced(dependencyState.getDependency());
        }
    }
}
