package earth.terrarium.heracles.common.network.packets.groups;

import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.groups.Group;
import earth.terrarium.heracles.api.quests.QuestIcon;
import earth.terrarium.heracles.api.quests.QuestIcons;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import earth.terrarium.heracles.client.screens.quests.QuestsScreen;
import net.minecraft.client.Minecraft;



import java.util.Optional;

public record ClientboundUpdateGroupPacket(
    String group,
    Optional<QuestIcon<?>> icon,
    Optional<String> title,
    Optional<String> description
) implements Packet<ClientboundUpdateGroupPacket> {

    public static final ClientboundPacketType<ClientboundUpdateGroupPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundUpdateGroupPacket> type() {
        return TYPE;
    }

    private static class Type implements ClientboundPacketType<ClientboundUpdateGroupPacket>, CodecPacketType<ClientboundUpdateGroupPacket> {
        private static final ByteCodec<ClientboundUpdateGroupPacket> CODEC = ObjectByteCodec.create(
            ByteCodec.STRING.fieldOf(ClientboundUpdateGroupPacket::group),
            QuestIcons.BYTE_CODEC.optionalFieldOf(ClientboundUpdateGroupPacket::icon),
            ByteCodec.STRING.optionalFieldOf(ClientboundUpdateGroupPacket::title),
            ByteCodec.STRING.optionalFieldOf(ClientboundUpdateGroupPacket::description),
            ClientboundUpdateGroupPacket::new
        );

        @Override
        public Class<ClientboundUpdateGroupPacket> type() {
            return ClientboundUpdateGroupPacket.class;
        }

        @Override
        public ResourceLocation id() {
            return new ResourceLocation(Heracles.MOD_ID, "update_group");
        }

        @Override
        public ByteCodec<ClientboundUpdateGroupPacket> codec() {
            return CODEC;
        }

        @Override
        public Runnable handle(ClientboundUpdateGroupPacket message) {
            return () -> {
                Group currentGroup = ClientQuests.groups().get(message.group);

                if (currentGroup == null) {
                    Heracles.LOGGER.warn("Group {} not found", message.group);
                    return;
                }

                Group updatedGroup = new Group(
                    message.icon().or(currentGroup::icon),
                    message.title().orElse(currentGroup.title()),
                    message.description().orElse(currentGroup.description())
                );
                
                ClientQuests.groups().put(message.group, updatedGroup);

                if (Minecraft.getInstance().screen instanceof QuestsScreen questsScreen) {
                    questsScreen.getGroupsList().update(ClientQuests.groups(), questsScreen.getGroup());
                }
            };
        }
    }
}
