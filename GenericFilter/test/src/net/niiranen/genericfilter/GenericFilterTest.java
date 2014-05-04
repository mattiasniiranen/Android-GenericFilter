/*
* Copyright (C) 2014 Mattias Niiranen
* Copyright (C) 2009 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package net.niiranen.genericfilter;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Filter;

public class GenericFilterTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private static final long    TIME_OUT        = 10000;
    private static final Integer TEST_CONSTRAINT = 0x600D7E57;
    private MockFilter mMockFilter;

    public GenericFilterTest() {
        super(StubActivity.class);
    }

    public void testConstructor() {
        new MockFilter();
    }

    public void testConvertResultToString() {
        final MockFilter filter = new MockFilter();
        assertEquals("", filter.convertResultToString(null));
        final String testStr = "Test";
        assertEquals(testStr, filter.convertResultToString(testStr));
        final int testInt = 42;
        assertEquals(Integer.toString(testInt), filter.convertResultToString(testInt));
        class TestObject {
            String value = "value";

            @Override
            public String toString() {
                return value;
            }
        }
        TestObject testObject = new TestObject();
        assertEquals(testObject.toString(), filter.convertResultToString(testObject));
    }

    public void testFilter1() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mMockFilter = new MockFilter();
                mMockFilter.filter(TEST_CONSTRAINT);
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mMockFilter.hadPerformedFiltering();
            }
        }.run();
        assertEquals(TEST_CONSTRAINT, mMockFilter.getPerformFilteringConstraint());
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mMockFilter.hadPublishedResults();
            }
        }.run();
        assertEquals(TEST_CONSTRAINT, mMockFilter.getPublishResultsConstraint());
        assertSame(mMockFilter.getExpectResults(), mMockFilter.getResults());
    }

    public void testFilter2() {
        final MockFilterListener mockFilterListener = new MockFilterListener();
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mMockFilter = new MockFilter();
                mMockFilter.filter(TEST_CONSTRAINT, mockFilterListener);
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mMockFilter.hadPerformedFiltering();
            }
        }.run();
        assertEquals(TEST_CONSTRAINT, mMockFilter.getPerformFilteringConstraint());
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mMockFilter.hadPublishedResults();
            }
        }.run();
        assertEquals(TEST_CONSTRAINT, mMockFilter.getPublishResultsConstraint());
        assertSame(mMockFilter.getExpectResults(), mMockFilter.getResults());
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mockFilterListener.hasCalledOnFilterComplete();
            }
        }.run();
    }

    private static class MockFilter extends GenericFilter<Integer> {
        private boolean mHadPublishedResults   = false;
        private boolean mHadPerformedFiltering = false;
        private Integer       mPerformFilteringConstraint;
        private Integer       mPublishResultsConstraint;
        private FilterResults mResults;
        private FilterResults mExpectResults = new FilterResults();

        public MockFilter() {
            super();
        }

        public boolean hadPublishedResults() {
            synchronized (this) {
                return mHadPublishedResults;
            }
        }

        public boolean hadPerformedFiltering() {
            synchronized (this) {
                return mHadPerformedFiltering;
            }
        }

        public Integer getPerformFilteringConstraint() {
            synchronized (this) {
                return mPerformFilteringConstraint;
            }
        }

        public Integer getPublishResultsConstraint() {
            synchronized (this) {
                return mPublishResultsConstraint;
            }
        }

        public FilterResults getResults() {
            synchronized (this) {
                return mResults;
            }
        }

        public FilterResults getExpectResults() {
            synchronized (this) {
                return mExpectResults;
            }
        }

        @Override
        protected FilterResults performFiltering(Integer constraint) {
            synchronized (this) {
                mHadPerformedFiltering = true;
                mPerformFilteringConstraint = constraint;
                return mExpectResults;
            }
        }

        @Override
        protected void publishResults(Integer constraint, FilterResults results) {
            synchronized (this) {
                mPublishResultsConstraint = constraint;
                mResults = results;
                mHadPublishedResults = true;
            }
        }
    }

    private static class MockFilterListener implements Filter.FilterListener {
        private boolean mCalledOnFilterComplete = false;

        public void onFilterComplete(int count) {
            mCalledOnFilterComplete = true;
        }

        public boolean hasCalledOnFilterComplete() {
            return mCalledOnFilterComplete;
        }
    }
}