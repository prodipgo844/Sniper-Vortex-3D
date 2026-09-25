package com.example.game;

import java.util.ArrayList;
import java.util.List;

public class Mission {
    public enum TargetType {
        STATIC,
        PATROL,
        ALERT_RUN
    }

    public final int id;
    public final String title;
    public final String codename;
    public final String briefing;
    public final String targetDescription;
    public final float distance;
    public final float baseWindSpeed;
    public final int cashReward;
    public final TargetType targetType;
    public final float patrolRadius;
    public final float patrolSpeed;
    public final float targetArmorHp;
    public final boolean isHostageMission;

    public Mission(int id, String title, String codename, String briefing, 
                   String targetDescription, float distance, float baseWindSpeed, 
                   int cashReward, TargetType targetType, float patrolRadius, 
                   float patrolSpeed, float targetArmorHp, boolean isHostageMission) {
        this.id = id;
        this.title = title;
        this.codename = codename;
        this.briefing = briefing;
        this.targetDescription = targetDescription;
        this.distance = distance;
        this.baseWindSpeed = baseWindSpeed;
        this.cashReward = cashReward;
        this.targetType = targetType;
        this.patrolRadius = patrolRadius;
        this.patrolSpeed = patrolSpeed;
        this.targetArmorHp = targetArmorHp;
        this.isHostageMission = isHostageMission;
    }

    public static final List<Mission> MISSIONS = new ArrayList<>();
    static {
        MISSIONS.add(new Mission(
            1,
            "The Sentinel Lookout",
            "OPERATION: EYE IN THE SKY",
            "An enemy insurgent scout is occupying the high tower overlooking our sector. He's relaying scout reports on our convoy. Eliminate him before the convoy passes the gorge.",
            "Wears a dark red tactical vest. Standing still on the water tower platform.",
            320f,
            1.5f,
            400,
            TargetType.STATIC,
            150f,
            2.0f,
            50f,
            false
        ));
        MISSIONS.add(new Mission(
            2,
            "Docks Guard Patrol",
            "OPERATION: IRON CLAW",
            "An armed mercenary is patrolling the shipping container loading yard. He moves consistently, looking for intruders. Time your shot perfectly to avoid drawing harbor attention.",
            "Wears a bright orange cargo belt. Walking back and forth between blue and green cargo containers.",
            550f,
            -4.5f, // West
            800,
            TargetType.PATROL,
            180f,
            1.8f,
            70f,
            false
        ));
        MISSIONS.add(new Mission(
            3,
            "High-Wind Harbor Escape",
            "OPERATION: RAINSTORM",
            "An international arms dealer has boarded a cargo ship deck to escape. A high offshore gale is sweeping the bay. Adjust your crosshairs carefully into the heavy wind to score a hit.",
            "Wears a purple jacket. Standing on the ship bridge deck next to the yellow crane.",
            780f,
            13.0f, // East gale
            1500,
            TargetType.STATIC,
            150f,
            2.0f,
            100f,
            false
        ));
        MISSIONS.add(new Mission(
            4,
            "Hostage Crisis",
            "OPERATION: SILVER SHIELD",
            "A high-ranking rebel has taken a local researcher hostage. They are in the open warehouse doorway. Do NOT hit the hostage under any circumstances. One mistake will abort the campaign.",
            "Target is wearing a red tactical band. Hostage is wearing white and stands directly next to the target. Shoot the red bandit only!",
            620f,
            3.0f,
            2200,
            TargetType.PATROL,
            100f,
            1.0f,
            80f,
            true
        ));
        MISSIONS.add(new Mission(
            5,
            "The Fortress Overlord",
            "OPERATION: APEX ASSASSIN",
            "The syndicate leader is doing a final inspection of his desert bunker. This is extreme long range. The wind is fierce, the target is heavily armored, and if you miss, he will flee to his bunker in 4 seconds.",
            "The Commander with the black cap. Pacing quickly behind fortified barricades.",
            950f,
            -16.0f, // Extreme West gale!
            3500,
            TargetType.ALERT_RUN,
            240f,
            2.5f,
            140f,
            false
        ));
    }

    public static Mission getMissionById(int id) {
        for (Mission m : MISSIONS) {
            if (m.id == id) return m;
        }
        return MISSIONS.get(0);
    }
}
