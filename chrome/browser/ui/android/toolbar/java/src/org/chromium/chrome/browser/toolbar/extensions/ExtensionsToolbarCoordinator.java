// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.view.KeyEvent;

import org.chromium.base.ServiceLoaderUtil;
import org.chromium.base.lifetime.Destroyable;
import org.chromium.build.annotations.Initializer;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.layouts.toolbar.ToolbarWidthConsumer;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTaskFeature;
import org.chromium.chrome.browser.ui.extensions.ExtensionUi;

import java.util.Map;

/**
 * The coordinator of the extension-related toolbar UI.
 *
 * <p>This interface is always compiled, while the rest of the extension UI code may not be compiled
 * if extensions are not enabled in the current build configuration. Any Java UI code that needs to
 * interact with the extension toolbar UI must go through this interface, instead of interacting
 * directly with any conditionally-compiled extension UI classes, to avoid ClassNotFoundException
 * when those classes are not compiled.
 *
 * <p>Call {@link #maybeCreate()} to instantiate an implementation of this interface ({@link
 * extensionsToolbarCoordinatorImpl}) when it is available.
 */
@NullMarked
public interface ExtensionsToolbarCoordinator extends ChromeAndroidTaskFeature, Destroyable {
    /** Instantiates the implementation if it is available. */
    @Nullable
    static ExtensionsToolbarCoordinator maybeCreate(ExtensionsToolbarInitParams params) {
        // Check if the extension UI is enabled first.
        if (!ExtensionUi.isEnabled(params.profile)) {
            return null;
        }

        ExtensionsToolbarCoordinator coordinator =
                ServiceLoaderUtil.maybeCreate(ExtensionsToolbarCoordinator.class);
        if (coordinator == null) {
            return null;
        }
        coordinator.initializeWithNative(params);
        return coordinator;
    }

    /**
     * Initializes the coordinator and inflates the UI.
     *
     * <p>This method must be called exactly once on initialization by {@link #maybeCreate()}. It is
     * illegal to call it multiple times. It is guaranteed to be called after native initialization.
     */
    @Initializer
    void initializeWithNative(ExtensionsToolbarInitParams params);

    /**
     * Dispatches the key event to trigger the corresponding extension action if any.
     *
     * @return Whether the event has been consumed.
     */
    boolean dispatchKeyEvent(KeyEvent event);

    /**
     * Updates the ripple background of the extensions menu button
     *
     * <p>This method is typically invoked when the toolbar's tab model changes, such as when
     * transitioning into incognito mode.
     */
    void updateMenuButtonBackground(int backgroundResource);

    /** Shows the extensions menu programmatically. */
    void showExtensionsMenu();

    /**
     * Returns every control of this toolbar that competes for toolbar width, keyed by the id the
     * toolbar ranks components by.
     *
     * <p>This is one method rather than one per control because the set of controls belongs to this
     * class alone. Four separate getters meant the knowledge existed in three places at once: here,
     * in the implementation, and again in whichever toolbar was registering them. Adding a control
     * meant editing all three, and a toolbar that forgot one simply did not arbitrate for it, with
     * nothing to fail.
     *
     * <p>Returning the whole set lets the toolbar install what it is given, so the two cannot drift
     * apart. The relative priority of these controls against the rest of the toolbar is not decided
     * here; it is the toolbar's ranking.
     */
    Map<Integer, ToolbarWidthConsumer> getWidthConsumers();

    /**
     * How the extensions menu pins and unpins its toolbar button.
     *
     * <p>The menu needs the toolbar to act on a pin, but it does not need to know how the toolbar
     * stores one. Declaring that here rather than inside the implementation means the menu depends
     * on this interface, which is the seam between a toolbar and an extension UI that may not be
     * compiled at all, instead of naming a class that exists only when it is.
     */
    interface MenuButtonPinningDelegate {
        /** Records the pinned state and re-reads the button. */
        void setMenuButtonPinned(boolean pinned);

        /** Returns whether the menu button is currently pinned. */
        boolean isMenuButtonPinned();

        /**
         * Asks for a layout pass, and with it width allocation.
         *
         * <p>Separate from {@link #setMenuButtonPinned(boolean)} because the toolbar lays the
         * button out when the popup closes, which is a different moment from the user choosing to
         * pin.
         */
        void requestLayoutWithViewUtils();
    }
}
