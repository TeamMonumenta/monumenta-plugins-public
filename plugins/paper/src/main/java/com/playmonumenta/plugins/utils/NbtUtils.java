package com.playmonumenta.plugins.utils;

import de.tr7zw.nbtapi.NBTCompound;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.Nullable;

public class NbtUtils {
	/*
	 * Iterator for nested NBTCompounds
	 * pathIterables is a list of key iterables, where
	 * index 0 is a sorted list of keys that may or may not be in compound,
	 * and each further index is nested one level beyond that.
	 * Keys not in an iterable are ignored.
	 * If an iterable is null, all entries at that level will be iterated over as-is.
	 */
	public class NestedCompoundIterator implements Iterator<NBTCompound> {
		private boolean mStarted = false;
		private NBTCompound mHere;
		// mIt is only null if uninitialized or mHere is the only node
		@Nullable private Iterator<String> mIt = null;
		private List<Iterable<String>> mSubPathIterables;

		// Used for remove()
		private boolean mLastRemoved = false;
		private String mLastLocalKey = "";
		@Nullable private NestedCompoundIterator mLastLocalIt = null;

		// Remember the next node once found
		private boolean mSearchedForNext = false;
		private boolean mHasNext = false;
		private String mNextLocalKey = "";
		@Nullable private NestedCompoundIterator mNextLocalIt = null;

		NestedCompoundIterator(NBTCompound compound, List<Iterable<String>> pathIterables) {
			mHere = compound;
			if (pathIterables.isEmpty()) {
				mSubPathIterables = new ArrayList();
				return;
			}
			mSubPathIterables = pathIterables.subList(1, pathIterables.size());
			mIt = pathIterables.get(0).iterator();
			if (mIt == null) {
				mIt = new ArrayList(mHere.getKeys()).iterator();
			}
		}

		@Override
		public boolean hasNext() {
			if (mIt == null) {
				// No paths provided; can only return mHere
				return !mStarted;
			}
			if (!mSearchedForNext) {
				findNext();
			}
			return mHasNext;
		}

		@Override
		public NBTCompound next() {
			mStarted = true;
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			if (mIt == null) {
				return mHere;
			}
			mLastRemoved = false;
			mLastLocalKey = mNextLocalKey;
			mLastLocalIt = mNextLocalIt;
			NBTCompound result = mNextLocalIt.next();
			mSearchedForNext = false;
			return result;
		}

		// Returns the path of the last next() call, ie ["tag", "plain", "display"]
		public List<String> lastPath() {
			if (!mStarted) {
				throw new IllegalStateException("Iterator.next() has not been called yet.");
			}
			if (mIt == null) {
				return new ArrayList<>();
			}
			List<String> path = mLastLocalIt.lastPath();
			path.add(0, mLastLocalKey);
			return path;
		}

		// NOTE: Does not remove anything if provided an empty pathIterables list (mIt == null)
		@Override
		public void remove() {
			if (!mStarted) {
				throw new IllegalStateException("Iterator.next() has not been called yet.");
			}
			if (mLastRemoved) {
				throw new IllegalStateException("Iterator.next() has not been called since the last element was removed.");
			}
			mLastRemoved = true;
			if (mIt == null) {
				return;
			}
			mLastLocalIt.remove();
			if (mSubPathIterables.isEmpty()) {
				mHere.removeKey(mLastLocalKey);
				return;
			}
			if (mLastLocalIt.mHere.getKeys().isEmpty()) {
				mHere.removeKey(mLastLocalKey);
			}
		}

		// Find the next entry without updating the last entry
		private void findNext() {
			if (mIt == null) {
				return;
			}
			if (mSearchedForNext) {
				return;
			}
			if (mNextLocalIt != null) {
				if (mNextLocalIt.hasNext()) {
					mSearchedForNext = true;
					mHasNext = true;
					return;
				}
			}
			while (mIt.hasNext()) {
				mNextLocalKey = mIt.next();
				if (mHere.hasKey(mNextLocalKey)) {
					NBTCompound child = mHere.addCompound(mNextLocalKey);
					mNextLocalIt = new NestedCompoundIterator(child, mSubPathIterables);
					if (mNextLocalIt.hasNext()) {
						mSearchedForNext = true;
						mHasNext = true;
					}
				}
			}
			mSearchedForNext = true;
			mHasNext = false;
		}
	}

	public static NBTCompound getNestedCompound(NBTCompound compound, List<String> path) {
		if (path.isEmpty()) {
			return compound;
		}
		NBTCompound child = compound.addCompound(path.get(0));
		return getNestedCompound(child, path.subList(1, path.size()));
	}

	public static void deleteNestedCompound(NBTCompound compound, List<String> path) {
		if (path.isEmpty()) {
			return;
		}
		String key = path.get(0);
		if (compound.hasKey(key)) {
			if (path.size() == 1) {
				compound.removeKey(key);
				return;
			}
			NBTCompound child = compound.addCompound(path.get(0));
			deleteNestedCompound(child, path.subList(1, path.size()));
			if (child.getKeys().isEmpty()) {
				compound.removeKey(key);
			}
		}
	}
}
