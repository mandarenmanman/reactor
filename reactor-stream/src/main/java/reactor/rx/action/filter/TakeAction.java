/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package reactor.rx.action.filter;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import reactor.rx.action.Action;

/**
 * @author Stephane Maldini
 * @since 2.0
 */
public class TakeAction<T> extends Action<T, T> {

	private final long limit;
	private long counted = 0l;

	private volatile     long                               requested = 0L;
	private static final AtomicLongFieldUpdater<TakeAction> REQUESTED =
			AtomicLongFieldUpdater.newUpdater(TakeAction.class, "requested");

	public TakeAction(long limit) {
		this.limit = limit;
	}

	@Override
	public void requestMore(long n) {
		if (n >= limit) {
			super.requestMore(limit);
		}
		else {
			long r, toAdd;
			do {
				r = requested;
				if (r >= limit) {
					return;
				}
				toAdd = r + n;
			}
			while (!REQUESTED.compareAndSet(this, r, toAdd));

			if (toAdd > limit) {
				super.requestMore(limit - r);
			}
			else {
				super.requestMore(n);
			}
		}
	}

	@Override
	protected void doNext(T ev) {
		broadcastNext(ev);

		if (++counted >= limit) {
			cancel();
			broadcastComplete();
		}
	}

	@Override
	public String toString() {
		return super.toString() + "{" +
				"take=" + limit +
				", counted=" + counted +
				'}';
	}
}
