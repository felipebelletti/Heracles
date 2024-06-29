package earth.terrarium.heracles.client.screens.quest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import earth.terrarium.heracles.api.client.theme.EditorTheme;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.constants.GuiConstants;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.menus.quest.QuestContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import earth.terrarium.heracles.common.network.packets.rewards.ClaimRewardsPacket;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import earth.terrarium.heracles.client.HeraclesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseQuestScreen extends AbstractQuestScreen<QuestContent> {

    @Nullable
    private Button claimRewards;
    @Nullable
    private QuestProgressWidget progressWidget;

    public int claimRewardsButtonHeight = 20;
    private int separatorLineX;

    public BaseQuestScreen(QuestContent content) {
        super(content,
                Optionull.mapOrDefault(quest(content), quest -> quest.display().title(), CommonComponents.EMPTY));
        ClientQuests.mergeProgress(Map.of(content.id(), content.progress()));
    }

    public void updateProgress(@Nullable QuestProgress newProgress) {
        if (newProgress != null) {
            this.content.progress().copyFrom(newProgress);
        }
        if (this.claimRewards != null) {
            this.claimRewards.active = this.content.progress().isComplete()
                    && this.content.progress().claimedRewards().size() < this.quest().rewards().size();
        }
        if (this.progressWidget != null) {
            this.progressWidget.update(this.quest().tasks().size(),
                    (int) this.quest().tasks().values().stream()
                            .filter(t -> this.content.progress().getTask(t).isComplete()).count(),
                    this.quest().rewards().size(), this.content.progress().claimedRewards().size());
        }
    }

    @Override
    protected void init() {
        super.init();
        int claimRewardsButtonWidth = sideBarWidth - 10;
        int goBackButtonWidth = sideBarWidth - 40;
        separatorLineX = (this.width / 2) - 6;

        boolean showRewards = isEditing() || ClientQuests.get(this.content.id())
                .map(ClientQuests.QuestEntry::value)
                .map(Quest::rewards)
                .map(rewards -> !rewards.isEmpty())
                .orElse(false);

        // replaced by the TaskListHeadingWidget aka "Task Completion" status widget
        // this.progressWidget = addRenderableOnly(new QuestProgressWidget(width / 2, 20 + GuiConstants.WINDOW_PADDING_Y, buttonWidth));

        if (showRewards) {
            this.claimRewards = addRenderableWidget(ThemedButton.builder(ConstantComponents.Rewards.CLAIM,
                button -> {
                    NetworkHandler.CHANNEL.sendToServer(new ClaimRewardsPacket(this.content.id()));
                    if (this.claimRewards != null) {
                        this.claimRewards.active = false;
                    }
                }).bounds(
                        this.width - GuiConstants.WINDOW_PADDING_X - claimRewardsButtonWidth - 5,
                        this.height - 25 - GuiConstants.WINDOW_PADDING_Y,
                        claimRewardsButtonWidth,
                        claimRewardsButtonHeight)
                .build());
        }

        addRenderableWidget(ThemedButton.builder(
        	Component.literal("Back"),
            button -> {
                goBack();
            }).bounds(
            		separatorLineX - (goBackButtonWidth / 2),
                    this.height - 25 - GuiConstants.WINDOW_PADDING_Y,
                    goBackButtonWidth,
                    20)
            .build()
        );
    }

    @Override
    protected void goBack() {
        HeraclesClient.lastOpenedQuestId = null;
        NetworkHandler.CHANNEL
                .sendToServer(new OpenGroupPacket(this.content.fromGroup(), this instanceof QuestEditScreen));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();

        // Render Progress Widget
        if (this.progressWidget != null) {
            this.progressWidget.render(graphics, mouseX, mouseY, partialTick);
        }

        // Render Task List
        if (getTaskList() instanceof Renderable renderableTaskList) {
            renderableTaskList.render(graphics, mouseX, mouseY, partialTick);
        }

        // Render Reward List
        if (this.claimRewards != null && getRewardList() instanceof Renderable renderableRewardList) {
            renderableRewardList.render(graphics, mouseX, mouseY, partialTick);
        }

        // Render Description Widget
        if (getDescriptionWidget() instanceof Renderable renderableDescription) {
            renderableDescription.render(graphics, mouseX, mouseY, partialTick);
        }

        // Render Description Error if exists
        if (getDescriptionError() != null) {
            int contentX = GuiConstants.WINDOW_PADDING_X + 4;
            int contentY = 30 + GuiConstants.WINDOW_PADDING_Y;
            int contentWidth = (int) (this.width * 0.63f) - 40 - 2 * GuiConstants.WINDOW_PADDING_X;
            int contentHeight = this.height - 45 - 2 * GuiConstants.WINDOW_PADDING_Y;
            for (FormattedCharSequence sequence : Minecraft.getInstance().font
                    .split(Component.literal(getDescriptionError()), contentWidth)) {
                int textWidth = this.font.width(sequence);
                graphics.drawString(
                        this.font,
                        sequence, (int) (contentX + (contentWidth - textWidth) / 2f),
                        (int) (contentY + (contentHeight - this.font.lineHeight) / 2f), EditorTheme.getError(),
                        false);
                contentY += this.font.lineHeight;
            }
        }

        ClientUtils.drawLine(
                graphics,
                separatorLineX,
                GuiConstants.WINDOW_PADDING_Y + 25,
                separatorLineX,
                this.height - GuiConstants.WINDOW_PADDING_Y - 30,
                255,
                255,
                255,
                1.0f);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>();
        if (getTaskList() != null) {
            children.add(getTaskList());
        }
        if (getRewardList() != null) {
            children.add(getRewardList());
        }
        if (getDescriptionWidget() != null) {
            children.add(getDescriptionWidget());
        }
        children.addAll(super.children());
        return children;
    }

    @Override
    public @NotNull Component getTitle() {
        return content.progress().isComplete()
                ? Component.translatable("gui.heracles.quest.title.complete", super.getTitle())
                : super.getTitle();
    }

    public Quest quest() {
        return quest(this.content);
    }

    public ClientQuests.QuestEntry entry() {
        return ClientQuests.get(this.content.id()).orElse(null);
    }

    public static Quest quest(QuestContent content) {
        return ClientQuests.get(content.id()).map(ClientQuests.QuestEntry::value).orElse(null);
    }

    public abstract GuiEventListener getTaskList();

    public abstract GuiEventListener getRewardList();

    public abstract GuiEventListener getDescriptionWidget();

    public abstract String getDescriptionError();

    public boolean isEditing() {
        return this instanceof QuestEditScreen;
    }

    @Override
    public boolean drawSidebar() {
        return false;
    }

    public String getQuestId() {
        return this.content.id();
    }
}
