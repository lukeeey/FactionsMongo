package io.github.lukeeey.factionsmongodb.storage;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.MemoryFPlayer;
import com.massivecraft.factions.landraidcontrol.PowerControl;

public class MongoFPlayer extends MemoryFPlayer {

    public MongoFPlayer(MemoryFPlayer arg0) {
        super(arg0);
    }

    public MongoFPlayer(String id) {
        super(id);
    }

    @Override
    public void remove() {
        ((MongoFPlayers) FPlayers.getInstance()).fPlayers.remove(getId());
    }

    @Override
    public boolean shouldBeSaved() {
        return this.hasFaction() ||
                (FactionsPlugin.getInstance().getLandRaidControl() instanceof PowerControl &&
                        (this.getPowerRounded() != this.getPowerMaxRounded() &&
                                this.getPowerRounded() != (int) Math.round(FactionsPlugin.getInstance().conf().factions().landRaidControl().power().getPlayerStarting())));
    }
}
