/*
 * Local replacement for the unavailable AyuGram4A_proprietary submodule.
 */

package com.radolyn.ayugram.proprietary;

import android.util.Pair;
import android.util.SparseArray;

import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public class AyuHistoryHook {
    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messages) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        if (messages != null) {
            for (MessageObject object : messages) {
                if (object == null) {
                    continue;
                }
                int id = object.getId();
                min = Math.min(min, id);
                max = Math.max(max, id);
            }
        }

        if (min == Integer.MAX_VALUE || max == Integer.MIN_VALUE) {
            return new Pair<>(0, 0);
        }

        return new Pair<>(min, max);
    }

    public static void doHook(
            int currentAccount,
            ArrayList<MessageObject> messages,
            SparseArray<MessageObject>[] messagesDict,
            int startId,
            int endId,
            long dialogId,
            int limit,
            long topicId,
            boolean secretChat
    ) {
        // The original implementation backfills locally saved deleted messages.
        // Keep this no-op so public builds remain functional without the private submodule.
    }
}
