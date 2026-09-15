package net.methadrenaline.smpcreative.mavelocore.network;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.methadrenaline.smpcreative.mavelocore.lang.LanguageCode;

public final class PublicAnnouncements {
    private record Subscription(String language, Consumer<Component> listener) {}

    private final CopyOnWriteArrayList<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private final Consumer<RuntimeException> onError;

    public PublicAnnouncements(Consumer<RuntimeException> onError) {
        this.onError = Objects.requireNonNull(onError);
    }

    public AutoCloseable subscribe(String language, Consumer<Component> listener) {
        var subscription = new Subscription(LanguageCode.normalize(language), Objects.requireNonNull(listener));
        subscriptions.add(subscription);
        return () -> subscriptions.remove(subscription);
    }

    public void publish(String username, boolean join) {
        for (var subscription : subscriptions) {
            try {
                subscription.listener().accept(NetworkJoinMessages.message(subscription.language(), username, join));
            } catch (RuntimeException exception) {
                onError.accept(exception);
            }
        }
    }

    public void clear() {
        subscriptions.clear();
    }
}
