package co.uk.loopylucy.kinkstuff.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientLeashTracker {
    private static final Map<UUID, UUID> CLIENT_MAP = new HashMap<>();

    public static void update(UUID target, UUID holder) {
        if (holder == null) {
            CLIENT_MAP.remove(target);
        } else {
            CLIENT_MAP.put(target, holder);
        }
    }

    public static UUID getHolderFor(UUID target) {
        return CLIENT_MAP.get(target);
    }
}
