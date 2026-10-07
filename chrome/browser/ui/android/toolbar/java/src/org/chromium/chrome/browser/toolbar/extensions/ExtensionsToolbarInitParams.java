// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewStub;

import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabCreator;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.theme.ThemeColorProvider;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTask;
import org.chromium.components.embedder_support.contextmenu.ContextMenuPopulatorFactory;
import org.chromium.content_public.browser.selection.SelectionDropdownMenuDelegate;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.modaldialog.ModalDialogManager;

/**
 * Everything {@link ExtensionsToolbarCoordinator} needs in order to be created and initialised.
 *
 * <p>This exists so the dependency list is written once rather than once per signature. The list
 * used to be repeated on {@code maybeCreate}, on {@code initializeWithNative}, on its
 * implementation, and again at each call site: adding a dependency meant editing four matching
 * lists, and swapping two dependencies of the same type compiled and ran without complaint.
 */
@NullMarked
public final class ExtensionsToolbarInitParams {
    public final Context context;
    public final ViewStub extensionsToolbarStub;
    public final WindowAndroid windowAndroid;
    public final ChromeAndroidTask task;
    public final Profile profile;
    public final NullableObservableSupplier<Tab> currentTabSupplier;
    public final TabCreator tabCreator;
    public final ThemeColorProvider themeColorProvider;
    public final ViewGroup rootView;
    public final @Nullable ContextMenuPopulatorFactory contextMenuPopulatorFactory;
    public final @Nullable SelectionDropdownMenuDelegate selectionDropdownMenuDelegate;
    public final TabModelSelector tabModelSelector;
    public final ModalDialogManager modalDialogManager;
    public final @Nullable Runnable onFeatureRemoved;

    public ExtensionsToolbarInitParams(
            Context context,
            ViewStub extensionsToolbarStub,
            WindowAndroid windowAndroid,
            ChromeAndroidTask task,
            Profile profile,
            NullableObservableSupplier<Tab> currentTabSupplier,
            TabCreator tabCreator,
            ThemeColorProvider themeColorProvider,
            ViewGroup rootView,
            @Nullable ContextMenuPopulatorFactory contextMenuPopulatorFactory,
            @Nullable SelectionDropdownMenuDelegate selectionDropdownMenuDelegate,
            TabModelSelector tabModelSelector,
            ModalDialogManager modalDialogManager,
            @Nullable Runnable onFeatureRemoved) {
        this.context = context;
        this.extensionsToolbarStub = extensionsToolbarStub;
        this.windowAndroid = windowAndroid;
        this.task = task;
        this.profile = profile;
        this.currentTabSupplier = currentTabSupplier;
        this.tabCreator = tabCreator;
        this.themeColorProvider = themeColorProvider;
        this.rootView = rootView;
        this.contextMenuPopulatorFactory = contextMenuPopulatorFactory;
        this.selectionDropdownMenuDelegate = selectionDropdownMenuDelegate;
        this.tabModelSelector = tabModelSelector;
        this.modalDialogManager = modalDialogManager;
        this.onFeatureRemoved = onFeatureRemoved;
    }
}