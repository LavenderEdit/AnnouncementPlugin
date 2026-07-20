package dev.studio.announcer.api.audience;

import dev.studio.announcer.domain.announcement.Announcement;

public interface Audience {
    
    static Audience all() {
        return AllAudience.INSTANCE;
    }
    
    static Audience actor() {
        return ActorAudience.INSTANCE;
    }
    
    static Audience others() {
        return OthersAudience.INSTANCE;
    }
    
    static Audience player(String playerId) {
        return new PlayerAudience(playerId);
    }

    static Audience fromMetadata(Announcement announcement) {
        String audienceTypeStr = announcement.metadata().getOrDefault("audience", "all").toLowerCase(java.util.Locale.ROOT);
        if (audienceTypeStr.equals("actor") || audienceTypeStr.equals("player")) {
            return actor();
        } else if (audienceTypeStr.equals("others") || audienceTypeStr.equals("exclude_actor")) {
            return others();
        } else {
            return all();
        }
    }
}
