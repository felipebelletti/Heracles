package earth.terrarium.heracles.client.screens.quest;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.screens.quest.tasks.TaskListWidget;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.menus.quest.QuestContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import earth.terrarium.hermes.api.TagProvider;
import earth.terrarium.hermes.api.themes.DefaultTheme;
import earth.terrarium.hermes.client.DocumentWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;
import earth.terrarium.heracles.common.constants.GuiConstants;
import earth.terrarium.heracles.client.HeraclesClient;

public class QuestScreen extends BaseQuestScreen {

    private TaskListWidget taskList;
    private RewardListWidget rewardList;
    private DocumentWidget description;

    private String descriptionError;
    private String desc;

    private int contentX;
    private int contentHeight;
    private static final int CONTENT_Y = 15;

    public QuestScreen(QuestContent content) {
        super(content);
        HeraclesClient.lastOpenedQuestId = content.id();
    }

    @Override
    public void updateProgress(@Nullable QuestProgress newProgress) {
        super.updateProgress(newProgress);
        this.taskList.update(this.quest().tasks().values());
        this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());

        calculateContentArea();

        TagProvider provider = new QuestTagProvider();
        this.description = new DocumentWidget(contentX, CONTENT_Y + GuiConstants.WINDOW_PADDING_Y, questContentWidth, contentHeight, 5.0D, 5.0D, new DefaultTheme(), provider.parse(desc));
    }

    @Override
    protected void init() {
        super.init();

        calculateContentArea();

        this.taskList = new TaskListWidget(contentX, CONTENT_Y + GuiConstants.WINDOW_PADDING_Y, questContentWidth, contentHeight, 5.0D, 5.0D, this.content.id(), this.entry(), this.content.progress(), this.content.quests(), null, null);
        this.rewardList = new RewardListWidget(contentX, CONTENT_Y + GuiConstants.WINDOW_PADDING_Y, questContentWidth, contentHeight, 5.0D, 5.0D, this.entry(), this.content.progress(), null, null);
        try {
            this.descriptionError = null;
            this.desc = String.join("", MarkdownParser.parse(this.quest().display().description()));
        } catch (Throwable e) {
            this.descriptionError = e.getMessage();
            Heracles.LOGGER.error("Error parsing quest description: ", e);
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
            addRenderableWidget(new ImageButton(this.width - 24 - GuiConstants.WINDOW_PADDING_X, 1 + GuiConstants.WINDOW_PADDING_Y, 11, 11, 33, 15, 11, HEADING, 256, 256, (button) ->
                NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(this.content.fromGroup(), this.content.id(), true))
            )).setTooltip(Tooltip.create(ConstantComponents.TOGGLE_EDIT));
        }
        updateProgress(null);
    }

    private void calculateContentArea() {
        contentX = (int) (questContentCenter() - (questContentWidth / 2f) + 0.5f) + GuiConstants.WINDOW_PADDING_X;
        contentHeight = this.height - 15 - 2 * GuiConstants.WINDOW_PADDING_Y;
    }

    @Override
    public GuiEventListener getTaskList() {
        return this.taskList;
    }

    @Override
    public GuiEventListener getRewardList() {
        return this.rewardList;
    }

    @Override
    public GuiEventListener getDescriptionWidget() {
        return this.description;
    }

    @Override
    public String getDescriptionError() {
        return this.descriptionError;
    }
}
