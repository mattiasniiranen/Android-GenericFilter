/*
 * Copyright (C) 2014 Mattias Niiranen
 * Copyright (C) 2007 The Android Open Source Project
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

package net.niiranen.genericfilter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Filter;

public abstract class GenericFilter<E> extends Filter {
    private static final String LOG_TAG = "GenericFilter";

    private static final String THREAD_NAME  = "GenericFilter";
    private static final int    FILTER_TOKEN = 0xBADF00D5;
    private static final int    FINISH_TOKEN = 0x600DBEEF;

    private Handler mThreadHandler;
    private Handler mResultHandler;

    private final Object mLock = new Object();

    public GenericFilter() {
        mResultHandler = new GenericResultsHandler();
    }

    /**
     * <p>Starts an asynchronous filtering operation. Calling this method cancels all previous
     * non-executed filtering requests and posts a new filtering request that will be executed
     * later.</p>
     *
     * @param constraint
     *     the constraint used to filter the data
     *
     * @see #filter(E, android.widget.Filter.FilterListener)
     */
    public final void filter(E constraint) {
        filter(constraint, null);
    }

    /**
     * <p>Starts an asynchronous filtering operation. Calling this method cancels all previous
     * non-executed filtering requests and posts a new filtering request that will be executed
     * later.</p> <p/> <p>Upon completion, the listener is notified.</p>
     *
     * @param constraint
     *     the constraint used to filter the data
     * @param listener
     *     a listener notified upon completion of the operation
     *
     * @see #filter(E)
     * @see #performFiltering(E)
     * @see #publishResults(E, android.widget.Filter.FilterResults)
     */
    public final void filter(E constraint, FilterListener listener) {

        synchronized (mLock) {
            if (mThreadHandler == null) {
                HandlerThread thread = new HandlerThread(
                    THREAD_NAME, android.os.Process.THREAD_PRIORITY_BACKGROUND);
                thread.start();
                mThreadHandler = new GenericRequestHandler(thread.getLooper());
            }

            final long delay = 0;

            Message message = mThreadHandler.obtainMessage(FILTER_TOKEN);

            GenericRequestArguments<E> args = new GenericRequestArguments<E>();
            args.constraint = constraint;
            args.listener = listener;
            message.obj = args;

            mThreadHandler.removeMessages(FILTER_TOKEN);
            mThreadHandler.removeMessages(FINISH_TOKEN);
            mThreadHandler.sendMessageDelayed(message, delay);
        }
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        return null;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
    }

    /**
     * <p>Invoked in a worker thread to filter the data according to the constraint. Subclasses must
     * implement this method to perform the filtering operation. Results computed by the filtering
     * operation must be returned as a {@link android.widget.Filter.FilterResults} that will then be
     * published in the UI thread through {@link #publishResults(E, android.widget.Filter.FilterResults)}.</p>
     * <p/> <p><strong>Contract:</strong> When the constraint is null, the original data must be
     * restored.</p>
     *
     * @param constraint
     *     the constraint used to filter the data
     *
     * @return the results of the filtering operation
     *
     * @see #filter(E, android.widget.Filter.FilterListener)
     * @see #publishResults(E, android.widget.Filter.FilterResults)
     * @see android.widget.Filter.FilterResults
     */
    protected abstract FilterResults performFiltering(E constraint);

    /**
     * <p>Invoked in the UI thread to publish the filtering results in the user interface.
     * Subclasses must implement this method to display the results computed in {@link
     * #performFiltering}.</p>
     *
     * @param constraint
     *     the constraint used to filter the data
     * @param results
     *     the results of the filtering operation
     *
     * @see #filter(E, android.widget.Filter.FilterListener)
     * @see #performFiltering(E)
     * @see android.widget.Filter.FilterResults
     */
    protected abstract void publishResults(E constraint, FilterResults results);

    /**
     * <p>Worker thread handler. When a new filtering request is posted from {@link
     * net.niiranen.genericfilter.GenericFilter#filter(E, android.widget.Filter.FilterListener)}, it
     * is sent to this handler.</p>
     */
    private class GenericRequestHandler extends Handler {
        public GenericRequestHandler(Looper looper) {
            super(looper);
        }

        /**
         * <p>Handles filtering requests by calling {@link net.niiranen.genericfilter.GenericFilter#performFiltering}
         * and then sending a message with the results to the results handler.</p>
         *
         * @param msg
         *     the filtering request
         */
        public void handleMessage(Message msg) {
            int what = msg.what;
            Message message;
            switch (what) {
                case FILTER_TOKEN:
                    GenericRequestArguments<E> args = (GenericRequestArguments<E>) msg.obj;
                    try {
                        args.results = performFiltering(args.constraint);
                    } catch (Exception e) {
                        args.results = new FilterResults();
                        Log.w(LOG_TAG, "An exception occurred during performFiltering()!", e);
                    } finally {
                        message = mResultHandler.obtainMessage(what);
                        message.obj = args;
                        message.sendToTarget();
                    }

                    synchronized (mLock) {
                        if (mThreadHandler != null) {
                            Message finishMessage = mThreadHandler.obtainMessage(FINISH_TOKEN);
                            mThreadHandler.sendMessageDelayed(finishMessage, 3000);
                        }
                    }
                    break;
                case FINISH_TOKEN:
                    synchronized (mLock) {
                        if (mThreadHandler != null) {
                            mThreadHandler.getLooper().quit();
                            mThreadHandler = null;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * <p>Handles the results of a filtering operation. The results are handled in the UI
     * thread.</p>
     */
    private class GenericResultsHandler extends Handler {
        /**
         * <p>Messages received from the request handler are processed in the UI thread. The
         * processing involves calling {@link net.niiranen.genericfilter.GenericFilter#publishResults(E,
         * android.widget.Filter.FilterResults)} to post the results back in the UI and then
         * notifying the listener, if any.</p>
         *
         * @param msg
         *     the filtering results
         */
        @Override
        public void handleMessage(Message msg) {
            GenericRequestArguments<E> args = (GenericRequestArguments<E>) msg.obj;

            publishResults(args.constraint, args.results);
            if (args.listener != null) {
                int count = args.results != null ? args.results.count : -1;
                args.listener.onFilterComplete(count);
            }
        }
    }

    /**
     * <p>Holds the arguments of a filtering request as well as the results of the request.</p>
     */
    private static class GenericRequestArguments<E> {
        /**
         * <p>The constraint used to filter the data.</p>
         */
        E constraint;

        /**
         * <p>The listener to notify upon completion. Can be null.</p>
         */
        FilterListener listener;

        /**
         * <p>The results of the filtering operation.</p>
         */
        FilterResults results;
    }
}
