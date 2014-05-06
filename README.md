Android-GenericFilter
=====================

Use generics to filter any Filterable.

Example Usage
-------------
```java
public class ObjectAdapter extends BaseAdapter implements Filterable {
    private List<Object> mData;
    private List<Object> mFilteredData;
    private ObjectFilter mFilter;

    @Override
    public int getCount() {
        return mFilteredData.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public ObjectFilter getFilter() {
        if (mFilter == null) {
            mFilter = new ObjectFilter();
        }
        return mFilter;
    }

    public class ObjectFilter extends GenericFilter<Object> {
        @Override
        protected FilterResults performFiltering(Object constraint) {
            List<Object> resultCollection = new ArrayList<>();
            for (Object o : mData) {
                if (o.equals(constraint)) {
                    resultCollection.add(o);
                }
            }
            FilterResults results = new FilterResults();
            results.count = resultCollection.size();
            results.values = resultCollection;
            return results;
        }

        @Override
        protected void publishResults(Object constraint, FilterResults results) {
            mFilteredData = (List<Object>) results.values;
        }
    }
}
```

Obtaining
---------
Add the the following to your gradle.build file.
```gradle
dependencies {
    compile 'net.niiranen:Android-GenericFilter:1.0.+'
}
```

License
-------

    Copyright 2014 Mattias Niiranen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
