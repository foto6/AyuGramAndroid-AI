/*
 * Local replacement for the unavailable AyuGram4A_proprietary submodule.
 */

package com.radolyn.ayugram.proprietary;

import android.text.TextUtils;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.entities.AyuMessageBase;
import com.radolyn.ayugram.messages.AyuSavePreferences;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;

public class AyuMessageUtils {
    private static final long CHANNEL_DIALOG_OFFSET = -1000000000000L;

    public static void map(AyuSavePreferences prefs, AyuMessageBase target) {
        TLRPC.Message message = prefs.getMessage();
        target.userId = prefs.getUserId();
        target.dialogId = prefs.getDialogId();
        target.topicId = prefs.getTopicId();
        target.messageId = prefs.getMessageId();
        target.entityCreateDate = prefs.getRequestCatchTime();

        if (message == null) {
            return;
        }

        target.groupedId = message.grouped_id;
        target.peerId = peerToDialogId(message.peer_id);
        target.fromId = peerToDialogId(message.from_id);
        target.date = message.date;
        target.flags = message.flags;
        target.editDate = message.edit_date;
        target.views = message.views;
        target.text = message.message;

        if (message.fwd_from != null) {
            target.fwdFlags = message.fwd_from.flags;
            target.fwdFromId = peerToDialogId(message.fwd_from.from_id);
            target.fwdName = message.fwd_from.from_name;
            target.fwdDate = message.fwd_from.date;
            target.fwdPostAuthor = message.fwd_from.post_author;
        }

        if (message.reply_to != null) {
            target.replyFlags = message.reply_to.flags;
            target.replyMessageId = message.reply_to.reply_to_msg_id;
            target.replyPeerId = peerToDialogId(message.reply_to.reply_to_peer_id);
            target.replyTopId = message.reply_to.reply_to_top_id;
            target.replyForumTopic = message.reply_to.forum_topic;
        }
    }

    public static void mapMedia(AyuSavePreferences prefs, AyuMessageBase target, boolean copyMedia) {
        TLRPC.Message message = prefs.getMessage();
        if (message == null || message.media == null) {
            target.documentType = AyuConstants.DOCUMENT_TYPE_NONE;
            return;
        }

        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            target.documentType = AyuConstants.DOCUMENT_TYPE_PHOTO;
        } else if (message.media instanceof TLRPC.TL_messageMediaDocument && message.media.document != null) {
            target.documentType = MessageObject.isStickerDocument(message.media.document)
                    ? AyuConstants.DOCUMENT_TYPE_STICKER
                    : AyuConstants.DOCUMENT_TYPE_FILE;
            target.mimeType = message.media.document.mime_type;
        } else {
            target.documentType = AyuConstants.DOCUMENT_TYPE_NONE;
        }

        File path = FileLoader.getInstance(prefs.getAccountId()).getPathToMessage(message);
        if (path != null) {
            target.mediaPath = path.getAbsolutePath();
        }
    }

    public static void map(AyuMessageBase source, TLRPC.TL_message target, int account) {
        target.id = source.messageId;
        target.dialog_id = source.dialogId;
        target.grouped_id = source.groupedId;
        target.peer_id = dialogIdToPeer(source.dialogId);
        target.from_id = dialogIdToPeer(source.fromId);
        target.out = source.fromId == UserConfig.getInstance(account).getClientUserId();
        target.date = source.date;
        target.flags = source.flags;
        target.edit_date = source.editDate;
        target.views = source.views;
        target.message = TextUtils.isEmpty(source.text) ? "" : source.text;

        if (source.replyMessageId != 0) {
            target.reply_to = new TLRPC.TL_messageReplyHeader();
            target.reply_to.flags = source.replyFlags;
            target.reply_to.reply_to_msg_id = source.replyMessageId;
            target.reply_to.reply_to_peer_id = dialogIdToPeer(source.replyPeerId);
            target.reply_to.reply_to_top_id = source.replyTopId;
            target.reply_to.forum_topic = source.replyForumTopic;
        }

        if (source.fwdDate != 0 || source.fwdFromId != 0 || !TextUtils.isEmpty(source.fwdName)) {
            target.fwd_from = new TLRPC.TL_messageFwdHeader();
            target.fwd_from.flags = source.fwdFlags;
            target.fwd_from.from_id = dialogIdToPeer(source.fwdFromId);
            target.fwd_from.from_name = source.fwdName;
            target.fwd_from.date = source.fwdDate;
            target.fwd_from.post_author = source.fwdPostAuthor;
        }
    }

    public static void mapMedia(AyuMessageBase source, TLRPC.TL_message target) {
        if (source.documentType == AyuConstants.DOCUMENT_TYPE_PHOTO) {
            TLRPC.TL_messageMediaPhoto media = new TLRPC.TL_messageMediaPhoto();
            media.photo = new TLRPC.TL_photoEmpty();
            media.photo.id = 0;
            target.media = media;
        } else if (source.documentType == AyuConstants.DOCUMENT_TYPE_FILE || source.documentType == AyuConstants.DOCUMENT_TYPE_STICKER) {
            TLRPC.TL_messageMediaDocument media = new TLRPC.TL_messageMediaDocument();
            TLRPC.TL_document document = new TLRPC.TL_document();
            document.id = 0;
            document.mime_type = TextUtils.isEmpty(source.mimeType) ? "application/octet-stream" : source.mimeType;
            media.document = document;
            target.media = media;
        } else {
            target.media = new TLRPC.TL_messageMediaEmpty();
        }
    }

    private static long peerToDialogId(TLRPC.Peer peer) {
        if (peer == null) {
            return 0;
        }
        if (peer.user_id != 0) {
            return peer.user_id;
        }
        if (peer.chat_id != 0) {
            return -peer.chat_id;
        }
        if (peer.channel_id != 0) {
            return CHANNEL_DIALOG_OFFSET - peer.channel_id;
        }
        return 0;
    }

    private static TLRPC.Peer dialogIdToPeer(long dialogId) {
        if (dialogId == 0) {
            return null;
        }
        if (dialogId < CHANNEL_DIALOG_OFFSET) {
            TLRPC.TL_peerChannel peer = new TLRPC.TL_peerChannel();
            peer.channel_id = (int) (-(dialogId - CHANNEL_DIALOG_OFFSET));
            return peer;
        }
        if (dialogId < 0) {
            TLRPC.TL_peerChat peer = new TLRPC.TL_peerChat();
            peer.chat_id = (int) -dialogId;
            return peer;
        }
        TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser();
        peer.user_id = dialogId;
        return peer;
    }
}
