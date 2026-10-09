// Copyright 2020 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.components.browser_ui.bottomsheet;

import org.chromium.base.Callback;
import org.chromium.build.annotations.NullUnmarked;
import org.chromium.ui.base.WindowAndroid;

/** A factory for producing a {@link BottomSheetController}. */
@NullUnmarked
public class BottomSheetControllerFactory {
    /**
     * @param params What the sheet is to be attached to, and how it is to behave.
     */
    public static ManagedBottomSheetController createBottomSheetController(
            BottomSheetParams params) {
        return new BottomSheetControllerImpl(
                params.getScrimManagerSupplier(),
                params.getWindow(),
                params.getKeyboardDelegate(),
                params.getRoot(),
                /* alwaysFullWidth= */ false,
                params.getEdgeToEdgeBottomInsetSupplier(),
                params.getDesktopWindowStateManager(),
                params.getInsetObserver(),
                params.getEnableLargeFormFactorUi());
    }

    // Redirect methods to provider to make them only accessible to classes that have access to the
    // factory.

    /**
     * Attach a shared {@link BottomSheetController} to a {@link WindowAndroid}.
     *
     * @param windowAndroid The window to attach the sheet's controller to.
     * @param controller The controller to attach.
     */
    public static void attach(
            WindowAndroid windowAndroid, ManagedBottomSheetController controller) {
        BottomSheetControllerProvider.attach(windowAndroid, controller);
    }

    /**
     * Detach the specified {@link BottomSheetController} from any {@link WindowAndroid}s it is
     * associated with.
     * @param controller The controller to remove from any associated windows.
     */
    public static void detach(ManagedBottomSheetController controller) {
        BottomSheetControllerProvider.detach(controller);
    }

    /** @param reporter A means of reporting an exception without crashing. */
    public static void setExceptionReporter(Callback<Throwable> reporter) {
        BottomSheet.setExceptionReporter(reporter);
    }
}
