package earth.terrarium.heracles.common.menus.quests;

import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public record QuestsContent(
    String group,
    Map<String, ModUtils.QuestStatus> questId_to_questStatus,
    Map<String, Long> questId_to_tasksLeft,
    boolean canEdit
) {

    public static QuestsContent from(FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        
        Map<String, ModUtils.QuestStatus> questId_to_questStatus = new HashMap<>();
        int questId_to_questStatus_size = buffer.readVarInt();
        for (int i = 0; i < questId_to_questStatus_size; i++) {
        	questId_to_questStatus.put(buffer.readUtf(), buffer.readEnum(ModUtils.QuestStatus.class));
        }
        
        Map<String, Long> questId_to_tasksLeft = new HashMap<>();
        int questId_to_tasksLeft_size = buffer.readVarInt();
        for (int i = 0; i < questId_to_tasksLeft_size; i++) {
        	questId_to_tasksLeft.put(buffer.readUtf(), buffer.readLong());
        }
        
        return new QuestsContent(group, questId_to_questStatus, questId_to_tasksLeft, buffer.readBoolean());
    }

    public void to(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.group);
        buffer.writeVarInt(this.questId_to_questStatus.size());
        for (var entry : this.questId_to_questStatus.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeEnum(entry.getValue());
        }
        
        buffer.writeVarInt(this.questId_to_tasksLeft.size());
        for (var entry : this.questId_to_tasksLeft.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeLong(entry.getValue());
        }
        
        buffer.writeBoolean(this.canEdit());
    }
}
