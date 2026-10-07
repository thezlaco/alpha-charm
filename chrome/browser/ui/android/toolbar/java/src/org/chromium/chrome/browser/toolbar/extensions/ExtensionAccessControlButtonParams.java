// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import org.chromium.base.Callback;
import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.ui.extensions.ExtensionsToolbarBridge;
import org.chromium.ui.modelutil.PropertyModel;

import java.util.function.Supplier;

/**
 * The dependencies shared by the request-access button coordinator and its mediator.
 *
 * <p>Five of the six coordinator arguments were forwarded unchanged to the mediator; the sixth,
 * the button view, stays with the coordinator because it is the mediator's source of context.
 */
@NullMarked
public final class ExtensionAccessControlButtonParams {
    public final PropertyModel model;
    public final NullableObservableSupplier<Tab> currentTabSupplier;
    public final ExtensionsToolbarBridge extensionsToolbarBridge;
    public final Callback<Boolean> visibilityObserver;
    public final Supplier<Boolean> isWindowCompactSupplier;

    public ExtensionAccessControlButtonParams(
            PropertyModel model,
            NullableObservableSupplier<Tab> currentTabSupplier,
            ExtensionsToolbarBridge extensionsToolbarBridge,
            Callback<Boolean> visibilityObserver,
            Supplier<Boolean> isWindowCompactSupplier) {
        this.model = model;
        this.currentTabSupplier = currentTabSupplier;
        this.extensionsToolbarBridge = extensionsToolbarBridge;
        this.visibilityObserver = visibilityObserver;
        this.isWindowCompactSupplier = isWindowCompactSupplier;
    }
}