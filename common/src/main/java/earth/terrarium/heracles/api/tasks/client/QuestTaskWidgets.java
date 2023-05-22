package earth.terrarium.heracles.api.tasks.client;

import earth.terrarium.heracles.api.client.DisplayWidget;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.api.tasks.client.defaults.AdvancementTaskWidget;
import earth.terrarium.heracles.api.tasks.client.defaults.ItemTaskWidget;
import earth.terrarium.heracles.api.tasks.client.defaults.KillEntityTaskWidget;
import earth.terrarium.heracles.api.tasks.client.defaults.RecipeTaskWidget;
import earth.terrarium.heracles.api.tasks.defaults.AdvancementTask;
import earth.terrarium.heracles.api.tasks.defaults.GatherItemTask;
import earth.terrarium.heracles.api.tasks.defaults.KillEntityQuestTask;
import earth.terrarium.heracles.api.tasks.defaults.RecipeTask;
import earth.terrarium.heracles.common.handlers.progress.TaskProgress;
import net.minecraft.Optionull;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class QuestTaskWidgets {

    private static final Map<QuestTaskType<?>, QuestTaskWidgetFactory<?, ?, ?>> FACTORIES = new IdentityHashMap<>();

    public static <I, S extends Tag, T extends QuestTask<I, S, T>> void register(QuestTaskType<T> type, QuestTaskWidgetFactory<I, S, T> factory) {
        FACTORIES.put(type, factory);
    }

    @SuppressWarnings("unchecked")
    public static <I, S extends Tag, T extends QuestTask<I, S, T>> QuestTaskWidgetFactory<I, S, T> getFactory(QuestTaskType<T> type) {
        if (!FACTORIES.containsKey(type)) {
            return null;
        }
        return (QuestTaskWidgetFactory<I, S, T>) FACTORIES.get(type);
    }

    @Nullable
    public static <T extends Tag> DisplayWidget create(QuestTask<?, T, ?> task, TaskProgress<T> progress) {
        return Optionull.map(getFactory(task.type()), factory -> factory.createAndCast(task, progress));
    }

    static {
        register(KillEntityQuestTask.TYPE, KillEntityTaskWidget::new);
        register(GatherItemTask.TYPE, ItemTaskWidget::new);
        register(AdvancementTask.TYPE, AdvancementTaskWidget::new);
        register(RecipeTask.TYPE, RecipeTaskWidget::new);
    }
}
