package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
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
import earth.terrarium.heracles.common.utils.ItemValue;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    
    private float scaleFactor;

    public QuestWidget(ClientQuests.QuestEntry entry, ModUtils.QuestStatus status, Long tasksLeft) {
        this.entry = entry;
        this.quest = entry.value();
        this.status = status;
        this.id = entry.key();
        this.tasksLeft = tasksLeft;
        this.scaleFactor = quest.display().scaleFactor();
    }

    public void render(GuiGraphics graphics, ScissorBoxStack scissor, int x, int y, int mouseX, int mouseY, boolean hovered, float ignoredPartialTicks) {
    	this.scaleFactor = quest.display().scaleFactor();
    	
        hovered = hovered && isMouseOver(mouseX - x, mouseY - y);
        info = TexturePlacements.getOrDefault(quest.display().iconBackground(), TexturePlacements.NO_OFFSET_24X);

        RenderSystem.enableBlend();
        
        int backgroundX = x + x() + info.xOffset();
        int backgroundY = y + y() + info.yOffset();
        int backgroundWidth = Math.round(info.width() * scaleFactor);
        int backgroundHeight = Math.round(info.height() * scaleFactor);
        graphics.blit(
        	quest.display().iconBackground(),
        	backgroundX,
        	backgroundY,
        	Math.round(status.ordinal() * info.width() * scaleFactor), // uOffset
            0,                                                     // vOffset
            backgroundWidth,                        // width
            backgroundHeight,                       // height
            Math.round(info.width() * 5 * scaleFactor), // texture width
            Math.round(info.height() * scaleFactor)     // texture height
        );

        if (hovered) {
            graphics.blit(quest.display().iconBackground(),
            	backgroundX,
            	backgroundY,
            	Math.round(4 * info.width() * scaleFactor),
                0,
                backgroundWidth,
                backgroundHeight,
                Math.round(info.width() * 5 * scaleFactor),
                Math.round(info.height() * scaleFactor)
            );
        }
        RenderSystem.disableBlend();
        
        int iconX = backgroundX + (int) Math.round(0.1667 * backgroundWidth);
        int iconY = backgroundY + (int) Math.round(0.1667 * backgroundHeight);
        
        ItemStack itemIconStack = quest.display().icon().getItem().getDefaultInstance();
        if (itemIconStack != null && !itemIconStack.is(Items.AIR)) {
            try (var pose = new CloseablePoseStack(graphics)) {
                pose.translate(iconX, iconY, 0);
                pose.scale(scaleFactor, scaleFactor, 1);
                graphics.renderFakeItem(itemIconStack, 0, 0);
            }
        }
        
        CursorUtils.setCursor(hovered, CursorScreen.Cursor.POINTER);
        
        if (hovered && (!(ClientUtils.screen() instanceof QuestsScreen screen) || !screen.isTemporaryWidgetVisible())) {
            List<Component> lines = new ArrayList<>();
            String subtitleText = quest.display().subtitle().getString();
            
            lines.add(quest.display().title().copy().withStyle(ChatFormatting.BOLD));
            
            if(this.tasksLeft != null && this.tasksLeft > 0) {
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
    
                if(!subtitleText.isBlank()) lines.add(Component.literal(""));
            }

            if (!subtitleText.isBlank()) {
                String[] subtitleLines = subtitleText.split("\n");
                for (String line : subtitleLines) {
                    lines.add(Component.literal(line));
                }
            }

            switch (status) {
			case COMPLETED:
				lines.add(ConstantComponents.Quests.CLAIMABLE);
			case COMPLETED_CLAIMED:
            	String completedUppercased = Component.translatable("quest.heracles.completed").getString().toUpperCase();
            	lines.add(Component.literal(completedUppercased).withStyle(ChatFormatting.GREEN));
			default:
				break;
            }
            
            ScreenUtils.setTooltip(lines, false);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int backgroundWidth = Math.round(info.width() * scaleFactor);
        int backgroundHeight = Math.round(info.height() * scaleFactor);
    	
        return mouseX >= x() &&
        		mouseX <= x() + backgroundWidth &&
        			mouseY >= y() &&
        				mouseY <= y() + backgroundHeight;
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
