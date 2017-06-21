package ch.deletescape.lawnchair.shortcuts;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.deletescape.lawnchair.BubbleTextView;
import ch.deletescape.lawnchair.ItemInfo;
import ch.deletescape.lawnchair.Launcher;
import ch.deletescape.lawnchair.LauncherAnimUtils;
import ch.deletescape.lawnchair.R;
import ch.deletescape.lawnchair.anim.PropertyListBuilder;
import ch.deletescape.lawnchair.popup.PopupContainerWithArrow;
import ch.deletescape.lawnchair.popup.PopupItemView;
import ch.deletescape.lawnchair.popup.PopupPopulator;
import ch.deletescape.lawnchair.popup.SystemShortcut;

public class ShortcutsItemView extends PopupItemView implements OnLongClickListener, OnTouchListener {
    private final List<DeepShortcutView> mDeepShortcutViews;
    private final Point mIconLastTouchPos;
    private final Point mIconShift;
    private Launcher mLauncher;
    private LinearLayout mShortcutsLayout;
    private LinearLayout mSystemShortcutIcons;
    private final List<View> mSystemShortcutViews;

    public ShortcutsItemView(Context context) {
        this(context, null, 0);
    }

    public ShortcutsItemView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ShortcutsItemView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mIconShift = new Point();
        mIconLastTouchPos = new Point();
        mDeepShortcutViews = new ArrayList<>();
        mSystemShortcutViews = new ArrayList<>();
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mShortcutsLayout = (LinearLayout) findViewById(R.id.deep_shortcuts);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case 0:
            case 2:
                mIconLastTouchPos.set((int) motionEvent.getX(), (int) motionEvent.getY());
                break;
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Override
    public boolean onLongClick(android.view.View r9) {
        /*
        r8 = this;
        r7 = 0;
        r0 = r9.isInTouchMode();
        if (r0 == 0) goto L_0x0011;
    L_0x0007:
        r0 = r9.getParent();
        r0 = r0 instanceof com.android.launcher3.shortcuts.DeepShortcutView;
        r0 = r0 ^ 1;
        if (r0 == 0) goto L_0x0012;
    L_0x0011:
        return r7;
    L_0x0012:
        r0 = r8.mLauncher;
        r0 = r0.isDraggingEnabled();
        if (r0 != 0) goto L_0x001b;
    L_0x001a:
        return r7;
    L_0x001b:
        r0 = r8.mLauncher;
        r0 = r0.getDragController();
        r0 = r0.isDragging();
        if (r0 == 0) goto L_0x0028;
    L_0x0027:
        return r7;
    L_0x0028:
        r0 = r9.getParent();
        r5 = r0;
        r5 = (com.android.launcher3.shortcuts.DeepShortcutView) r5;
        r5.setWillDrawIcon(r7);
        r0 = r8.mIconShift;
        r1 = r8.mIconLastTouchPos;
        r1 = r1.x;
        r2 = r5.getIconCenter();
        r2 = r2.x;
        r1 = r1 - r2;
        r0.x = r1;
        r0 = r8.mIconShift;
        r1 = r8.mIconLastTouchPos;
        r1 = r1.y;
        r2 = r8.mLauncher;
        r2 = r2.getDeviceProfile();
        r2 = r2.iconSizePx;
        r1 = r1 - r2;
        r0.y = r1;
        r0 = r8.mLauncher;
        r0 = r0.getWorkspace();
        r1 = r5.getIconView();
        r2 = r8.getParent();
        r2 = (com.android.launcher3.popup.PopupContainerWithArrow) r2;
        r3 = r5.getFinalInfo();
        r4 = new com.android.launcher3.shortcuts.ShortcutDragPreviewProvider;
        r5 = r5.getIconView();
        r6 = r8.mIconShift;
        r4.<init>(r5, r6);
        r5 = new com.android.launcher3.dragndrop.DragOptions;
        r5.<init>();
        r0 = r0.beginDragShared(r1, r2, r3, r4, r5);
        r1 = r8.mIconShift;
        r1 = r1.x;
        r1 = -r1;
        r2 = r8.mIconShift;
        r2 = r2.y;
        r2 = -r2;
        r0.animateShift(r1, r2);
        r0 = r8.mLauncher;
        r1 = 1;
        com.android.launcher3.AbstractFloatingView.closeOpenContainer(r0, r1);
        return r7;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.shortcuts.ShortcutsItemView.onLongClick(android.view.View):boolean");
    }

    public void addShortcutView(View view, PopupPopulator.Item item) {
        addShortcutView(view, item, -1);
    }

    private void addShortcutView(View view, PopupPopulator.Item item, int i) {
        if (item == PopupPopulator.Item.SHORTCUT) {
            mDeepShortcutViews.add((DeepShortcutView) view);
        } else {
            mSystemShortcutViews.add(view);
        }
        if (item == PopupPopulator.Item.SYSTEM_SHORTCUT_ICON) {
            if (mSystemShortcutIcons == null) {
                mSystemShortcutIcons = (LinearLayout) mLauncher.getLayoutInflater().inflate(R.layout.system_shortcut_icons, mShortcutsLayout, false);
                mShortcutsLayout.addView(mSystemShortcutIcons, 0);
            }
            mSystemShortcutIcons.addView(view, i);
            return;
        }
        if (mShortcutsLayout.getChildCount() > 0) {
            View childAt = mShortcutsLayout.getChildAt(mShortcutsLayout.getChildCount() - 1);
            if (childAt instanceof DeepShortcutView) {
                childAt.findViewById(R.id.divider).setVisibility(View.VISIBLE);
            }
        }
        mShortcutsLayout.addView(view, i);
    }

    public List<DeepShortcutView> getDeepShortcutViews(boolean z) {
        if (z) {
            Collections.reverse(mDeepShortcutViews);
        }
        return mDeepShortcutViews;
    }

    public List<View> getSystemShortcutViews(boolean z) {
        if (z || mSystemShortcutIcons != null) {
            Collections.reverse(mSystemShortcutViews);
        }
        return mSystemShortcutViews;
    }

    public void enableWidgetsIfExist(BubbleTextView bubbleTextView) {
        PopupPopulator.Item item;
        ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
        SystemShortcut widgets = new SystemShortcut.Widgets();
        OnClickListener onClickListener = widgets.getOnClickListener(mLauncher, itemInfo);
        for (View view : mSystemShortcutViews) {
            if (view.getTag() instanceof SystemShortcut.Widgets) {
                break;
            }
        }
        View view2 = null;
        if (mSystemShortcutIcons == null) {
            item = PopupPopulator.Item.SYSTEM_SHORTCUT;
        } else {
            item = PopupPopulator.Item.SYSTEM_SHORTCUT_ICON;
        }
        if (onClickListener != null && view2 == null) {
            view2 = mLauncher.getLayoutInflater().inflate(item.layoutId, this, false);
            PopupPopulator.initializeSystemShortcut(getContext(), view2, widgets);
            view2.setOnClickListener(onClickListener);
            if (item == PopupPopulator.Item.SYSTEM_SHORTCUT_ICON) {
                addShortcutView(view2, item, 0);
                return;
            }
            ((PopupContainerWithArrow) getParent()).close(false);
            PopupContainerWithArrow.showForIcon(bubbleTextView);
        } else if (onClickListener == null && view2 != null) {
            if (item == PopupPopulator.Item.SYSTEM_SHORTCUT_ICON) {
                mSystemShortcutViews.remove(view2);
                mSystemShortcutIcons.removeView(view2);
                return;
            }
            ((PopupContainerWithArrow) getParent()).close(false);
            PopupContainerWithArrow.showForIcon(bubbleTextView);
        }
    }

    @Override
    public Animator createOpenAnimation(boolean z, boolean z2) {
        AnimatorSet createAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        createAnimatorSet.play(super.createOpenAnimation(z, z2));
        for (int i = 0; i < mShortcutsLayout.getChildCount(); i++) {
            if (mShortcutsLayout.getChildAt(i) instanceof DeepShortcutView) {
                View iconView = ((DeepShortcutView) mShortcutsLayout.getChildAt(i)).getIconView();
                iconView.setScaleX(0.0f);
                iconView.setScaleY(0.0f);
                createAnimatorSet.play(LauncherAnimUtils.ofPropertyValuesHolder(iconView, new PropertyListBuilder().scale(1.0f).build()));
            }
        }
        return createAnimatorSet;
    }

    @Override
    public Animator createCloseAnimation(boolean z, boolean z2, long j) {
        AnimatorSet createAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        createAnimatorSet.play(super.createCloseAnimation(z, z2, j));
        for (int i = 0; i < mShortcutsLayout.getChildCount(); i++) {
            if (mShortcutsLayout.getChildAt(i) instanceof DeepShortcutView) {
                View iconView = ((DeepShortcutView) mShortcutsLayout.getChildAt(i)).getIconView();
                iconView.setScaleX(1.0f);
                iconView.setScaleY(1.0f);
                createAnimatorSet.play(LauncherAnimUtils.ofPropertyValuesHolder(iconView, new PropertyListBuilder().scale(0.0f).build()));
            }
        }
        return createAnimatorSet;
    }

    @Override
    public int getArrowColor(boolean z) {
        int i;
        Context context = getContext();
        if (z || mSystemShortcutIcons == null) {
            i = R.color.popup_background_color;
        } else {
            i = R.color.popup_header_background_color;
        }
        return ContextCompat.getColor(context, i);
    }
}