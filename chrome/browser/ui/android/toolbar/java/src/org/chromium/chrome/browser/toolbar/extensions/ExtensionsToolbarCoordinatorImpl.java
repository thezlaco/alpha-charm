// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.extensions;

import android.animation.Animator;
import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.chromium.base.CallbackUtils;
import org.chromium.base.lifetime.LifetimeAssert;
import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.build.annotations.ServiceImpl;
import org.chromium.chrome.browser.layouts.toolbar.ToolbarWidthConsumer;
import org.chromium.chrome.browser.preferences.Pref;
import org.chromium.chrome.browser.preferences.PrefServiceUtil;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabCreator;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.theme.ThemeColorProvider;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTask;
import org.chromium.chrome.browser.ui.browser_window.ChromeAndroidTaskFeature.InitInfo;
import org.chromium.chrome.browser.ui.extensions.ExtensionActionsBridge;
import org.chromium.chrome.browser.ui.extensions.ExtensionsToolbarBridge;
import org.chromium.chrome.browser.user_education.IphCommandBuilder;
import org.chromium.chrome.browser.user_education.UserEducationHelper;
import org.chromium.components.embedder_support.contextmenu.ContextMenuPopulatorFactory;
import org.chromium.components.feature_engagement.FeatureConstants;
import org.chromium.components.prefs.PrefChangeRegistrar;
import org.chromium.components.prefs.PrefService;
import org.chromium.components.user_prefs.UserPrefs;
import org.chromium.content_public.browser.selection.SelectionDropdownMenuDelegate;
import org.chromium.ui.base.ViewUtils;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.listmenu.ListMenuButton;
import org.chromium.ui.modaldialog.ModalDialogManager;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.modelutil.PropertyModelChangeProcessor;
import org.chromium.ui.widget.AnchoredPopupWindow.HorizontalOrientation;

import java.util.Collection;
import java.util.function.Consumer;

/** The implementation of {@link extensionsToolbarCoordinator}. */
@NullMarked
@ServiceImpl(ExtensionsToolbarCoordinator.class)
public class ExtensionsToolbarCoordinatorImpl
        implements ExtensionsToolbarCoordinator, ComponentCallbacks {
    private static final int COMPACT_WINDOW_THRESHOLD_DP = 600;
    private final @Nullable LifetimeAssert mLifetimeAssert = LifetimeAssert.create(this);

    // TODO(crbug.com/473396591): Remove once {link ExtensionActionsBridge} is deprecated.
    private ExtensionActionsBridge mBridge;

    private LinearLayout mContainer;
    private ExtensionsToolbarBridge mExtensionsToolbarBridge;
    private ExtensionActionListCoordinator mExtensionActionListCoordinator;
    private ExtensionsMenuCoordinator mExtensionsMenuCoordinator;
    private ExtensionAccessControlButtonCoordinator mExtensionAccessControlButtonCoordinator;
    private PropertyModel mToolbarModel;
    private PropertyModelChangeProcessor mMenuButtonChangeProcessor;

    private final MenuButtonWidthConsumer mMenuButtonWidthConsumer = new MenuButtonWidthConsumer();
    private final RequestAccessButtonWidthConsumer mRequestAccessButtonWidthConsumer =
            new RequestAccessButtonWidthConsumer();
    private final ActionListWidthConsumer mActionListWidthConsumer = new ActionListWidthConsumer();
    private final PoppedOutActionWidthConsumer mPoppedOutActionWidthConsumer =
            new PoppedOutActionWidthConsumer();

    private boolean mCanShowMenuIcon = true;
    private boolean mShowExtensionsMenuPending;
    private boolean mIsDestroyed;
    private final ExtensionsToolbarBridge.Observer mExtensionsToolbarBridgeObserver =
            new ExtensionsToolbarBridge.Observer() {
                @Override
                public void showManageExtensionsIPH() {
                    showIphInternal();
                }

                @Override
                public void showPinnedByDefaultIPH(String extensionId) {
                    showPinnedByDefaultIphInternal(extensionId);
                }
            };
    private final MenuButtonPinningDelegate mMenuButtonPinningDelegate =
            new MenuButtonPinningDelegate();
    private View.@Nullable OnLayoutChangeListener mLayoutChangeListener;
    private View.@Nullable mLayoutChangeAnchor;
    private boolean mWasWindowCompact;
    private WindowAndroid mWindowAndroid;
    private Profile mProfile;
    private PrefService mPrefService;
    private PrefChangeRegistrar mPrefChangeRegistrar;
    private @Nullable NullableObservableSupplier<Tab> mCurrentTabSupplier;
    private int mLastDensityDpi;
    private int mLastIconWidthPx;
    private @Nullable Runnable mOnFeatureRemoved;

    @Override
    public void initializeWithNative(ExtensionsToolbarInitParams params) {
        Context context = params.context;
        ChromeAndroidTask task = params.task;
        Profile profile = params.profile;
        WindowAndroid windowAndroid = params.windowAndroid;
        TabModelSelector tabModelSelector = params.tabModelSelector;
        ViewGroup rootView = params.rootView;
        NullableObservableSupplier<Tab> currentTabSupplier = params.currentTabSupplier;
        TabCreator tabCreator = params.tabCreator;
        ThemeColorProvider themeColorProvider = params.themeColorProvider;
        ModalDialogManager modalDialogManager = params.modalDialogManager;

        final ContextMenuPopulatorFactory contextMenuPopulatorFactory =
                params.contextMenuPopulatorFactory;
        final SelectionDropdownMenuDelegate selectionDropdownMenuDelegate =
                params.selectionDropdownMenuDelegate;

        mWindowAndroid = windowAndroid;
        mProfile = profile;
        mCurrentTabSupplier = params.currentTabSupplier;
        mOnFeatureRemoved = params.onFeatureRemoved;

        mBridge = new ExtensionActionsBridge(task, profile);

        params.extensionsToolbarStub.setLayoutResource(R.layout.extensions_toolbar_container);
        mContainer = (LinearLayout) params.extensionsToolbarStub.inflate();
        mExtensionsToolbarBridge = new ExtensionsToolbarBridge(task, profile);
        mExtensionsToolbarBridge.addObserver(mExtensionsToolbarBridgeObserver);

        mPrefService = UserPrefs.get(profile);

        mExtensionActionListCoordinator =
                new ExtensionActionListCoordinator(
                        new ExtensionActionListParams(
                                context,
                                windowAndroid,
                                task,
                                profile,
                                currentTabSupplier,
                                mExtensionsToolbarBridge,
                                contextMenuPopulatorFactory,
                                selectionDropdownMenuDelegate,
                                tabModelSelector,
                                modalDialogManager),
                        mContainer.findViewById(R.id.extension_action_list),
                        rootView);
        mToolbarModel = new PropertyModel.Builder(ExtensionsToolbarProperties.ALL_KEYS).build();
        mMenuButtonChangeProcessor =
                PropertyModelChangeProcessor.create(
                        mToolbarModel,
                        mContainer.findViewById(R.id.extensions_menu_button),
                        ExtensionsMenuButtonViewBinder::bind);

        mExtensionsMenuCoordinator =
                new ExtensionsMenuCoordinator(
                        new ExtensionsMenuParams(
                                context,
                                task,
                                profile,
                                currentTabSupplier,
                                tabCreator,
                                mExtensionsToolbarBridge),
                        mContainer.findViewById(R.id.extensions_menu_button),
                        themeColorProvider,
                        windowAndroid,
                        mMenuButtonPinningDelegate,
                        modalDialogManager);

        mExtensionAccessControlButtonCoordinator =
                new ExtensionAccessControlButtonCoordinator(
                        mToolbarModel,
                        currentTabSupplier,
                        mExtensionsToolbarBridge,
                        (TextView) mContainer.findViewById(R.id.extensions_request_access_button),
                        CallbackUtils.emptyCallback(),
                        () ->
                                mContainer.getResources().getConfiguration().screenWidthDp
                                        < COMPACT_WINDOW_THRESHOLD_DP);
        mPrefChangeRegistrar = PrefServiceUtil.createFor(profile);
        mPrefChangeRegistrar.addObserver(
                Pref.PIN_EXTENSIONS_MENU_BUTTON, this::updateMenuButtonPinState);
        context.registerComponentCallbacks(this);
        mWasWindowCompact =
                context.getResources().getConfiguration().screenWidthDp
                        < COMPACT_WINDOW_THRESHOLD_DP;
        mLastDensityDpi = context.getResources().getConfiguration().densityDpi;
        mLastIconWidthPx =
                context.getResources()
                        .getDimensionPixelSize(R.dimen.extension_action_icon_canvas_width);
    }

    @Override
    public void onAddedToTask(InitInfo initInfo) {
        // Usually the native side of a {@code ChromeAndroidTaskFeature} should be
        // initialized here (e.g., using {@code initInfo.nativeBrowserWindowPtr} to
        // obtain a native `BrowserWindowInterface` pointer).
        //
        // However, initializing {@code ExtensionsToolbarCoordinator} requires various
        // Activity and Toolbar UI dependencies (views, suppliers, dialog managers,
        // etc.) that are created and owned by {@code ToolbarManager} and are not
        // accessible via {@code InitInfo}. Therefore, initialization is performed
        // explicitly via {@code initializeWithNative()} when {@code ToolbarManager}
        // instantiates this feature, while its teardown lifecycle remains
        // managed by {@code ChromeAndroidTask}.
    }

    @Override
    public void onFeatureRemoved() {
        if (mOnFeatureRemoved != null) {
            mOnFeatureRemoved.run();
        }
        destroy();
    }

    @Override
    public void destroy() {
        if (mIsDestroyed) {
            return;
        }
        mIsDestroyed = true;

        if (mLayoutChangeListener != null && mLayoutChangeAnchor != null) {
            mLayoutChangeAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mLayoutChangeListener = null;
        mLayoutChangeAnchor = null;

        mMenuButtonChangeProcessor.destroy();

        if (mPrefChangeRegistrar != null) {
            mPrefChangeRegistrar.removeObserver(Pref.PIN_EXTENSIONS_MENU_BUTTON);
            mPrefChangeRegistrar.destroy();
        }
        if (mContainer != null && mContainer.getContext() != null) {
            mContainer.getContext().unregisterComponentCallbacks(this);
        }

        mExtensionsToolbarBridge.removeObserver(mExtensionsToolbarBridgeObserver);
        mExtensionAccessControlButtonCoordinator.destroy();
        mExtensionsMenuCoordinator.destroy();
        mExtensionActionListCoordinator.destroy();
        mExtensionsToolbarBridge.destroy();
        mBridge.destroy();
        mCurrentTabSupplier = null;

        LifetimeAssert.setSafeToGc(mLifetimeAssert, true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean isWindowCompact = newConfig.screenWidthDp < COMPACT_WINDOW_THRESHOLD_DP;
        if (isWindowCompact != mWasWindowCompact) {
            mWasWindowCompact = isWindowCompact;
            mExtensionAccessControlButtonCoordinator.requestVisibilityUpdate();
        }

        int densityDpi = newConfig.densityDpi;
        int iconWidthPx =
                mContainer
                        .getContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.extension_action_icon_canvas_width);
        if (densityDpi != mLastDensityDpi || iconWidthPx != mLastIconWidthPx) {
            mLastDensityDpi = densityDpi;
            mLastIconWidthPx = iconWidthPx;
            if (mCurrentTabSupplier != null) {
                Tab currentTab = mCurrentTabSupplier.get();
                if (currentTab != null && currentTab.getWebContents() != null) {
                    mExtensionActionListCoordinator.updateAllIcons(currentTab.getWebContents());
                }
            }
        }
        // Force layout refresh to pick up new dimensions if density changed.
        ViewUtils.requestLayout(
                mContainer, "ExtensionsToolbarCoordinatorImpl.onConfigurationChanged");
    }

    @Override
    public void onLowMemory() {}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Filter out events we are not interested in before calling into JNI.
        if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
            return false;
        }

        return mExtensionsToolbarBridge.handleKeyDownEvent(event);
    }

    @Override
    public void updateMenuButtonBackground(int backgroundResource) {
        mToolbarModel.set(
                ExtensionsToolbarProperties.EXTENSIONS_MENU_BUTTON_DEFAULT_BACKGROUND,
                backgroundResource);
    }

    @Override
    public void showExtensionsMenu() {
        mShowExtensionsMenuPending = true;
        updateMenuIconVisibility();

        ListMenuButton extensionsMenuButton = mContainer.findViewById(R.id.extensions_menu_button);
        assert extensionsMenuButton != null;

        // Post to click after the layout pass.
        extensionsMenuButton.post(
                () -> {
                    mShowExtensionsMenuPending = false;
                    extensionsMenuButton.performClick();
                });
    }

    private void showIphInternal() {
        if (mProfile.shutdownStarted()) return;

        Activity activity = mWindowAndroid.getActivity().get();
        if (activity == null) return;

        View anchorView = mContainer.findViewById(R.id.extensions_menu_button);
        if (anchorView == null) return;

        Handler handler = new Handler(Looper.getMainLooper());

        runWhenShown(
                anchorView,
                view ->
                        showIphInternalHelper(
                                activity, view, handler, manageExtensionsIphCommand(view)));
    }

    /**
     * Runs {@code action} as soon as {@code anchorView} is on screen.
     *
     * <p>A view that has not been laid out yet reports itself as not shown even though it is about
     * to appear, so the run waits for the next layout pass in that case. Both help bubbles in this
     * toolbar need this, and each carried its own copy of the deferral, with its own answer to what
     * should happen to a pending one on teardown: the menu bubble remembered its listener and
     * removed it, the pinned bubble forgot, so only one of the two could be cancelled.
     *
     * <p>At most one run is pending at a time: asking again replaces the previous one instead of
     * adding a second listener. The anchor is kept alongside the listener so that teardown detaches
     * from whichever view it was attached to, rather than looking one up by id and hoping it is the
     * right one.
     */
    private void runWhenShown(View anchorView, Consumer<View> action) {
        if (anchorView.isShown()) {
            action.accept(anchorView);
            return;
        }

        if (mLayoutChangeListener != null && mLayoutChangeAnchor != null) {
            mLayoutChangeAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mLayoutChangeAnchor = anchorView;
        mLayoutChangeListener =
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom) {
                        if (v.isShown()) {
                            v.removeOnLayoutChangeListener(this);
                            mLayoutChangeListener = null;
                            mLayoutChangeAnchor = null;
                            action.accept(v);
                        }
                    }
                };
        anchorView.addOnLayoutChangeListener(mLayoutChangeListener);
    }

    private void showIphInternalHelper(
            Activity activity, View anchorView, Handler handler, IphCommandBuilder command) {
        UserEducationHelper userEducationHelper =
                new UserEducationHelper(activity, mProfile, handler);

        userEducationHelper.requestShowIph(
                command.setAnchorView(anchorView)
                        .setPreferredHorizontalOrientation(
                                HorizontalOrientation.MAX_AVAILABLE_SPACE)
                        .setHorizontalOverlapAnchor(true)
                        .setRemoveArrow(true)
                        .setInsetRect(new Rect())
                        .build());
    }

    /** Builds the command for the bubble that points at the extensions menu button. */
    private static IphCommandBuilder manageExtensionsIphCommand(View anchorView) {
        return new IphCommandBuilder(
                anchorView.getContext().getResources(),
                FeatureConstants.IPH_EXTENSIONS_MANAGE_TOOLBAR_FEATURE,
                R.string.extensions_menu_manage_toolbar_iph,
                R.string.extensions_menu_manage_toolbar_iph);
    }

    /** Builds the command for the bubble that points at a pinned extension's button. */
    private static IphCommandBuilder pinnedByDefaultIphCommand(View anchorView) {
        return new IphCommandBuilder(
                anchorView.getContext().getResources(),
                FeatureConstants.IPH_EXTENSIONS_PINNED_BY_DEFAULT_FEATURE,
                R.string.extensions_pinned_by_default_iph_body,
                R.string.extensions_pinned_by_default_iph_body);
    }

    private void showPinnedByDefaultIphInternal(String extensionId) {
        if (mProfile.shutdownStarted()) {
            return;
        }

        Activity activity = mWindowAndroid.getActivity().get();
        if (activity == null) {
            return;
        }

        View anchorView = mExtensionActionListCoordinator.getButtonViewForId(extensionId);
        if (anchorView == null) {
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());

        runWhenShown(
                anchorView,
                view ->
                        showIphInternalHelper(
                                activity, view, handler, pinnedByDefaultIphCommand(view)));
    }


    private void saveMenuButtonPinState(boolean pinned) {
        mPrefService.setBoolean(Pref.PIN_EXTENSIONS_MENU_BUTTON, pinned);
        updateMenuButtonPinState();
    }

    private void updateMenuButtonPinState() {
        if (mExtensionsMenuCoordinator.isExtensionsMenuOpen()) {
            mExtensionsMenuCoordinator.setMenuButtonPinned(isMenuButtonPinned());
        }

        // Trigger layout and wait for the toolbar to provide us with width allocation.
        ViewUtils.requestLayout(
                mContainer, "ExtensionsToolbarCoordinatorImpl.updateMenuButtonPinState()");
    }

    private boolean isMenuButtonPinned() {
        if (mProfile.shutdownStarted()) {
            // TODO(crbug.com/459079170): This is to prevent tests from breaking. {@code
            // ExtensionsToolbarCoordinatorImpl} should ideally be destroyed following {@code
            // ChromeAndroidTask}'s destruction, and it is currently being worked on.
            return true;
        }
        return mPrefService.getBoolean(Pref.PIN_EXTENSIONS_MENU_BUTTON);
    }

    private void updateMenuIconVisibility() {
        int visibility = shouldShowMenuIcon() ? View.VISIBLE : View.GONE;
        mContainer.findViewById(R.id.extensions_menu_button).setVisibility(visibility);
    }

    private boolean shouldShowMenuIcon() {
        return mExtensionsMenuCoordinator.isExtensionsMenuOpen()
                || mShowExtensionsMenuPending
                || (mCanShowMenuIcon && isMenuButtonPinned());
    }

    public class MenuButtonPinningDelegate {
        void setMenuButtonPinned(boolean pinned) {
            ExtensionsToolbarCoordinatorImpl.this.saveMenuButtonPinState(pinned);
        }

        boolean isMenuButtonPinned() {
            return ExtensionsToolbarCoordinatorImpl.this.isMenuButtonPinned();
        }

        void requestLayoutWithViewUtils() {
            // Trigger layout and wait for the toolbar to provide us with width allocation.
            ViewUtils.requestLayout(
                    mContainer, "ExtensionsToolbarCoordinatorImpl.requestLayoutWithViewUtils()");
        }
    }

    @Override
    public ToolbarWidthConsumer getPoppedOutActionWidthConsumer() {
        return mPoppedOutActionWidthConsumer;
    }

    @Override
    public ToolbarWidthConsumer getMenuButtonWidthConsumer() {
        return mMenuButtonWidthConsumer;
    }

    @Override
    public ToolbarWidthConsumer getRequestAccessButtonWidthConsumer() {
        return mRequestAccessButtonWidthConsumer;
    }

    @Override
    public ToolbarWidthConsumer getActionListWidthConsumer() {
        return mActionListWidthConsumer;
    }

    /**
     * What the four toolbar width consumers have in common.
     *
     * <p>Each one answers whether its own control fits, so none of them ever wants the animated
     * form to behave differently from the plain one: the toolbar measures and lays out in one pass
     * and animates nothing here. That delegation was written out four times, along with the flag
     * three of them keep and report through {@link #hasSpaceToShow()}.
     */
    private abstract class BaseWidthConsumer implements ToolbarWidthConsumer {
        private boolean mHasSpaceToShow;

        @Override
        public boolean hasSpaceToShow() {
            return mHasSpaceToShow;
        }

        protected final void setHasSpaceToShow(boolean hasSpaceToShow) {
            mHasSpaceToShow = hasSpaceToShow;
        }

        @Override
        public int updateVisibilityWithAnimation(
                int availableWidth, Collection<Animator> animators) {
            return updateVisibility(availableWidth);
        }
    }

    private class PoppedOutActionWidthConsumer extends BaseWidthConsumer {
        @Override
        public boolean isVisible() {
            return mExtensionActionListCoordinator.hasPoppedOutAction();
        }

        @Override
        public int updateVisibility(int availableWidth) {
            // Do not update the UI here just yet. We will leave that to {@link
            // ActionListWidthConsumer}, which will be called but later because it has lower
            // priority.
            int width = mExtensionActionListCoordinator.setCanShowPoppedOutAction(availableWidth);
            setHasSpaceToShow(mExtensionActionListCoordinator.canShowPoppedOutAction());
            return width;
        }
    }

    private class RequestAccessButtonWidthConsumer extends BaseWidthConsumer {
        @Override
        public boolean isVisible() {
            return mToolbarModel.get(ExtensionsToolbarProperties.IS_REQUEST_ACCESS_BUTTON_VISIBLE);
        }

        private void setHasSpaceToShow(boolean hasSpaceToShow) {
            setHasSpaceToShow(hasSpaceToShow);
            int visibility = hasSpaceToShow ? View.VISIBLE : View.GONE;
            mContainer
                    .findViewById(R.id.extensions_request_access_button)
                    .setVisibility(visibility);
        }

        @Override
        public int updateVisibility(int availableWidth) {
            boolean isWindowCompact =
                    mContainer.getResources().getConfiguration().screenWidthDp
                            < COMPACT_WINDOW_THRESHOLD_DP;

            if (!isVisible()) {
                setHasSpaceToShow(false);
                return 0;
            }

            TextView requestAccessButton =
                    mContainer.findViewById(R.id.extensions_request_access_button);

            int heightSpec;
            if (mContainer.getHeight() > 0) {
                heightSpec =
                        View.MeasureSpec.makeMeasureSpec(
                                mContainer.getHeight(), View.MeasureSpec.EXACTLY);
            } else {
                int expectedHeight =
                        mContainer
                                .getResources()
                                .getDimensionPixelSize(R.dimen.toolbar_button_height);
                heightSpec =
                        View.MeasureSpec.makeMeasureSpec(expectedHeight, View.MeasureSpec.EXACTLY);
            }
            requestAccessButton.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), heightSpec);
            int buttonWidth = requestAccessButton.getMeasuredWidth();

            boolean hasSpaceToShow = !isWindowCompact && buttonWidth <= availableWidth;
            setHasSpaceToShow(hasSpaceToShow);

            return Math.min(availableWidth, buttonWidth);
        }
    }

    private class MenuButtonWidthConsumer extends BaseWidthConsumer {
        @Override
        public boolean isVisible() {
            // This return value is used to determine whether to show the icon row in the app menu.
            // Since the extensions menu should not affect that behavior, we always return true
            // here.
            return true;
        }

        @Override
        public boolean hasSpaceToShow() {
            return mCanShowMenuIcon;
        }

        @Override
        public int updateVisibility(int availableWidth) {
            int puzzleButtonWidth =
                    mContainer.getResources().getDimensionPixelSize(R.dimen.toolbar_button_width);
            mCanShowMenuIcon = puzzleButtonWidth <= availableWidth;

            updateMenuIconVisibility();

            return shouldShowMenuIcon() ? puzzleButtonWidth : 0;
        }
    }

    private class ActionListWidthConsumer extends BaseWidthConsumer {
        @Override
        public boolean isVisible() {
            return mContainer.findViewById(R.id.extension_action_list).getVisibility()
                    == View.VISIBLE;
        }

        @Override
        public int updateVisibility(int availableWidth) {
            int width = mExtensionActionListCoordinator.fitActionsWithinWidth(availableWidth);
            setHasSpaceToShow(width > 0);
            return width;
        }
    }
}
