package ch.deletescape.lawnchair.popup;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.deletescape.lawnchair.AbstractFloatingView;
import ch.deletescape.lawnchair.BubbleTextView;
import ch.deletescape.lawnchair.DragSource;
import ch.deletescape.lawnchair.DropTarget;
import ch.deletescape.lawnchair.FastBitmapDrawable;
import ch.deletescape.lawnchair.ItemInfo;
import ch.deletescape.lawnchair.Launcher;
import ch.deletescape.lawnchair.LauncherAnimUtils;
import ch.deletescape.lawnchair.LauncherModel;
import ch.deletescape.lawnchair.LogAccelerateInterpolator;
import ch.deletescape.lawnchair.R;
import ch.deletescape.lawnchair.Utilities;
import ch.deletescape.lawnchair.accessibility.LauncherAccessibilityDelegate;
import ch.deletescape.lawnchair.accessibility.ShortcutMenuAccessibilityDelegate;
import ch.deletescape.lawnchair.anim.PropertyListBuilder;
import ch.deletescape.lawnchair.anim.PropertyResetListener;
import ch.deletescape.lawnchair.badge.BadgeInfo;
import ch.deletescape.lawnchair.dragndrop.DragController;
import ch.deletescape.lawnchair.dragndrop.DragLayer;
import ch.deletescape.lawnchair.dragndrop.DragOptions;
import ch.deletescape.lawnchair.graphics.IconPalette;
import ch.deletescape.lawnchair.graphics.TriangleShape;
import ch.deletescape.lawnchair.notification.NotificationItemView;
import ch.deletescape.lawnchair.notification.NotificationKeyData;
import ch.deletescape.lawnchair.shortcuts.DeepShortcutManager;
import ch.deletescape.lawnchair.shortcuts.ShortcutsItemView;
import ch.deletescape.lawnchair.util.PackageUserKey;

public class PopupContainerWithArrow extends AbstractFloatingView implements DragSource, DragController.DragListener {
    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    private View mArrow;
    private boolean mDeferContainerRemoval;
    private PointF mInterceptTouchDown;
    protected boolean mIsAboveIcon;
    private boolean mIsLeftAligned;
    private final boolean mIsRtl;
    protected final Launcher mLauncher;
    private NotificationItemView mNotificationItemView;
    protected Animator mOpenCloseAnimator;
    protected BubbleTextView mOriginalIcon;
    private AnimatorSet mReduceHeightAnimatorSet;
    public ShortcutsItemView mShortcutsItemView;
    private final int mStartDragThreshold;
    private final Rect mTempRect;

    final class C04792 extends AnimatorListenerAdapter {
        C04792() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            PopupContainerWithArrow.this.mOpenCloseAnimator = null;
            Utilities.sendCustomAccessibilityEvent(PopupContainerWithArrow.this, 32, PopupContainerWithArrow.this.getContext().getString(R.string.action_deep_shortcut));
        }
    }

    final class C04803 implements DragOptions.PreDragCondition {
        C04803() {
        }

        @Override
        public boolean shouldStartDrag(double d) {
            return d > ((double) PopupContainerWithArrow.this.mStartDragThreshold);
        }

        @Override
        public void onPreDragStart(DropTarget.DragObject dragObject) {
            PopupContainerWithArrow.this.mOriginalIcon.setVisibility(4);
        }

        @Override
        public void onPreDragEnd(DropTarget.DragObject dragObject, boolean z) {
            if (!z) {
                PopupContainerWithArrow.this.mOriginalIcon.setVisibility(0);
                if (!PopupContainerWithArrow.this.mIsAboveIcon) {
                    PopupContainerWithArrow.this.mOriginalIcon.setTextVisibility(false);
                }
            }
        }
    }

    final class C04825 extends AnimatorListenerAdapter {
        C04825() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            PopupContainerWithArrow.this.removeView(PopupContainerWithArrow.this.mNotificationItemView);
            PopupContainerWithArrow.this.mNotificationItemView = null;
            if (PopupContainerWithArrow.this.getItemCount() == 0) {
                PopupContainerWithArrow.this.close(false);
            }
        }
    }

    final class C04858 extends AnimatorListenerAdapter {
        C04858() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            PopupContainerWithArrow.this.mOpenCloseAnimator = null;
            if (PopupContainerWithArrow.this.mDeferContainerRemoval) {
                PopupContainerWithArrow.this.setVisibility(4);
            } else {
                PopupContainerWithArrow.this.closeComplete();
            }
        }
    }

    public PopupContainerWithArrow(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mTempRect = new Rect();
        mInterceptTouchDown = new PointF();
        mLauncher = Launcher.getLauncher(context);
        mStartDragThreshold = getResources().getDimensionPixelSize(R.dimen.deep_shortcuts_start_drag_threshold);
        mAccessibilityDelegate = new ShortcutMenuAccessibilityDelegate(mLauncher);
        mIsRtl = Utilities.isRtl(getResources());
    }

    public PopupContainerWithArrow(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PopupContainerWithArrow(Context context) {
        this(context, null, 0);
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public static PopupContainerWithArrow showForIcon(BubbleTextView bubbleTextView) {
        Launcher launcher = Launcher.getLauncher(bubbleTextView.getContext());
        if (getOpen(launcher) != null) {
            bubbleTextView.clearFocus();
            return null;
        }
        ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
        if (!DeepShortcutManager.supportsShortcuts(itemInfo)) {
            return null;
        }
        PopupDataProvider popupDataProvider = launcher.getPopupDataProvider();
        List shortcutIdsForItem = popupDataProvider.getShortcutIdsForItem(itemInfo);
        List notificationKeysForItem = popupDataProvider.getNotificationKeysForItem(itemInfo);
        List enabledSystemShortcutsForItem = popupDataProvider.getEnabledSystemShortcutsForItem(itemInfo);
        PopupContainerWithArrow popupContainerWithArrow = (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(R.layout.popup_container, launcher.getDragLayer(), false);
        popupContainerWithArrow.setVisibility(INVISIBLE);
        launcher.getDragLayer().addView(popupContainerWithArrow);
        popupContainerWithArrow.populateAndShow(bubbleTextView, shortcutIdsForItem, notificationKeysForItem, enabledSystemShortcutsForItem);
        return popupContainerWithArrow;
    }

    public void populateAndShow(BubbleTextView bubbleTextView, List list, List list2, List list3) {
        List list4;
        List list5;
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.popup_arrow_width);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.popup_arrow_height);
        int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.popup_arrow_vertical_offset);
        mOriginalIcon = bubbleTextView;
        PopupPopulator.Item[] itemsToPopulate = PopupPopulator.getItemsToPopulate(list, list2, list3);
        addDummyViews(itemsToPopulate, list2.size() > 1);
        measure(0, 0);
        orientAboutIcon(bubbleTextView, dimensionPixelSize2 + dimensionPixelSize3);
        boolean z = mIsAboveIcon;
        if (z) {
            removeAllViews();
            mNotificationItemView = null;
            mShortcutsItemView = null;
            addDummyViews(PopupPopulator.reverseItems(itemsToPopulate), list2.size() > 1);
            measure(0, 0);
            orientAboutIcon(bubbleTextView, dimensionPixelSize2 + dimensionPixelSize3);
        }
        ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
        if (mShortcutsItemView == null) {
            list4 = Collections.EMPTY_LIST;
        } else {
            list4 = mShortcutsItemView.getDeepShortcutViews(z);
        }
        if (mShortcutsItemView == null) {
            list5 = Collections.EMPTY_LIST;
        } else {
            list5 = mShortcutsItemView.getSystemShortcutViews(z);
        }
        if (mNotificationItemView != null) {
            updateNotificationHeader();
        }
        int size = list4.size() + list5.size();
        if (list2.size() == 0) {
            setContentDescription(getContext().getString(R.string.shortcuts_menu_description, size, bubbleTextView.getContentDescription().toString()));
        } else {
            setContentDescription(getContext().getString(R.string.shortcuts_menu_with_notifications_description, new Object[]{Integer.valueOf(size), Integer.valueOf(list2.size()), bubbleTextView.getContentDescription().toString()}));
        }
        if (isAlignedWithStart()) {
            size = R.dimen.popup_arrow_horizontal_offset_start;
        } else {
            size = R.dimen.popup_arrow_horizontal_offset_end;
        }
        mArrow = addArrowView(resources.getDimensionPixelSize(size), dimensionPixelSize3, dimensionPixelSize, dimensionPixelSize2);
        mArrow.setPivotX((float) (dimensionPixelSize / 2));
        View view = mArrow;
        if (mIsAboveIcon) {
            size = 0;
        } else {
            size = dimensionPixelSize2;
        }
        view.setPivotY((float) size);
        animateOpen();
        mLauncher.getDragController().addDragListener(this);
        mOriginalIcon.forceHideBadge(true);
        new Handler(LauncherModel.getWorkerLooper()).postAtFrontOfQueue(PopupPopulator.createUpdateRunnable(mLauncher, itemInfo, new Handler(Looper.getMainLooper()), this, list, list4, list2, mNotificationItemView, list3, list5));
    }

    private void addDummyViews(PopupPopulator.Item[] itemArr, boolean z) {
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.popup_items_spacing);
        LayoutInflater layoutInflater = mLauncher.getLayoutInflater();
        int length = itemArr.length;
        for (int i = 0; i < length; i++) {
            PopupPopulator.Item item;
            PopupPopulator.Item item2 = itemArr[i];
            if (i < length - 1) {
                item = itemArr[i + 1];
            } else {
                item = null;
            }
            View inflate = layoutInflater.inflate(item2.layoutId, this, false);
            if (item2 == PopupPopulator.Item.NOTIFICATION) {
                int dimensionPixelSize2;
                mNotificationItemView = (NotificationItemView) inflate;
                if (z) {
                    dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.notification_footer_height);
                } else {
                    dimensionPixelSize2 = 0;
                }
                inflate.findViewById(R.id.footer).getLayoutParams().height = dimensionPixelSize2;
                mNotificationItemView.getMainView().setAccessibilityDelegate(mAccessibilityDelegate);
            } else if (item2 == PopupPopulator.Item.SHORTCUT) {
                inflate.setAccessibilityDelegate(mAccessibilityDelegate);
            }
            boolean b = item != null && item2.isShortcut ^ item.isShortcut;
            if (item2.isShortcut) {
                if (mShortcutsItemView == null) {
                    mShortcutsItemView = (ShortcutsItemView) layoutInflater.inflate(R.layout.shortcuts_item, this, false);
                    addView(mShortcutsItemView);
                }
                mShortcutsItemView.addShortcutView(inflate, item2);
                if (b) {
                    ((LayoutParams) mShortcutsItemView.getLayoutParams()).bottomMargin = dimensionPixelSize;
                }
            } else {
                addView(inflate);
                if (b) {
                    ((LayoutParams) inflate.getLayoutParams()).bottomMargin = dimensionPixelSize;
                }
            }
        }
    }

    protected PopupItemView getItemViewAt(int i) {
        if (!mIsAboveIcon) {
            i++;
        }
        return (PopupItemView) getChildAt(i);
    }

    protected int getItemCount() {
        return getChildCount() - 1;
    }

    private void animateOpen() {
        Animator ofFloat;
        setVisibility(VISIBLE);
        mIsOpen = true;
        AnimatorSet createAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        int itemCount = getItemCount();
        long integer = (long) getResources().getInteger(R.integer.config_deepShortcutOpenDuration);
        long integer2 = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
        long j = integer - integer2;
        long integer3 = (long) getResources().getInteger(R.integer.config_deepShortcutOpenStagger);
        TimeInterpolator logAccelerateInterpolator = new LogAccelerateInterpolator(100, 0);
        TimeInterpolator decelerateInterpolator = new DecelerateInterpolator();
        for (int i = 0; i < itemCount; i++) {
            PopupItemView itemViewAt = getItemViewAt(i);
            itemViewAt.setVisibility(INVISIBLE);
            itemViewAt.setAlpha(0.0f);
            Animator createOpenAnimation = itemViewAt.createOpenAnimation(mIsAboveIcon, mIsLeftAligned);
            final PopupItemView popupItemView = itemViewAt;
            createOpenAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    popupItemView.setVisibility(VISIBLE);
                }
            });
            createOpenAnimation.setDuration(integer);
            createOpenAnimation.setStartDelay(((long) (mIsAboveIcon ? (itemCount - i) - 1 : i)) * integer3);
            createOpenAnimation.setInterpolator(decelerateInterpolator);
            createAnimatorSet.play(createOpenAnimation);
            ofFloat = ObjectAnimator.ofFloat(itemViewAt, View.ALPHA, 1.0f);
            ofFloat.setInterpolator(logAccelerateInterpolator);
            ofFloat.setDuration(j);
            createAnimatorSet.play(ofFloat);
        }
        createAnimatorSet.addListener(new C04792());
        mArrow.setScaleX(0.0f);
        mArrow.setScaleY(0.0f);
        ofFloat = createArrowScaleAnim(1.0f).setDuration(integer2);
        ofFloat.setStartDelay(j);
        createAnimatorSet.play(ofFloat);
        mOpenCloseAnimator = createAnimatorSet;
        createAnimatorSet.start();
    }

    private void orientAboutIcon(BubbleTextView bubbleTextView, int i) {
        int i2;
        int i3;
        int width;
        Resources resources;
        int i4;
        int height;
        FrameLayout.LayoutParams layoutParams;
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight() + i;
        DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.getDescendantRectRelativeToSelf(bubbleTextView, mTempRect);
        Rect insets = dragLayer.getInsets();
        int paddingLeft = mTempRect.left + bubbleTextView.getPaddingLeft();
        int paddingRight = (mTempRect.right - measuredWidth) - bubbleTextView.getPaddingRight();
        Object obj = (paddingLeft + measuredWidth) + insets.left < dragLayer.getRight() - insets.right ? 1 : null;
        Object obj2 = paddingRight > dragLayer.getLeft() + insets.left ? 1 : null;
        if (obj != null) {
            if (!mIsRtl) {
                i2 = paddingLeft;
            } else if (obj2 == null) {
                i2 = paddingLeft;
            }
            mIsLeftAligned = i2 != paddingLeft;
            if (mIsRtl) {
                i3 = i2;
            } else {
                i3 = i2 - (dragLayer.getWidth() - measuredWidth);
            }
            width = (int) (((float) ((bubbleTextView.getWidth() - bubbleTextView.getTotalPaddingLeft()) - bubbleTextView.getTotalPaddingRight())) * bubbleTextView.getScaleX());
            resources = getResources();
            if (isAlignedWithStart()) {
                i2 = ((width / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_end);
            } else {
                i2 = ((width / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_icon_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_start);
            }
            if (!mIsLeftAligned) {
                i2 = -i2;
            }
            i4 = i3 + i2;
            height = bubbleTextView.getIcon().getBounds().height();
            i2 = (mTempRect.top + bubbleTextView.getPaddingTop()) - measuredHeight;
            mIsAboveIcon = i2 <= dragLayer.getTop() + insets.top;
            if (mIsAboveIcon) {
                i3 = (mTempRect.top + bubbleTextView.getPaddingTop()) + height;
            } else {
                i3 = i2;
            }
            if (mIsRtl) {
                i2 = i4 - insets.left;
            } else {
                i2 = insets.right + i4;
            }
            i4 = i3 - insets.top;
            if (i4 < dragLayer.getTop() || i4 + measuredHeight > dragLayer.getBottom()) {
                ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 16;
                i3 = (paddingLeft + width) - insets.left;
                i2 = (paddingRight - width) - insets.left;
                if (mIsRtl) {
                    if (i3 + measuredWidth >= dragLayer.getRight()) {
                        mIsLeftAligned = true;
                    } else {
                        mIsLeftAligned = false;
                        i3 = i2;
                    }
                } else if (i2 <= dragLayer.getLeft()) {
                    mIsLeftAligned = false;
                    i3 = i2;
                } else {
                    mIsLeftAligned = true;
                }
                mIsAboveIcon = true;
                i2 = i3;
            }
            if (i2 < dragLayer.getLeft() || i2 + measuredWidth > dragLayer.getRight()) {
                layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
                layoutParams.gravity |= 1;
            }
            i3 = ((FrameLayout.LayoutParams) getLayoutParams()).gravity;
            if (!Gravity.isHorizontal(i3)) {
                setX((float) i2);
            }
            if (!Gravity.isVertical(i3)) {
                setY((float) i4);
            }
        }
        i2 = paddingRight;
        if (i2 != paddingLeft) {
        }
        mIsLeftAligned = i2 != paddingLeft;
        if (mIsRtl) {
            i3 = i2;
        } else {
            i3 = i2 - (dragLayer.getWidth() - measuredWidth);
        }
        width = (int) (((float) ((bubbleTextView.getWidth() - bubbleTextView.getTotalPaddingLeft()) - bubbleTextView.getTotalPaddingRight())) * bubbleTextView.getScaleX());
        resources = getResources();
        if (isAlignedWithStart()) {
            i2 = ((width / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_end);
        } else {
            i2 = ((width / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_icon_size) / 2)) - resources.getDimensionPixelSize(R.dimen.popup_padding_start);
        }
        if (mIsLeftAligned) {
            i2 = -i2;
        }
        i4 = i3 + i2;
        height = bubbleTextView.getIcon().getBounds().height();
        i2 = (mTempRect.top + bubbleTextView.getPaddingTop()) - measuredHeight;
        if (i2 <= dragLayer.getTop() + insets.top) {
        }
        mIsAboveIcon = i2 <= dragLayer.getTop() + insets.top;
        if (mIsAboveIcon) {
            i3 = i2;
        } else {
            i3 = (mTempRect.top + bubbleTextView.getPaddingTop()) + height;
        }
        if (mIsRtl) {
            i2 = i4 - insets.left;
        } else {
            i2 = insets.right + i4;
        }
        i4 = i3 - insets.top;
        ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 16;
        i3 = (paddingLeft + width) - insets.left;
        i2 = (paddingRight - width) - insets.left;
        if (mIsRtl) {
            if (i2 <= dragLayer.getLeft()) {
                mIsLeftAligned = true;
            } else {
                mIsLeftAligned = false;
                i3 = i2;
            }
        } else if (i3 + measuredWidth >= dragLayer.getRight()) {
            mIsLeftAligned = false;
            i3 = i2;
        } else {
            mIsLeftAligned = true;
        }
        mIsAboveIcon = true;
        i2 = i3;
        layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        layoutParams.gravity |= 1;
        i3 = ((FrameLayout.LayoutParams) getLayoutParams()).gravity;
        if (Gravity.isHorizontal(i3)) {
            setX((float) i2);
        }
        if (!Gravity.isVertical(i3)) {
            setY((float) i4);
        }
    }

    private boolean isAlignedWithStart() {
        return mIsLeftAligned && !mIsRtl || !mIsLeftAligned && mIsRtl;
    }

    private View addArrowView(int i, int i2, int i3, int i4) {
        int i5 = 0;
        PopupContainerWithArrow.LayoutParams layoutParams = new LayoutParams(i3, i4);
        if (mIsLeftAligned) {
            layoutParams.gravity = Gravity.LEFT;
            layoutParams.leftMargin = i;
        } else {
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.rightMargin = i;
        }
        if (mIsAboveIcon) {
            layoutParams.topMargin = i2;
        } else {
            layoutParams.bottomMargin = i2;
        }
        View view = new View(getContext());
        if (Gravity.isVertical(((FrameLayout.LayoutParams) getLayoutParams()).gravity)) {
            view.setVisibility(INVISIBLE);
        } else {
            ShapeDrawable shapeDrawable = new ShapeDrawable(TriangleShape.create((float) i3, (float) i4, !mIsAboveIcon));
            Paint paint = shapeDrawable.getPaint();
            paint.setColor(((PopupItemView) getChildAt(mIsAboveIcon ? getChildCount() - 1 : 0)).getArrowColor(mIsAboveIcon));
            paint.setPathEffect(new CornerPathEffect((float) getResources().getDimensionPixelSize(R.dimen.popup_arrow_corner_radius)));
            view.setBackground(shapeDrawable);
            view.setElevation(getElevation());
        }
        if (mIsAboveIcon) {
            i5 = getChildCount();
        }
        addView(view, i5, layoutParams);
        return view;
    }

    @Override
    public View getExtendedTouchView() {
        return mOriginalIcon;
    }

    public DragOptions.PreDragCondition createPreDragCondition() {
        return new C04803();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        if (motionEvent.getAction() == 0) {
            mInterceptTouchDown.set(motionEvent.getX(), motionEvent.getY());
            return false;
        }
        if (Math.hypot((double) (mInterceptTouchDown.x - motionEvent.getX()), (double) (mInterceptTouchDown.y - motionEvent.getY())) > ((double) ViewConfiguration.get(getContext()).getScaledTouchSlop())) {
            z = true;
        }
        return z;
    }

    public void updateNotificationHeader(Set set) {
        if (set.contains(PackageUserKey.fromItemInfo((ItemInfo) mOriginalIcon.getTag()))) {
            updateNotificationHeader();
        }
    }

    private void updateNotificationHeader() {
        BadgeInfo badgeInfoForItem = mLauncher.getPopupDataProvider().getBadgeInfoForItem((ItemInfo) mOriginalIcon.getTag());
        if (mNotificationItemView != null && badgeInfoForItem != null) {
            IconPalette iconPalette;
            if (mOriginalIcon.getIcon() instanceof FastBitmapDrawable) {
                iconPalette = ((FastBitmapDrawable) mOriginalIcon.getIcon()).getIconPalette();
            } else {
                iconPalette = null;
            }
            mNotificationItemView.updateHeader(badgeInfoForItem.getNotificationCount(), iconPalette);
        }
    }

    public void trimNotifications(Map map) {
        if (mNotificationItemView != null) {
            BadgeInfo badgeInfo = (BadgeInfo) map.get(PackageUserKey.fromItemInfo((ItemInfo) mOriginalIcon.getTag()));
            if (badgeInfo == null || badgeInfo.getNotificationKeys().size() == 0) {
                final View itemViewAt;
                AnimatorSet createAnimatorSet = LauncherAnimUtils.createAnimatorSet();
                int integer = getResources().getInteger(R.integer.config_removeNotificationViewDuration);
                final int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.popup_items_spacing);
                createAnimatorSet.play(reduceNotificationViewHeight(mNotificationItemView.getHeightMinusFooter() + dimensionPixelSize, integer));
                if (mIsAboveIcon) {
                    itemViewAt = getItemViewAt(getItemCount() - 2);
                } else {
                    itemViewAt = mNotificationItemView;
                }
                if (itemViewAt != null) {
                    ValueAnimator duration = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f}).setDuration((long) integer);
                    duration.addUpdateListener(new AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            ((MarginLayoutParams) itemViewAt.getLayoutParams()).bottomMargin = (int) ((Float) valueAnimator.getAnimatedValue() * ((float) dimensionPixelSize));
                        }
                    });
                    createAnimatorSet.play(duration);
                }
                Animator duration2 = ObjectAnimator.ofFloat(mNotificationItemView, ALPHA, new float[]{0.0f}).setDuration((long) integer);
                duration2.addListener(new C04825());
                createAnimatorSet.play(duration2);
                long integer2 = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
                createArrowScaleAnim(0.0f).setDuration(integer2).setStartDelay(0);
                createArrowScaleAnim(1.0f).setDuration(integer2).setStartDelay((long) (((double) integer) - (((double) integer2) * 1.5d)));
                createAnimatorSet.playSequentially(duration2/*, r3*/);
                createAnimatorSet.start();
                return;
            }
            mNotificationItemView.trimNotifications(NotificationKeyData.extractKeysOnly(badgeInfo.getNotificationKeys()));
        }
    }

    @Override
    protected void onWidgetsBound() {
        if (mShortcutsItemView != null) {
            mShortcutsItemView.enableWidgetsIfExist(mOriginalIcon);
        }
    }

    private ObjectAnimator createArrowScaleAnim(float f) {
        return LauncherAnimUtils.ofPropertyValuesHolder(mArrow, new PropertyListBuilder().scale(f).build());
    }

    public Animator reduceNotificationViewHeight(int i, int i2) {
        if (mReduceHeightAnimatorSet != null) {
            mReduceHeightAnimatorSet.cancel();
        }
        final int i3 = mIsAboveIcon ? i : -i;
        mReduceHeightAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        mReduceHeightAnimatorSet.play(mNotificationItemView.animateHeightRemoval(i));
        AnimatorListener propertyResetListener = new PropertyResetListener(TRANSLATION_Y, 0.0f);
        for (int i4 = 0; i4 < getItemCount(); i4++) {
            PopupItemView itemViewAt = getItemViewAt(i4);
            if (mIsAboveIcon || itemViewAt != mNotificationItemView) {
                Animator duration = ObjectAnimator.ofFloat(itemViewAt, TRANSLATION_Y, new float[]{itemViewAt.getTranslationY() + ((float) i3)}).setDuration((long) i2);
                duration.addListener(propertyResetListener);
                mReduceHeightAnimatorSet.play(duration);
            }
        }
        mReduceHeightAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (PopupContainerWithArrow.this.mIsAboveIcon) {
                    PopupContainerWithArrow.this.setTranslationY(PopupContainerWithArrow.this.getTranslationY() + ((float) i3));
                }
                PopupContainerWithArrow.this.mReduceHeightAnimatorSet = null;
            }
        });
        return mReduceHeightAnimatorSet;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1.0f;
    }

    @Override
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z, boolean z2) {
        if (!z2) {
            dragObject.dragView.remove();
            mLauncher.showWorkspace(true);
            mLauncher.getDropTargetBar().onDragEnd();
        }
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        mDeferContainerRemoval = true;
        animateClose();
    }

    @Override
    public void onDragEnd() {
        if (!mIsOpen) {
            if (mOpenCloseAnimator != null) {
                mDeferContainerRemoval = false;
            } else if (mDeferContainerRemoval) {
                closeComplete();
            }
        }
    }

    @Override
    protected void handleClose(boolean z) {
        if (z) {
            animateClose();
        } else {
            closeComplete();
        }
    }

    protected void animateClose() {
        if (mIsOpen) {
            int i;
            if (mOpenCloseAnimator != null) {
                mOpenCloseAnimator.cancel();
            }
            mIsOpen = false;
            AnimatorSet createAnimatorSet = LauncherAnimUtils.createAnimatorSet();
            int itemCount = getItemCount();
            int i2 = 0;
            for (i = 0; i < itemCount; i++) {
                if (getItemViewAt(i).isOpenOrOpening()) {
                    i2++;
                }
            }
            long integer = (long) getResources().getInteger(R.integer.config_deepShortcutCloseDuration);
            long integer2 = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
            long integer3 = (long) getResources().getInteger(R.integer.config_deepShortcutCloseStagger);
            TimeInterpolator logAccelerateInterpolator = new LogAccelerateInterpolator(100, 0);
            i = mIsAboveIcon ? itemCount - i2 : 0;
            for (int i3 = i; i3 < i + i2; i3++) {
                PopupItemView itemViewAt = getItemViewAt(i3);
                Animator createCloseAnimation = itemViewAt.createCloseAnimation(mIsAboveIcon, mIsLeftAligned, integer);
                if (mIsAboveIcon) {
                    itemCount = i3 - i;
                } else {
                    itemCount = (i2 - i3) - 1;
                }
                createCloseAnimation.setStartDelay(((long) itemCount) * integer3);
                Animator ofFloat = ObjectAnimator.ofFloat(itemViewAt, View.ALPHA, 0.0f);
                ofFloat.setStartDelay((((long) itemCount) * integer3) + integer2);
                ofFloat.setDuration(integer - integer2);
                ofFloat.setInterpolator(logAccelerateInterpolator);
                createAnimatorSet.play(ofFloat);
                final PopupItemView popupItemView = itemViewAt;
                createCloseAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        popupItemView.setVisibility(INVISIBLE);
                    }
                });
                createAnimatorSet.play(createCloseAnimation);
            }
            Animator duration = createArrowScaleAnim(0.0f).setDuration(integer2);
            duration.setStartDelay(0);
            createAnimatorSet.play(duration);
            createAnimatorSet.addListener(new C04858());
            mOpenCloseAnimator = createAnimatorSet;
            createAnimatorSet.start();
            mOriginalIcon.forceHideBadge(false);
        }
    }

    protected void closeComplete() {
        if (mOpenCloseAnimator != null) {
            mOpenCloseAnimator.cancel();
            mOpenCloseAnimator = null;
        }
        mIsOpen = false;
        mDeferContainerRemoval = false;
        mOriginalIcon.setTextVisibility(!(((ItemInfo) mOriginalIcon.getTag()).container == -101));
        mOriginalIcon.forceHideBadge(false);
        mLauncher.getDragController().removeDragListener(this);
        mLauncher.getDragLayer().removeView(this);
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 2) != 0;
    }

    public static PopupContainerWithArrow getOpen(Launcher launcher) {
        return (PopupContainerWithArrow) AbstractFloatingView.getOpenView(launcher, 2);
    }

    @Override
    public int getLogContainerType() {
        return 9;
    }
}