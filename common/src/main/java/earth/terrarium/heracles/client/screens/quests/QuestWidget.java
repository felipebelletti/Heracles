package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.scissor.ScissorBoxStack;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import com.teamresourceful.resourcefullib.client.utils.CursorUtils;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class QuestWidget {

    private final ClientQuests.QuestEntry entry;
    private final Quest quest;
    private final ModUtils.QuestStatus status;
    private final String id;
    private final Long tasksLeft;

    private TexturePlacements.Info info = TexturePlacements.NO_OFFSET_24X;

    public QuestWidget(ClientQuests.QuestEntry entry, ModUtils.QuestStatus status, Long tasksLeft) {
        this.entry = entry;
        this.quest = entry.value();
        this.status = status;
        this.id = entry.key();
        this.tasksLeft = tasksLeft;
    }

    public void render(GuiGraphics graphics, ScissorBoxStack scissor, int x, int y, int mouseX, int mouseY, boolean hovered, float ignoredPartialTicks) {
        hovered = hovered && isMouseOver(mouseX - x, mouseY - y);

        info = TexturePlacements.getOrDefault(quest.display().iconBackground(), TexturePlacements.NO_OFFSET_24X);

        RenderSystem.enableBlend();
        
        graphics.blit(quest.display().iconBackground(),
            x + x() + info.xOffset(), y + y() + info.yOffset(),
            status.ordinal() * info.width(), 0,
            info.width(), info.height(),
            info.width() * 5, info.height()
        );

        if (hovered) {
            graphics.blit(quest.display().iconBackground(),
                x + x() + info.xOffset(), y + y() + info.yOffset(),
                4 * info.width(), 0,
                info.width(), info.height(),
                info.width() * 5, info.height()
            );
        }
        RenderSystem.disableBlend();
        quest.display().icon().render(graphics, scissor, x + x() + 4, y + y() + 4, 24, 24);
        CursorUtils.setCursor(hovered, CursorScreen.Cursor.POINTER);
        if (hovered && (!(ClientUtils.screen() instanceof QuestsScreen screen) || !screen.isTemporaryWidgetVisible())) {
            List<Component> lines = new ArrayList<>();
            lines.add(quest.display().title().copy().withStyle(ChatFormatting.BOLD));
            
            if(this.tasksLeft > 0) {
            	int totalTasks = quest.tasks().size();
            	lines.add(Component.literal(totalTasks - this.tasksLeft + "/" + totalTasks + " Tasks Complete"));            	
            }
            
            if(this.status == ModUtils.QuestStatus.LOCKED && quest.dependencies().size() > 0) {
            	lines.add(Component.literal("Requires:")
            			.withStyle(ChatFormatting.RED)
            			.withStyle(ChatFormatting.UNDERLINE));
            	
                quest.dependencies().forEach(
                    dependencyName -> lines.add(
                            Component.literal(String.format("- %s", dependencyName))
                                    .withStyle(ChatFormatting.RED)));
    
            	
            	lines.add(Component.literal(""));
            }

            String subtitleText = quest.display().subtitle().getString();
            if (!subtitleText.isBlank()) {
                String[] subtitleLines = subtitleText.split("\n");
                for (String line : subtitleLines) {
                    lines.add(Component.literal(line));
                }
            }

            if (status == ModUtils.QuestStatus.COMPLETED) lines.add(ConstantComponents.Quests.CLAIMABLE);

            ScreenUtils.setTooltip(lines, false);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x() && mouseX <= x() + 24 && mouseY >= y() && mouseY <= y() + 24;
    }

    public int x() {
        return position().x();
    }

    public int y() {
        return position().y();
    }

    public Vector2i position() {
        if (ClientUtils.screen() instanceof QuestsScreen screen) {
            return this.quest.display().position(screen.getGroup());
        }
        return new Vector2i();
    }

    public String group() {
        if (ClientUtils.screen() instanceof QuestsScreen screen) {
            return screen.getGroup();
        }
        return "";
    }

    public Quest quest() {
        return this.quest;
    }

    public ClientQuests.QuestEntry entry() {
        return this.entry;
    }

    public String id() {
        return this.id;
    }

    public TexturePlacements.Info getTextureInfo() {
        return this.info;
    }
}
