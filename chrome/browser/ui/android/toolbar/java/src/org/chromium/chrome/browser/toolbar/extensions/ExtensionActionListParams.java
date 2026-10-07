// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.content.Context;

import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTask;
import org.chromium.components.embedder_support.contextmenu.ContextMenuPopulatorFactory;
import org.chromium.content_public.browser.selection.SelectionDropdownMenuDelegate;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.modaldialog.ModalDialogManager;

/**
 * The dependencies that the action list coordinator and its mediator both need.
 *
 * <p>The mediator is created by the coordinator and by nobody else, so the two of them had the
 * same ten dependencies written side by side, with the coordinator passing all ten through one by
 * one. They are stated here once instead, which leaves each class carrying only what is genuinely
 * its own, and lets a dependency be added in two places rather than four.
 */
@NullMarked
public final class ExtensionActionListParams {
    public final Context context;
    public final WindowAndroid windowAndroid;
    public final ChromeAndroidTask task;
    public final Profile profile;
    public final NullableObservableSupplier<Tab> currentTabSupplier;
    public final ExtensionsToolbarBridge extensionsToolbarBridge;
    public final @Nullable ContextMenuPopulatorFactory contextMenuPopulatorFactory;
    public final @Nullable SelectionDropdownMenuDelegate selectionDropdownMenuDelegate;
    public final TabModelSelector tabModelSelector;
    public final ModalDialogManager modalDialogManager;

    public ExtensionActionListParams(
            Context context,
            WindowAndroid windowAndroid,
            ChromeAndroidTask task,
            Profile profile,
            NullableObservableSupplier<Tab> currentTabSupplier,
            ExtensionsToolbarBridge extensionsToolbarBridge,
            @Nullable ContextMenuPopulatorFactory contextMenuPopulatorFactory,
            @Nullable SelectionDropdownMenuDelegate selectionDropdownMenuDelegate,
            TabModelSelector tabModelSelector,
            ModalDialogManager modalDialogManager) {
        this.context = context;
        this.windowAndroid = windowAndroid;
        this.task = task;
        this.profile = profile;
        this.currentTabSupplier = currentTabSupplier;
        this.extensionsToolbarBridge = extensionsToolbarBridge;
        this.contextMenuPopulatorFactory = contextMenuPopulatorFactory;
        this.selectionDropdownMenuDelegate = selectionDropdownMenuDelegate;
        this.tabModelSelector = tabModelSelector;
        this.modalDialogManager = modalDialogManager;
    }
}