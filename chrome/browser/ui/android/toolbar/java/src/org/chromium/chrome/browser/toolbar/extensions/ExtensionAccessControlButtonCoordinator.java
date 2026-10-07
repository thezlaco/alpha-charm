// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.widget.TextView;

import org.chromium.base.lifetime.Destroyable;
import org.chromium.build.annotations.NullMarked;
import org.chromium.ui.modelutil.PropertyModelChangeProcessor;

/**
 * Coordinator for the request access button. This class is responsible for the button that allows
 * extensions to request access to the current site.
 */
@NullMarked
public class ExtensionAccessControlButtonCoordinator implements Destroyable {
    private final PropertyModelChangeProcessor mChangeProcessor;
    private final ExtensionAccessControlButtonMediator mMediator;

    /**
     * Constructor.
     *
     * @param params The dependencies shared with the mediator.
     * @param requestAccessButton The button to request access to the current site.
     */
    public ExtensionAccessControlButtonCoordinator(
            ExtensionAccessControlButtonParams params, TextView requestAccessButton) {

        mChangeProcessor =
                PropertyModelChangeProcessor.create(
                        params.model,
                        requestAccessButton,
                        ExtensionAccessControlButtonViewBinder::bind);

        mMediator =
                new ExtensionAccessControlButtonMediator(
                        requestAccessButton.getContext(), params);
    }

    public void requestVisibilityUpdate() {
        mMediator.requestVisibilityUpdate();
    }

    @Override
    public void destroy() {
        mChangeProcessor.destroy();
        mMediator.destroy();
    }
}
