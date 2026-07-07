/*
 * Debounced Telegram auto-reply controller for AyuGram Android.
 */

package com.radolyn.ayugram.ai;

import android.text.TextUtils;

import com.radolyn.ayugram.AyuConfig;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class AyuAiReplyController {
    private static volatile AyuAiReplyController[] instances = new AyuAiReplyController[UserConfig.MAX_ACCOUNT_COUNT];

    private final int account;
    private final HashMap<Long, DialogState> states = new HashMap<>();

    private AyuAiReplyController(int account) {
        this.account = account;
    }

    public static AyuAiReplyController getInstance(int account) {
        AyuAiReplyController local = instances[account];
        if (local == null) {
            synchronized (AyuAiReplyController.class) {
                local = instances[account];
                if (local == null) {
                    local = instances[account] = new AyuAiReplyController(account);
                }
            }
        }
        return local;
    }

    public void onNewMessage(TLRPC.Message message) {
        if (message == null || message instanceof TLRPC.TL_messageEmpty) {
            return;
        }

        long dialogId = MessageObject.getDialogId(message);
        if (dialogId == 0) {
            return;
        }

        DialogState state;
        synchronized (this) {
            state = states.get(dialogId);
            if (state == null) {
                state = new DialogState();
                states.put(dialogId, state);
            }
            state.append(new HistoryEntry(message.out, describeMessage(message)));
        }

        if (!shouldReply(message, dialogId)) {
            return;
        }

        synchronized (this) {
            state.version++;
            if (state.pendingRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(state.pendingRunnable);
            }

            int version = state.version;
            Runnable runnable = () -> requestReply(dialogId, version);
            state.pendingRunnable = runnable;
            AndroidUtilities.runOnUIThread(runnable, Math.max(1, AyuConfig.aiDebounceSeconds) * 1000L);
        }
    }

    private boolean shouldReply(TLRPC.Message message, long dialogId) {
        if (!AyuConfig.aiReplyEnabled || message.out || message.from_scheduled) {
            return false;
        }
        if (message.action != null && !(message.action instanceof TLRPC.TL_messageActionEmpty)) {
            return false;
        }
        if (dialogId == UserConfig.getInstance(account).getClientUserId()) {
            return false;
        }
        if (AyuConfig.aiPrivateOnly && dialogId <= 0) {
            return false;
        }
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
            if (user != null && user.bot) {
                return false;
            }
        }
        return !TextUtils.isEmpty(describeMessage(message));
    }

    private void requestReply(long dialogId, int version) {
        String prompt;
        synchronized (this) {
            DialogState state = states.get(dialogId);
            if (state == null || state.version != version) {
                return;
            }
            state.pendingRunnable = null;
            prompt = buildPrompt(dialogId, state);
        }

        AyuAiBridgeClient.requestReply(prompt, new AyuAiBridgeClient.ResultCallback() {
            @Override
            public void onSuccess(String text) {
                AndroidUtilities.runOnUIThread(() -> deliverReply(dialogId, version, text));
            }

            @Override
            public void onError(Exception error) {
                FileLog.e("AI bridge request failed", error);
            }
        });
    }

    private void deliverReply(long dialogId, int version, String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        synchronized (this) {
            DialogState state = states.get(dialogId);
            if (state == null || state.version != version) {
                return;
            }
        }

        ArrayList<String> parts = splitReply(text);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (TextUtils.isEmpty(part)) {
                continue;
            }
            AndroidUtilities.runOnUIThread(() -> {
                SendMessagesHelper.getInstance(account).sendMessage(
                        part,
                        dialogId,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null,
                        null,
                        true,
                        0,
                        null,
                        false
                );
                synchronized (AyuAiReplyController.this) {
                    DialogState state = states.get(dialogId);
                    if (state != null) {
                        state.append(new HistoryEntry(true, part));
                    }
                }
            }, i * 1300L);
        }
    }

    private String buildPrompt(long dialogId, DialogState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are replying from Telegram account ").append(UserConfig.getInstance(account).getClientUserId()).append(".\n");
        builder.append("Dialog id: ").append(dialogId).append(".\n");
        builder.append("Recent dialog messages, oldest first:\n");

        int start = Math.max(0, state.history.size() - Math.max(1, AyuConfig.aiHistoryLimit));
        for (int i = start; i < state.history.size(); i++) {
            HistoryEntry entry = state.history.get(i);
            builder.append(entry.outgoing ? "me: " : "them: ")
                    .append(entry.text)
                    .append('\n');
        }

        builder.append("\nReturn only the Telegram reply text. ");
        builder.append("If no answer is needed, return an empty message. ");
        builder.append("Do not answer simple acknowledgements or conversation endings. ");
        builder.append("If several short messages are more natural, put each message on a separate line.");
        return builder.toString();
    }

    private ArrayList<String> splitReply(String text) {
        ArrayList<String> result = new ArrayList<>();
        String trimmed = text.trim();
        if (TextUtils.isEmpty(trimmed)) {
            return result;
        }

        if (!AyuConfig.aiSplitReplies) {
            result.add(trimmed);
            return result;
        }

        String[] lines = trimmed.split("\\r?\\n");
        for (String line : lines) {
            String part = line.trim();
            if (!TextUtils.isEmpty(part)) {
                result.add(part);
            }
        }
        if (result.isEmpty()) {
            result.add(trimmed);
        }
        return result;
    }

    private String describeMessage(TLRPC.Message message) {
        if (!TextUtils.isEmpty(message.message)) {
            return message.message;
        }
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            return "[photo]";
        }
        if (message.media instanceof TLRPC.TL_messageMediaDocument && message.media.document != null) {
            if (MessageObject.isStickerDocument(message.media.document)) {
                return "[sticker]";
            }
            if (MessageObject.isVoiceDocument(message.media.document)) {
                return "[voice]";
            }
            if (MessageObject.isRoundVideoDocument(message.media.document)) {
                return "[round video]";
            }
            if (MessageObject.isVideoDocument(message.media.document)) {
                return "[video]";
            }
            return String.format(Locale.US, "[file: %s]", TextUtils.isEmpty(message.media.document.mime_type) ? "unknown" : message.media.document.mime_type);
        }
        if (message.media != null && !(message.media instanceof TLRPC.TL_messageMediaEmpty)) {
            return "[media]";
        }
        return "";
    }

    private static class DialogState {
        final ArrayList<HistoryEntry> history = new ArrayList<>();
        int version;
        Runnable pendingRunnable;

        void append(HistoryEntry entry) {
            history.add(entry);
            while (history.size() > 100) {
                history.remove(0);
            }
        }
    }

    private static class HistoryEntry {
        final boolean outgoing;
        final String text;

        HistoryEntry(boolean outgoing, String text) {
            this.outgoing = outgoing;
            this.text = text;
        }
    }
}
