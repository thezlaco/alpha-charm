// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.content.Context;

import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabCreator;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTask;
import org.chromium.chrome.browser.ui.extensions.ExtensionsToolbarBridge;

/**
 * The dependencies that the extensions menu coordinator and its mediator both need.
 *
 * <p>The mediator is created by the coordinator and by nobody else, yet the two of them named the
 * same six dependencies side by side, and the coordinator passed each one along on its own. Stating
 * them once leaves the coordinator with only what is its own: the menu button, the theme
 * colours, the
 * window and the pinning delegate.
 */
@NullMarked
public final class ExtensionsMenuParams {
    public final Context context;
    public final ChromeAndroidTask task;
    public final Profile profile;
    public final NullableObservableSupplier<Tab> currentTabSupplier;
    public final TabCreator tabCreator;
    public final ExtensionsToolbarBridge toolbarBridge;

    public ExtensionsMenuParams(
            Context context,
            ChromeAndroidTask task,
            Profile profile,
            NullableObservableSupplier<Tab> currentTabSupplier,
            TabCreator tabCreator,
            ExtensionsToolbarBridge toolbarBridge) {
        this.context = context;
        this.task = task;
        this.profile = profile;
        this.currentTabSupplier = currentTabSupplier;
        this.tabCreator = tabCreator;
        this.toolbarBridge = toolbarBridge;
    }
}