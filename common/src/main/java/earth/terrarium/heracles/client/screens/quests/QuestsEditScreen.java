package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.QuestSettings;
import earth.terrarium.heracles.client.handlers.ClientQuestNetworking;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.mousemode.MouseButtonType;
import earth.terrarium.heracles.client.screens.mousemode.MouseMode;
import earth.terrarium.heracles.client.screens.mousemode.MouseModeButton;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.widgets.SelectableImageButton;
import earth.terrarium.heracles.client.widgets.modals.AddDependencyModal;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import earth.terrarium.heracles.client.widgets.modals.TextInputModal;
import earth.terrarium.heracles.client.widgets.modals.icon.background.IconBackgroundModal;
import earth.terrarium.heracles.client.widgets.modals.upload.UploadModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.menus.quests.QuestsContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.CreateGroupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import org.joml.Vector2i;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public class QuestsEditScreen extends QuestsScreen {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\x00-\\x7F]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");
    private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:CLOCK\\$|CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);

    private SelectableImageButton moveTool;
    private SelectableImageButton dragTool;
    private SelectableImageButton addTool;
    private SelectableImageButton linkTool;

    private UploadModal uploadModal;
    private TextInputModal<Unit> groupModal;
    private IconBackgroundModal iconBackgroundModal;
    private ItemModal itemModal;
    private AddDependencyModal dependencyModal;
    private TextInputModal<MouseClick> questModal;

    public QuestsEditScreen(QuestsContent content) {
        super(content);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ImageButton(sideBarWidth - 12, 1, 11, 11, 22, 15, 11, HEADING, 256, 256, (button) -> {
            if (this.groupModal != null) {
                this.groupModal.setVisible(true);
            }
        })).setTooltip(Tooltip.create(ConstantComponents.Groups.CREATE));

        selectQuestWidget = new SelectQuestWidget(
            (int) (this.width * 0.75f) + 2,
            15,
            sideBarWidth,
            this.height - 15,
            this.questsWidget
        );

        this.moveTool = addRenderableWidget(new MouseModeButton(sideBarWidth + 3, 1, MouseButtonType.MOVE, () -> {
            if (questsWidget.selectHandler().selectedQuest() != null) {
                if (!actualChildren().contains(selectQuestWidget)) {
                    addRenderableWidget(selectQuestWidget);
                }
                selectQuestWidget.setEntry(questsWidget.selectHandler().selectedQuest().entry());
            }
        }));
        this.moveTool.setTooltip(Tooltip.create(ConstantComponents.Tools.MOVE));

        this.dragTool = addRenderableWidget(new MouseModeButton(sideBarWidth + 15, 1, MouseButtonType.DRAG, this::clearWidget));
        this.dragTool.setTooltip(Tooltip.create(ConstantComponents.Tools.DRAG));

        this.addTool = addRenderableWidget(new MouseModeButton(sideBarWidth + 27, 1, MouseButtonType.ADD, this::clearWidget));
        this.addTool.setTooltip(Tooltip.create(ConstantComponents.Tools.ADD_QUEST));

        this.linkTool = addRenderableWidget(new MouseModeButton(sideBarWidth + 39, 1, MouseButtonType.LINK, this::clearWidget));
        this.linkTool.setTooltip(Tooltip.create(ConstantComponents.Tools.LINK));

        addRenderableWidget(new ImageButton(this.width - 36, 1, 11, 11, 33, 37, 11, HEADING, 256, 256, (button) -> {
            if (this.uploadModal != null) {
                this.uploadModal.setVisible(true);
            }
        })).setTooltip(Tooltip.create(ConstantComponents.Quests.IMPORT));

        this.uploadModal = addTemporary(new UploadModal(this.width, this.height));
        this.groupModal = addTemporary(new TextInputModal<>(this.width, this.height, ConstantComponents.Groups.CREATE, (ignored, text) -> {
            NetworkHandler.CHANNEL.sendToServer(new CreateGroupPacket(text));
            ClientQuests.groups().add(text);
            if (Minecraft.getInstance().screen instanceof QuestsScreen screen) {
                screen.getGroupsList().addGroup(text);
            }
        }, text -> !ClientQuests.groups().contains(text.trim())));
        this.iconBackgroundModal = addTemporary(new IconBackgroundModal(this.width, this.height));
        this.itemModal = addTemporary(new ItemModal(this.width, this.height));
        this.dependencyModal = addTemporary(new AddDependencyModal(this.width, this.height));
        this.questModal = addTemporary(new TextInputModal<>(this.width, this.height, ConstantComponents.Quests.CREATE, (position, text) -> {
            MouseClick local = this.questsWidget.getLocal(position);
            QuestDisplay display = QuestDisplay.createDefault(new GroupDisplay(
                this.content.group(),
                new Vector2i((int) local.x() - 12, (int) local.y() - 12)
            ));
            display.setTitle(Component.literal(text));
            Quest quest = new Quest(
                display,
                QuestSettings.createDefault(),
                new HashSet<>(),
                new HashMap<>(),
                new HashMap<>()
            );
            this.questsWidget.addQuest(ClientQuestNetworking.add(toSafeFilename(text), quest));
        }, text -> {
            String id = toSafeFilename(text);
            return id.length() >= 2 && ClientQuests.get(id).isEmpty();
        }));
    }

    private static String toSafeFilename(String text) {
        text = text.trim();
        text = text.replace(" ", "_");
        text = NON_ASCII.matcher(text).replaceAll(result -> Base64.getEncoder().encodeToString(result.group().getBytes(StandardCharsets.UTF_8)));
        text = NON_ALPHANUMERIC.matcher(text).replaceAll("");
        text = text.toLowerCase(Locale.ROOT);
        if (RESERVED_WINDOWS_FILENAMES.matcher(text).matches()) {
            return "";
        }
        return text;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (dragTool.isSelected()) {
            setCursor(Cursor.RESIZE_ALL);
        }
        if (addTool.isSelected()) {
            setCursor(Cursor.CROSSHAIR);
        }
        super.renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected MouseMode getMouseMode() {
        if (dragTool.isSelected()) {
            return MouseMode.DRAG_MOVE;
        } else if (addTool.isSelected()) {
            return MouseMode.ADD;
        } else if (linkTool.isSelected()) {
            return MouseMode.SELECT_LINK;
        }
        return MouseMode.SELECT_MOVE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isTemporaryWidgetVisible() && getMouseMode() == MouseMode.ADD && this.questsWidget.isMouseOver(mouseX, mouseY) && button == 0) {
            this.questModal.setVisible(true);
            this.questModal.setData(new MouseClick(mouseX, mouseY, button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (selectQuestWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isTemporaryWidgetVisible()) return false;
        return switch (keyCode) {
            case InputConstants.KEY_V -> {
                moveTool.setSelected(true);
                yield true;
            }
            case InputConstants.KEY_H -> {
                dragTool.setSelected(true);
                yield true;
            }
            case InputConstants.KEY_U -> {
                addTool.setSelected(true);
                yield true;
            }
            case InputConstants.KEY_L -> {
                linkTool.setSelected(true);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public void removed() {
        super.removed();
        uploadModal.setVisible(false);
    }

    private void clearWidget() {
        if (selectQuestWidget != null) {
            selectQuestWidget.setEntry(null);
            removeWidget(selectQuestWidget);
        }
    }

    public IconBackgroundModal iconBackgroundModal() {
        return this.iconBackgroundModal;
    }

    public ItemModal itemModal() {
        return this.itemModal;
    }

    public AddDependencyModal dependencyModal() {
        return this.dependencyModal;
    }

    public TextInputModal<MouseClick> questModal() {
        return this.questModal;
    }
}