/*
 * Copyright (C) 2014 Mattias Niiranen
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

package net.niiranen.genericfilter.sampleapplication;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import net.niiranen.genericfilter.GenericFilter;

class EnumFilterAdapter extends BaseAdapter implements Filterable {
    public enum NumberFilter {
        All,
        Negative,
        Positive,
        Even,
        Odd,
    }

    private ListFilter         mFilter;
    private ArrayList<Integer> mData;
    private ArrayList<Integer> mFilteredData;
    private Context            mContext;

    public EnumFilterAdapter(Context context, ArrayList<Integer> data) {
        mContext = context;
        mData = data;
        mFilteredData = mData;
    }

    @Override
    public int getCount() {
        return mFilteredData != null ? mFilteredData.size() : 0;
    }

    @Override
    public Integer getItem(int position) {
        return mFilteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) view.findViewById(R.id.text);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        holder.text.setText(getItem(position).toString());
        return view;
    }

    private static class ViewHolder {
        TextView text;
    }

    @Override
    public ListFilter getFilter() {
        if (mFilter == null) {
            mFilter = new ListFilter();
        }
        return mFilter;
    }

    class ListFilter extends GenericFilter<NumberFilter> {
        @Override
        protected FilterResults performFiltering(NumberFilter constraint) {
            FilterResults results = new FilterResults();
            if (mData.isEmpty()) {
                results.count = 0;
                return results;
            }
            if (constraint == NumberFilter.All) {
                results.count = mData.size();
                results.values = mData;
                return results;
            }
            ArrayList<Integer> filtered = new ArrayList<>();
            for (Integer i : mData) {
                switch (constraint) {
                    case Negative:
                        if (i < 0) {
                            filtered.add(i);
                        }
                        break;
                    case Positive:
                        if (i > 0) {
                            filtered.add(i);
                        }
                        break;
                    case Odd:
                        if ((i & 1) != 0) {
                            filtered.add(i);
                        }
                        break;
                    case Even:
                        if ((i & 1) == 0) {
                            filtered.add(i);
                        }
                        break;
                }
            }
            results.count = filtered.size();
            results.values = filtered;
            return results;
        }

        @Override
        protected void publishResults(NumberFilter constraint, FilterResults results) {
            if (results.values != null) {
                mFilteredData = (ArrayList<Integer>) results.values;
            } else {
                mFilteredData = mData;
            }
            notifyDataSetChanged();
        }
    }
}