package net.methadrenaline.smpcreative.mavelocore.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class PublicAnnouncementsTest {
    @Test
    void forwardsOneLocalizedMessagePerNetworkEventAndUnsubscribes() throws Exception {
        var received = new ArrayList<Component>();
        var announcements = new PublicAnnouncements(error -> { throw error; });
        var subscription = announcements.subscribe("ru_ru", received::add);
        announcements.publish("PlayerOne", true);
        announcements.publish("PlayerOne", false);
        assertEquals(List.of(NetworkJoinMessages.message("ru", "PlayerOne", true),
                NetworkJoinMessages.message("ru", "PlayerOne", false)), received);
        subscription.close();
        announcements.publish("PlayerTwo", true);
        assertEquals(2, received.size());
    }

    @Test
    void failingListenerDoesNotBlockOthersAndShutdownClearsSubscriptions() {
        var errors = new ArrayList<RuntimeException>();
        var received = new ArrayList<Component>();
        var announcements = new PublicAnnouncements(errors::add);
        announcements.subscribe("en_us", message -> { throw new IllegalStateException(); });
        announcements.subscribe("en_us", received::add);
        announcements.publish("PlayerOne", false);
        assertEquals(1, errors.size());
        assertEquals(List.of(NetworkJoinMessages.message("en", "PlayerOne", false)), received);
        announcements.clear();
        announcements.publish("PlayerOne", true);
        assertEquals(1, received.size());
    }
}
