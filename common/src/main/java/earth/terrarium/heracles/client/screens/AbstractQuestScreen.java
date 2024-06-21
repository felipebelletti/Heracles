package earth.terrarium.heracles.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.screens.BaseCursorScreen;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.client.theme.QuestsScreenTheme;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.client.widgets.base.TemporaryWidget;
import earth.terrarium.heracles.client.widgets.modals.EditObjectModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import earth.terrarium.heracles.common.constants.GuiConstants;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractQuestScreen<T> extends BaseCursorScreen {

    public static final ResourceLocation HEADING = new ResourceLocation(Heracles.MOD_ID, "textures/gui/heading.png");

    protected final List<TemporaryWidget> temporaryWidgets = new ArrayList<>();
    protected boolean hasBackButton = true;

    protected final T content;

    protected static final float SIDE_BAR_PORTION = 0.17f;
    protected static int sideBarWidth;

    protected static final float QUEST_CONTENT_PORTION = 0.66f;
    protected static int questContentWidth;

    public AbstractQuestScreen(T content, Component component) {
        super(component);
        this.content = content;
    }

    @Override
    protected void init() {
        super.init();
        sideBarWidth = (int) (width * SIDE_BAR_PORTION) - 2;
        questContentWidth = (int) (width * QUEST_CONTENT_PORTION);

        if (hasBackButton) {
            addRenderableWidget(new ImageButton(GuiConstants.WINDOW_PADDING_X + 1, GuiConstants.WINDOW_PADDING_Y + 1, 11, 11, 0, 15, 11, HEADING, 256, 256, (button) ->
                goBack()
            )).setTooltip(Tooltip.create(CommonComponents.GUI_BACK));
        }
        addRenderableWidget(new ImageButton(this.width - GuiConstants.WINDOW_PADDING_X - 12, GuiConstants.WINDOW_PADDING_Y + 1, 11, 11, 11, 15, 11, HEADING, 256, 256, (button) -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
        })).setTooltip(Tooltip.create(ConstantComponents.CLOSE));
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.temporaryWidgets.clear();
    }

    public <R extends Renderable & TemporaryWidget> R addTemporary(R renderable) {
        addRenderableOnly(renderable);
        this.temporaryWidgets.add(renderable);
        return renderable;
    }

    public List<TemporaryWidget> temporaryWidgets() {
        return this.temporaryWidgets;
    }

    protected void goBack() {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int i, int j, float f) {
        this.renderBg(graphics, f, i, j);
        super.render(graphics, i, j, f);
        this.renderLabels(graphics, i, j);
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int adjustedWidth = width - 2 * GuiConstants.WINDOW_PADDING_X;
        int adjustedHeight = height - 2 * GuiConstants.WINDOW_PADDING_Y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (drawSidebar()) {
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X, 15 + GuiConstants.WINDOW_PADDING_Y, sideBarWidth, adjustedHeight - 15, 0, 128, 128, 128); // Side Background
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X + sideBarWidth + 2, 15 + GuiConstants.WINDOW_PADDING_Y, adjustedWidth - sideBarWidth, adjustedHeight - 15, 128, 128, 128, 128); // Main Background
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X, GuiConstants.WINDOW_PADDING_Y, sideBarWidth, 15, 0, 0, 128, 15); // Side Header
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X + sideBarWidth + 2, GuiConstants.WINDOW_PADDING_Y, adjustedWidth - sideBarWidth, 15, 130, 0, 126, 15); // Main Header
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X + sideBarWidth, GuiConstants.WINDOW_PADDING_Y, 2, 15, 128, 0, 2, 15); // Header Separator
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X + sideBarWidth, 15 + GuiConstants.WINDOW_PADDING_Y, 2, adjustedHeight - 15, 128, 15, 2, 113); // Body Separator
        } else {
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X, 15 + GuiConstants.WINDOW_PADDING_Y, adjustedWidth, adjustedHeight - 15, 128, 128, 128, 128); // Main Background
            ClientUtils.blitTiling(graphics, HEADING, GuiConstants.WINDOW_PADDING_X, GuiConstants.WINDOW_PADDING_Y, adjustedWidth, 15, 130, 0, 126, 15); // Main Header
        }
        RenderSystem.disableBlend();
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int center = questContentCenter();
        Component title = getTitle();
        graphics.drawString(
            this.font,
            title, (int) (center - (this.font.width(title) / 2f)), GuiConstants.WINDOW_PADDING_Y + 3, QuestsScreenTheme.getHeaderTitle(),
            false
        );
    }

    public boolean isTemporaryWidgetVisible() {
        for (TemporaryWidget widget : temporaryWidgets) {
            if (widget.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        boolean visible = false;
        for (TemporaryWidget widget : this.temporaryWidgets) {
            visible |= widget.isVisible();
            if (widget.isVisible() && widget instanceof GuiEventListener listener) {
                return listener;
            }
        }
        if (visible) {
            return null;
        }
        return super.getFocused();
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        List<GuiEventListener> listeners = new ArrayList<>();
        for (TemporaryWidget widget : temporaryWidgets) {
            if (widget.isVisible() && widget instanceof GuiEventListener listener) {
                listeners.add(listener);
            }
        }
        if (!listeners.isEmpty()) {
            return listeners;
        }
        return super.children();
    }

    public List<? extends GuiEventListener> actualChildren() {
        return super.children();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (this instanceof InternalKeyPressHook hook) {
            return hook.heracles$internalKeyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
    }

    public EditObjectModal findOrCreateEditWidget() {
        boolean found = false;
        EditObjectModal widget = new EditObjectModal(this.width, this.height);
        for (TemporaryWidget temporaryWidget : this.temporaryWidgets()) {
            if (temporaryWidget instanceof EditObjectModal modal) {
                found = true;
                widget = modal;
                break;
            }
        }
        widget.setVisible(true);
        if (!found) {
            this.addTemporary(widget);
        }
        return widget;
    }

    public int questContentCenter() {
        // float, to avoid truncating (effectively rounding when 0.5f is added) twice
        float nonSideBarWidth = width - (width * SIDE_BAR_PORTION);
        return drawSidebar() ?
            (int) (0.5f + (width - (nonSideBarWidth / 2f))) :
            (int) (0.5f + (width / 2f));
    }

    public boolean drawSidebar() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
