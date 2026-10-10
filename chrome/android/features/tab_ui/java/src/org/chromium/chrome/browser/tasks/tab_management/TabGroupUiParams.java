// Copyright 2026 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.tasks.tab_management;

import android.app.Activity;
import android.view.ViewGroup;

import org.chromium.base.supplier.MonotonicObservableSupplier;
import org.chromium.base.supplier.NonNullObservableSupplier;
import org.chromium.base.supplier.OneshotSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.bookmarks.TabBookmarker;
import org.chromium.chrome.browser.browser_controls.BrowserControlsStateProvider;
import org.chromium.chrome.browser.data_sharing.DataSharingTabManager;
import org.chromium.chrome.browser.layouts.LayoutStateProvider;
import org.chromium.chrome.browser.share.ShareDelegate;
import org.chromium.chrome.browser.tab_ui.TabContentManager;
import org.chromium.chrome.browser.tabmodel.TabCreatorManager;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.theme.ThemeColorProvider;
import org.chromium.chrome.browser.undo_tab_close_snackbar.UndoBarThrottle;
import org.chromium.components.browser_ui.bottomsheet.BottomSheetController;
import org.chromium.components.browser_ui.widget.scrim.ScrimManager;

import java.util.function.Supplier;

/**
 * Everything {@link TabManagementDelegate#createTabGroupUi} needs, named.
 *
 * <p>These were sixteen positional arguments, written out four times: in the interface, in the
 * implementation, in the constructor of the supplier that passes them on, and in the call that
 * builds that supplier. Every type in the list is distinct, so a swap would not compile, which is
 * the one thing that made the list tolerable. It does not make it readable: sixteen arguments is
 * past what anyone holds in their head, and the prose describing them ran to seventeen lines of
 * javadoc for a single method.
 *
 * <p>The cost that actually bites is the next one. Adding a parameter means editing four lists that
 * have to agree with one another and one that may not, and nothing in the compiler says which of
 * the five was forgotten.
 */
@NullMarked
public class TabGroupUiParams {
    /** The activity that creates the surface. */
    public final Activity activity;
    /** The parent view of the UI. */
    public final ViewGroup parentView;
    /** The state of the top browser controls. */
    public final BrowserControlsStateProvider browserControlsStateProvider;
    /** Controls the scrim shown behind the surface. */
    public final ScrimManager scrimManager;
    /** Access to the focus state of the omnibox. */
    public final NonNullObservableSupplier<Boolean> omniboxFocusStateSupplier;
    /** The bottom sheet controller of the current activity. */
    public final BottomSheetController bottomSheetController;
    /** Manages communication between the UI and the data sharing services. */
    public final DataSharingTabManager dataSharingTabManager;
    /** Gives access to the current set of tabs. */
    public final TabModelSelector tabModelSelector;
    /** Gives access to the tab content. */
    public final TabContentManager tabContentManager;
    /** Manages creation of tabs. */
    public final TabCreatorManager tabCreatorManager;
    /** Supplies the layout state. */
    public final OneshotSupplier<LayoutStateProvider> layoutStateProviderSupplier;
    /** Used to show confirmation dialogs. */
    public final org.chromium.ui.modaldialog.ModalDialogManager modalDialogManager;
    /** Used to provide the theme. */
    public final ThemeColorProvider themeColorProvider;
    /** Used to suppress the undo bar. */
    public final UndoBarThrottle undoBarThrottle;
    /** Supplies the bookmarker used to bookmark a given tab. */
    public final MonotonicObservableSupplier<TabBookmarker> tabBookmarkerSupplier;
    /** Supplies the delegate used to share a tab's URL. */
    public final Supplier</* @Nullable */ ShareDelegate> shareDelegateSupplier;

    public TabGroupUiParams(
            Activity activity,
            ViewGroup parentView,
            BrowserControlsStateProvider browserControlsStateProvider,
            ScrimManager scrimManager,
            NonNullObservableSupplier<Boolean> omniboxFocusStateSupplier,
            BottomSheetController bottomSheetController,
            DataSharingTabManager dataSharingTabManager,
            TabModelSelector tabModelSelector,
            TabContentManager tabContentManager,
            TabCreatorManager tabCreatorManager,
            OneshotSupplier<LayoutStateProvider> layoutStateProviderSupplier,
            org.chromium.ui.modaldialog.ModalDialogManager modalDialogManager,
            ThemeColorProvider themeColorProvider,
            UndoBarThrottle undoBarThrottle,
            MonotonicObservableSupplier<TabBookmarker> tabBookmarkerSupplier,
            Supplier</* @Nullable */ ShareDelegate> shareDelegateSupplier) {
        this.activity = activity;
        this.parentView = parentView;
        this.browserControlsStateProvider = browserControlsStateProvider;
        this.scrimManager = scrimManager;
        this.omniboxFocusStateSupplier = omniboxFocusStateSupplier;
        this.bottomSheetController = bottomSheetController;
        this.dataSharingTabManager = dataSharingTabManager;
        this.tabModelSelector = tabModelSelector;
        this.tabContentManager = tabContentManager;
        this.tabCreatorManager = tabCreatorManager;
        this.layoutStateProviderSupplier = layoutStateProviderSupplier;
        this.modalDialogManager = modalDialogManager;
        this.themeColorProvider = themeColorProvider;
        this.undoBarThrottle = undoBarThrottle;
        this.tabBookmarkerSupplier = tabBookmarkerSupplier;
        this.shareDelegateSupplier = shareDelegateSupplier;
    }
}