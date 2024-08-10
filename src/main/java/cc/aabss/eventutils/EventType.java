package cc.aabss.eventutils;

import cc.aabss.eventutils.config.ConfigScreen;
import cc.aabss.eventutils.config.EventConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static cc.aabss.eventutils.EventUtils.translate;
import static net.minecraft.text.Text.translatable;


public enum EventType {
    FAMOUS(translatable("eventutils.famous.display"),
            translatable("eventutils.famous.new").formatted(Formatting.AQUA)),
    POTENTIAL_FAMOUS(translatable("eventutils.potential_famous.display"),
            translatable("eventutils.potential_famous.new").formatted(Formatting.DARK_AQUA)),
    PARTNER(translatable("eventutils.partner.display"),
            translatable("eventutils.partner.new").formatted(Formatting.LIGHT_PURPLE)),
    COMMUNITY(translatable("eventutils.community.display"),
            translatable("eventutils.community.new").formatted(Formatting.DARK_GRAY)),
    MONEY(translatable("eventutils.money.display"),
            prize -> translatable("eventutils.money.new").formatted(Formatting.GREEN).append("($" + prize + ")").formatted(Formatting.GRAY)),
    FUN(translatable("eventutils.fun.display"),
            translatable("eventutils.fun.new").formatted(Formatting.RED)),
    HOUSING(translatable("eventutils.housing.display"),
            translatable("eventutils.housing.new").formatted(Formatting.GOLD)),
    CIVILIZATION(translatable("eventutils.civilization.display"),
            translatable("eventutils.civilization.new").formatted(Formatting.BLUE));

    @NotNull private static final Map<Long, EventType> FROM_ROLE_ID = Map.of(
            970434201990070424L, PARTNER,
            980950599946362900L, COMMUNITY,
            970434305203511359L, MONEY,
            970434303391576164L, FUN,
            970434294893928498L, HOUSING,
            1134932175821734119L, CIVILIZATION);

    @NotNull public final MutableText displayName;
    @NotNull public final String displayNameString;
    @NotNull public final Function<Integer, MutableText> toast;

    EventType(@NotNull MutableText displayName, @NotNull Function<Integer, MutableText> toast) {
        this.displayName = displayName;
        this.displayNameString = this.name().toLowerCase().replace("_", "");
        this.toast = toast;
    }

    EventType(@NotNull MutableText displayName, @NotNull MutableText toast) {
        this(displayName, prize -> toast);
    }

    @NotNull
    public Option<Boolean> getOption(@NotNull EventConfig config) {
        return Option.<Boolean>createBuilder()
                .name(displayName)
                .description(OptionDescription.of(Text.of(Language.getInstance().get("eventutils.config.eventdescription").replace("{event}", displayNameString.toLowerCase()))))
                .binding(true, () -> config.eventTypes.contains(this), newValue -> {
                    if (Boolean.TRUE.equals(newValue)) {
                        config.eventTypes.add(this);
                    } else {
                        config.eventTypes.remove(this);
                    }
                    config.setSave("notifications", config.eventTypes);
                })
                .controller(ConfigScreen::getBooleanBuilder)
                .build();
    }

    public void sendToast(@Nullable Integer prize) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        String[] args = translate("eventutils.event.teleport").split(" \\{command} ");
        final MutableText description = Text.literal(args[0]).formatted(Formatting.WHITE)
                .append("/eventtp " + name().toLowerCase()).formatted(Formatting.YELLOW)
                .append(args[1]).formatted(Formatting.WHITE);
        client.getToastManager().add(new NotificationToast(toast.apply(prize), description));
        if (client.player != null) client.player.playSound(SoundEvent.of(Identifier.of("eventutils", "alert")), 1 ,1);
    }

    @Nullable
    public static EventType fromString(@NotNull String eventType) {
        try {
            return EventType.valueOf(eventType.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static EventType fromRoleId(long roleId) {
        return FROM_ROLE_ID.get(roleId);
    }

    @NotNull
    public static Set<EventType> fromJson(@NotNull JsonObject json) {
        final Set<EventType> eventTypes = new HashSet<>();
        final JsonArray roles = json.getAsJsonArray("roles");
        if (roles == null) return eventTypes;
        for (final JsonElement role : roles) {
            final EventType eventType = fromRoleId(role.getAsLong());
            if (eventType != null) eventTypes.add(eventType);
        }
        return eventTypes;
    }
}
