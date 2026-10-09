// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.components.browser_ui.bottomsheet;

import android.view.ViewGroup;
import android.view.Window;

import org.chromium.build.annotations.Nullable;
import org.chromium.components.browser_ui.desktop_windowing.DesktopWindowStateManager;
import org.chromium.components.browser_ui.widget.scrim.ScrimManager;
import org.chromium.ui.KeyboardVisibilityDelegate;
import org.chromium.ui.insets.InsetObserver;

import java.util.function.Supplier;

/**
 * Everything a {@link BottomSheetController} needs in order to attach itself to a window.
 *
 * <p>These were positional arguments on a factory method, and three of them were suppliers of
 * different types: a {@link ScrimManager}, a {@link ViewGroup} and an {@link Integer}. Generics
 * erase, so three suppliers in a row were the same type to the compiler and passing one where
 * another belonged compiled, then failed at runtime. Naming them settles which is which.
 */
public class BottomSheetParams {
    /**
     * @param scrimManagerSupplier Supplies the {@link ScrimManager}, used to show scrims behind
     *     the sheet.
     * @param window The activity's window.
     * @param keyboardDelegate A means of hiding the keyboard.
     * @param root The view that should contain the sheet.
     * @param insetObserver The {@link InsetObserver} for inset changes.
     */
    public Builder newBuilder(
            final Supplier</* @Nullable */ ScrimManager> scrimManagerSupplier,
            Window window,
            KeyboardVisibilityDelegate keyboardDelegate,
            Supplier<ViewGroup> root,
            InsetObserver insetObserver) {
        return new Builder(scrimManagerSupplier, window, keyboardDelegate, root, insetObserver);
    }

    Supplier</* @Nullable */ ScrimManager> getScrimManagerSupplier() {
        return mScrimManagerSupplier;
    }

    Window getWindow() {
        return mWindow;
    }

    KeyboardVisibilityDelegate getKeyboardDelegate() {
        return mKeyboardDelegate;
    }

    Supplier<ViewGroup> getRoot() {
        return mRoot;
    }

    Supplier<Integer> getEdgeToEdgeBottomInsetSupplier() {
        return mEdgeToEdgeBottomInsetSupplier;
    }

    @Nullable
    DesktopWindowStateManager getDesktopWindowStateManager() {
        return mDesktopWindowStateManager;
    }

    InsetObserver getInsetObserver() {
        return mInsetObserver;
    }

    boolean getEnableLargeFormFactorUi() {
        return mEnableLargeFormFactorUi;
    }

    private final Supplier</* @Nullable */ ScrimManager> mScrimManagerSupplier;
    private final Window mWindow;
    private final KeyboardVisibilityDelegate mKeyboardDelegate;
    private final Supplier<ViewGroup> mRoot;
    private final Supplier<Integer> mEdgeToEdgeBottomInsetSupplier;
    private final @Nullable DesktopWindowStateManager mDesktopWindowStateManager;
    private final InsetObserver mInsetObserver;
    private final boolean mEnableLargeFormFactorUi;

    private BottomSheetParams(Builder builder) {
        mScrimManagerSupplier = builder.mScrimManagerSupplier;
        mWindow = builder.mWindow;
        mKeyboardDelegate = builder.mKeyboardDelegate;
        mRoot = builder.mRoot;
        mEdgeToEdgeBottomInsetSupplier = builder.mEdgeToEdgeBottomInsetSupplier;
        mDesktopWindowStateManager = builder.mDesktopWindowStateManager;
        mInsetObserver = builder.mInsetObserver;
        mEnableLargeFormFactorUi = builder.mEnableLargeFormFactorUi;
    }

    /** A builder for {@link BottomSheetParams}. */
    public static class Builder {
        /**
         * @param scrimManagerSupplier Supplies the {@link ScrimManager}, used to show scrims behind
         *     the sheet.
         * @param window The activity's window.
         * @param keyboardDelegate A means of hiding the keyboard.
         * @param root The view that should contain the sheet.
         * @param insetObserver The {@link InsetObserver} for inset changes.
         */
        public Builder(
                final Supplier</* @Nullable */ ScrimManager> scrimManagerSupplier,
                Window window,
                KeyboardVisibilityDelegate keyboardDelegate,
                Supplier<ViewGroup> root,
                InsetObserver insetObserver) {
            mScrimManagerSupplier = scrimManagerSupplier;
            mWindow = window;
            mKeyboardDelegate = keyboardDelegate;
            mRoot = root;
            mInsetObserver = insetObserver;
        }

        private final Supplier</* @Nullable */ ScrimManager> mScrimManagerSupplier;
        private final Window mWindow;
        private final KeyboardVisibilityDelegate mKeyboardDelegate;
        private final Supplier<ViewGroup> mRoot;
        private final InsetObserver mInsetObserver;

        // A sheet that is not asked for edge to edge insets reports none, and a sheet in a window
        // with no desktop windowing available has none to report. Zero and null are therefore the
        // answers, rather than a decision every caller has to make.
        private Supplier<Integer> mEdgeToEdgeBottomInsetSupplier = () -> 0;
        private @Nullable DesktopWindowStateManager mDesktopWindowStateManager;

        // Nothing sets this yet. The factory that created full width sheets was removed as unused,
        // and the controller reads the flag once, to hand it to the sheet, which branches on it to
        // lay itself out. It is kept here so that the controller and the sheet keep agreeing on
        // what they were asked for, rather than one of them inventing an answer the other cannot
        // see.
        private boolean mEnableLargeFormFactorUi;

        /**
         * @param edgeToEdgeBottomInsetSupplier Supplier of bottom inset when e2e is on.
         */
        public Builder setEdgeToEdgeBottomInsetSupplier(
                Supplier<Integer> edgeToEdgeBottomInsetSupplier) {
            mEdgeToEdgeBottomInsetSupplier = edgeToEdgeBottomInsetSupplier;
            return this;
        }

        /**
         * @param desktopWindowStateManager The {@link DesktopWindowStateManager} for the app
         *     header.
         */
        public Builder setDesktopWindowStateManager(
                @Nullable DesktopWindowStateManager desktopWindowStateManager) {
            mDesktopWindowStateManager = desktopWindowStateManager;
            return this;
        }

        /**
         * @param enableLargeFormFactorUi Whether to use a different UI explicitly designed for
         *     bottom sheets when operating in a desktop or large form factor environment. Some
         *     implementations may want to opt out of this behavior.
         */
        public Builder setEnableLargeFormFactorUi(boolean enableLargeFormFactorUi) {
            mEnableLargeFormFactorUi = enableLargeFormFactorUi;
            return this;
        }

        public BottomSheetParams build() {
            return new BottomSheetParams(this);
        }
    }
}