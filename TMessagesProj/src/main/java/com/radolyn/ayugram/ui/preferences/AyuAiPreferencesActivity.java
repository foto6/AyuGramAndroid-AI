/*
 * AyuGram Android AI Bridge settings.
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.ai.AyuAiBridgeClient;
import com.radolyn.ayugram.ui.preferences.utils.AyuUi;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;

public class AyuAiPreferencesActivity extends BasePreferencesActivity {
    private int bridgeHeaderRow;
    private int enabledRow;
    private int privateOnlyRow;
    private int splitRepliesRow;
    private int bridgeUrlRow;
    private int bridgeApiKeyRow;
    private int bridgeModelRow;
    private int debounceRow;
    private int historyLimitRow;
    private int systemPromptRow;
    private int testBridgeRow;
    private int bridgeDividerRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        bridgeHeaderRow = newRow();
        enabledRow = newRow();
        privateOnlyRow = newRow();
        splitRepliesRow = newRow();
        bridgeUrlRow = newRow();
        bridgeApiKeyRow = newRow();
        bridgeModelRow = newRow();
        debounceRow = newRow();
        historyLimitRow = newRow();
        systemPromptRow = newRow();
        testBridgeRow = newRow();
        bridgeDividerRow = newRow();
    }

    @Override
    protected String getTitle() {
        return "AI Bridge";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enabledRow) {
            AyuConfig.editor.putBoolean("aiReplyEnabled", AyuConfig.aiReplyEnabled ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.aiReplyEnabled);
        } else if (position == privateOnlyRow) {
            AyuConfig.editor.putBoolean("aiPrivateOnly", AyuConfig.aiPrivateOnly ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.aiPrivateOnly);
        } else if (position == splitRepliesRow) {
            AyuConfig.editor.putBoolean("aiSplitReplies", AyuConfig.aiSplitReplies ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.aiSplitReplies);
        } else if (position == bridgeUrlRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    (TextCell) view,
                    "Bridge URL",
                    AyuConfig::getAiBridgeUrl,
                    "aiBridgeUrl",
                    "http://192.168.137.1:17448/v1"
            );
        } else if (position == bridgeApiKeyRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    (TextCell) view,
                    "API key",
                    AyuConfig::getAiBridgeApiKey,
                    "aiBridgeApiKey",
                    ""
            );
        } else if (position == bridgeModelRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    (TextCell) view,
                    "Model",
                    AyuConfig::getAiBridgeModel,
                    "aiBridgeModel",
                    "chatgpt-web"
            );
        } else if (position == debounceRow) {
            AyuUi.spawnIntBox(
                    getParentActivity(),
                    (TextCell) view,
                    "Debounce seconds",
                    () -> AyuConfig.aiDebounceSeconds,
                    "aiDebounceSeconds",
                    10,
                    1,
                    120,
                    value -> AyuConfig.aiDebounceSeconds = value
            );
        } else if (position == historyLimitRow) {
            AyuUi.spawnIntBox(
                    getParentActivity(),
                    (TextCell) view,
                    "History messages",
                    () -> AyuConfig.aiHistoryLimit,
                    "aiHistoryLimit",
                    20,
                    1,
                    100,
                    value -> AyuConfig.aiHistoryLimit = value
            );
        } else if (position == systemPromptRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    (TextCell) view,
                    "System prompt",
                    AyuConfig::getAiSystemPrompt,
                    "aiSystemPrompt",
                    AyuConstants.DEFAULT_AI_SYSTEM_PROMPT
            );
        } else if (position == testBridgeRow) {
            AyuAiBridgeClient.requestReply("Reply with exactly one word: pong", new AyuAiBridgeClient.ResultCallback() {
                @Override
                public void onSuccess(String text) {
                    AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(AyuAiPreferencesActivity.this)
                            .createSimpleBulletin(R.raw.info, "AI Bridge: " + shortValue(text))
                            .show());
                }

                @Override
                public void onError(Exception error) {
                    AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(AyuAiPreferencesActivity.this)
                            .createSimpleBulletin(R.raw.error, "AI Bridge error: " + shortValue(error.getMessage()))
                            .show());
                }
            });
        }
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, org.telegram.messenger.R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == bridgeUrlRow) {
                        textCell.setTextAndValue("Bridge URL", AyuConfig.getAiBridgeUrl(), true);
                    } else if (position == bridgeApiKeyRow) {
                        String key = AyuConfig.getAiBridgeApiKey();
                        textCell.setTextAndValue("API key", key.isEmpty() ? "empty" : "set", true);
                    } else if (position == bridgeModelRow) {
                        textCell.setTextAndValue("Model", AyuConfig.getAiBridgeModel(), true);
                    } else if (position == debounceRow) {
                        textCell.setTextAndValue("Debounce seconds", String.valueOf(AyuConfig.aiDebounceSeconds), true);
                    } else if (position == historyLimitRow) {
                        textCell.setTextAndValue("History messages", String.valueOf(AyuConfig.aiHistoryLimit), true);
                    } else if (position == systemPromptRow) {
                        textCell.setTextAndValue("System prompt", shortValue(AyuConfig.getAiSystemPrompt()), false);
                    } else if (position == testBridgeRow) {
                        textCell.setTextAndValue("Test bridge", "send ping", false);
                    }
                    break;
                case 3:
                    ((HeaderCell) holder.itemView).setText("AI auto replies");
                    break;
                case 5:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == enabledRow) {
                        checkCell.setTextAndCheck("Enable auto replies", AyuConfig.aiReplyEnabled, true);
                    } else if (position == privateOnlyRow) {
                        checkCell.setTextAndCheck("Private chats only", AyuConfig.aiPrivateOnly, true);
                    } else if (position == splitRepliesRow) {
                        checkCell.setTextAndCheck("Split reply lines", AyuConfig.aiSplitReplies, true);
                    }
                    break;
            }
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull android.view.ViewGroup parent, int viewType) {
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == bridgeDividerRow) {
                return 1;
            }
            if (position == bridgeHeaderRow) {
                return 3;
            }
            if (position == enabledRow || position == privateOnlyRow || position == splitRepliesRow) {
                return 5;
            }
            return 2;
        }
    }

    private String shortValue(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('\n', ' ').trim();
        int max = AndroidUtilities.dp(1) > 0 ? 42 : 42;
        return clean.length() > max ? clean.substring(0, max) + "..." : clean;
    }
}
