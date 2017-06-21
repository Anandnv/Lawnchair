/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.deletescape.lawnchair.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.deletescape.lawnchair.LauncherAppState;
import ch.deletescape.lawnchair.R;
import ch.deletescape.lawnchair.WidgetPreviewLoader;
import ch.deletescape.lawnchair.compat.AlphabeticIndexCompat;
import ch.deletescape.lawnchair.model.PackageItemInfo;
import ch.deletescape.lawnchair.model.WidgetItem;
import ch.deletescape.lawnchair.model.WidgetsModel;
import ch.deletescape.lawnchair.util.LabelComparator;
import ch.deletescape.lawnchair.util.MultiHashMap;
import ch.deletescape.lawnchair.util.PackageUserKey;

/**
 * List view adapter for the widget tray.
 * <p>
 * <p>Memory vs. Performance:
 * The less number of types of views are inserted into a {@link RecyclerView}, the more recycling
 * happens and less memory is consumed. {@link #getItemViewType} was not overridden as there is
 * only a single type of view.
 */
public class WidgetsListAdapter extends Adapter<WidgetsRowViewHolder> {

    private final ArrayList<WidgetListRowEntry> mEntries = new ArrayList<>();

    private final WidgetPreviewLoader mWidgetPreviewLoader;
    private final LayoutInflater mLayoutInflater;
    private final AlphabeticIndexCompat mIndexer;

    private final View.OnClickListener mIconClickListener;
    private final View.OnLongClickListener mIconLongClickListener;

    private WidgetsModel mWidgetsModel;

    private final int mIndent;

    public WidgetsListAdapter(View.OnClickListener iconClickListener,
                              View.OnLongClickListener iconLongClickListener,
                              Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        mIndexer = new AlphabeticIndexCompat(context);
        mIconClickListener = iconClickListener;
        mIconLongClickListener = iconLongClickListener;
        mIndent = context.getResources().getDimensionPixelSize(R.dimen.widget_section_indent);
    }

    public void setWidgets(MultiHashMap<PackageItemInfo, ArrayList<WidgetListRowEntry>> w) {
        this.mEntries.clear();
        Comparator widgetItemComparator = new WidgetItemComparator();
        for (Map.Entry entry : w.entrySet()) {
            WidgetListRowEntry widgetListRowEntry = new WidgetListRowEntry((PackageItemInfo) entry.getKey(), (ArrayList) entry.getValue());
            widgetListRowEntry.titleSectionName = this.mIndexer.computeSectionName(widgetListRowEntry.pkgItem.title);
            Collections.sort(widgetListRowEntry.widgets, widgetItemComparator);
            this.mEntries.add(widgetListRowEntry);
        }
        Collections.sort(this.mEntries, new WidgetListRowEntryComparator());
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    public List copyWidgetsForPackageUser(PackageUserKey packageUserKey) {
        for (WidgetListRowEntry widgetListRowEntry : mEntries) {
            if (widgetListRowEntry.pkgItem.packageName.equals(packageUserKey.mPackageName)) {
                ArrayList arrayList = new ArrayList(widgetListRowEntry.widgets);
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    if (!((WidgetItem) it.next()).user.equals(packageUserKey.mUser)) {
                        it.remove();
                    }
                }
                if (arrayList.isEmpty()) {
                    arrayList = null;
                }
                return arrayList;
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(WidgetsRowViewHolder holder, int pos) {
        WidgetListRowEntry widgetListRowEntry = mEntries.get(pos);
        List list = widgetListRowEntry.widgets;
        ViewGroup viewGroup = holder.cellContainer;
        int max = Math.max(0, list.size() - 1) + list.size();
        int childCount = viewGroup.getChildCount();
        if (max > childCount) {
            while (childCount < max) {
                if ((childCount & 1) == 1) {
                    this.mLayoutInflater.inflate(R.layout.widget_list_divider, viewGroup);
                } else {
                    WidgetCell widgetCell = (WidgetCell) this.mLayoutInflater.inflate(R.layout.widget_cell, viewGroup, false);
                    widgetCell.setOnClickListener(this.mIconClickListener);
                    widgetCell.setOnLongClickListener(this.mIconLongClickListener);
                    viewGroup.addView(widgetCell);
                }
                childCount++;
            }
        } else if (max < childCount) {
            for (int i2 = max; i2 < childCount; i2++) {
                viewGroup.getChildAt(i2).setVisibility(8);
            }
        }
        holder.title.applyFromPackageItemInfo(widgetListRowEntry.pkgItem);
        for (max = 0; max < list.size(); max++) {
            WidgetCell widgetCell2 = (WidgetCell) viewGroup.getChildAt(max * 2);
            widgetCell2.applyFromCellItem((WidgetItem) list.get(max), this.mWidgetPreviewLoader);
            widgetCell2.ensurePreview();
            widgetCell2.setVisibility(View.VISIBLE);
            if (max > 0) {
                viewGroup.getChildAt((max * 2) - 1).setVisibility(0);
            }
        }
    }

    @Override
    public WidgetsRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ViewGroup container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.widgets_list_row_view, parent, false);
        LinearLayout cellList = (LinearLayout) container.findViewById(R.id.widgets_cell_list);

        // if the end padding is 0, then container view (horizontal scroll view) doesn't respect
        // the end of the linear layout width + the start padding and doesn't allow scrolling.
        cellList.setPaddingRelative(mIndent, 0, 1, 0);

        return new WidgetsRowViewHolder(container);
    }

    @Override
    public void onViewRecycled(WidgetsRowViewHolder holder) {
        int childCount = holder.cellContainer.getChildCount();
        for (int i = 0; i < childCount; i += 2) {
            ((WidgetCell) holder.cellContainer.getChildAt(i)).clear();
        }
    }

    @Override
    public boolean onFailedToRecycleView(WidgetsRowViewHolder holder) {
        // If child views are animating, then the RecyclerView may choose not to recycle the view,
        // causing extraneous onCreateViewHolder() calls.  It is safe in this case to continue
        // recycling this view, and take care in onViewRecycled() to cancel any existing
        // animations.
        return true;
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    public class WidgetListRowEntryComparator implements Comparator<WidgetListRowEntry> {
        private final LabelComparator mComparator = new LabelComparator();

        @Override
        public int compare(WidgetListRowEntry widgetListRowEntry, WidgetListRowEntry widgetListRowEntry2) {
            return this.mComparator.compare(widgetListRowEntry.pkgItem.title.toString(), widgetListRowEntry2.pkgItem.title.toString());
        }
    }
}
