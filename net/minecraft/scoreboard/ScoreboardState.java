package net.minecraft.scoreboard;

import java.util.Collection;
import java.util.Iterator;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;

public class ScoreboardState extends PersistentState {
   public static final String field_31893 = "scoreboard";
   private final Scoreboard scoreboard;

   public ScoreboardState(Scoreboard scoreboard) {
      this.scoreboard = scoreboard;
   }

   public ScoreboardState readNbt(NbtCompound nbt) {
      this.readObjectivesNbt(nbt.getList("Objectives", 10));
      this.scoreboard.readNbt(nbt.getList("PlayerScores", 10));
      if (nbt.contains("DisplaySlots", 10)) {
         this.readDisplaySlotsNbt(nbt.getCompound("DisplaySlots"));
      }

      if (nbt.contains("Teams", 9)) {
         this.readTeamsNbt(nbt.getList("Teams", 10));
      }

      return this;
   }

   private void readTeamsNbt(NbtList nbt) {
      for(int i = 0; i < nbt.size(); ++i) {
         NbtCompound nbtCompound = nbt.getCompound(i);
         String string = nbtCompound.getString("Name");
         if (string.length() > 16) {
            string = string.substring(0, 16);
         }

         Team team = this.scoreboard.addTeam(string);
         Text text = Text.Serializer.fromJson(nbtCompound.getString("DisplayName"));
         if (text != null) {
            team.setDisplayName(text);
         }

         if (nbtCompound.contains("TeamColor", 8)) {
            team.setColor(Formatting.byName(nbtCompound.getString("TeamColor")));
         }

         if (nbtCompound.contains("AllowFriendlyFire", 99)) {
            team.setFriendlyFireAllowed(nbtCompound.getBoolean("AllowFriendlyFire"));
         }

         if (nbtCompound.contains("SeeFriendlyInvisibles", 99)) {
            team.setShowFriendlyInvisibles(nbtCompound.getBoolean("SeeFriendlyInvisibles"));
         }

         MutableText text3;
         if (nbtCompound.contains("MemberNamePrefix", 8)) {
            text3 = Text.Serializer.fromJson(nbtCompound.getString("MemberNamePrefix"));
            if (text3 != null) {
               team.setPrefix(text3);
            }
         }

         if (nbtCompound.contains("MemberNameSuffix", 8)) {
            text3 = Text.Serializer.fromJson(nbtCompound.getString("MemberNameSuffix"));
            if (text3 != null) {
               team.setSuffix(text3);
            }
         }

         AbstractTeam.VisibilityRule visibilityRule2;
         if (nbtCompound.contains("NameTagVisibility", 8)) {
            visibilityRule2 = AbstractTeam.VisibilityRule.getRule(nbtCompound.getString("NameTagVisibility"));
            if (visibilityRule2 != null) {
               team.setNameTagVisibilityRule(visibilityRule2);
            }
         }

         if (nbtCompound.contains("DeathMessageVisibility", 8)) {
            visibilityRule2 = AbstractTeam.VisibilityRule.getRule(nbtCompound.getString("DeathMessageVisibility"));
            if (visibilityRule2 != null) {
               team.setDeathMessageVisibilityRule(visibilityRule2);
            }
         }

         if (nbtCompound.contains("CollisionRule", 8)) {
            AbstractTeam.CollisionRule collisionRule = AbstractTeam.CollisionRule.getRule(nbtCompound.getString("CollisionRule"));
            if (collisionRule != null) {
               team.setCollisionRule(collisionRule);
            }
         }

         this.readTeamPlayersNbt(team, nbtCompound.getList("Players", 8));
      }

   }

   private void readTeamPlayersNbt(Team team, NbtList nbt) {
      for(int i = 0; i < nbt.size(); ++i) {
         this.scoreboard.addPlayerToTeam(nbt.getString(i), team);
      }

   }

   private void readDisplaySlotsNbt(NbtCompound nbt) {
      for(int i = 0; i < 19; ++i) {
         if (nbt.contains("slot_" + i, 8)) {
            String string = nbt.getString("slot_" + i);
            ScoreboardObjective scoreboardObjective = this.scoreboard.getNullableObjective(string);
            this.scoreboard.setObjectiveSlot(i, scoreboardObjective);
         }
      }

   }

   private void readObjectivesNbt(NbtList nbt) {
      for(int i = 0; i < nbt.size(); ++i) {
         NbtCompound nbtCompound = nbt.getCompound(i);
         ScoreboardCriterion.getOrCreateStatCriterion(nbtCompound.getString("CriteriaName")).ifPresent((scoreboardCriterion) -> {
            String string = nbtCompound.getString("Name");
            if (string.length() > 16) {
               string = string.substring(0, 16);
            }

            Text text = Text.Serializer.fromJson(nbtCompound.getString("DisplayName"));
            ScoreboardCriterion.RenderType renderType = ScoreboardCriterion.RenderType.getType(nbtCompound.getString("RenderType"));
            this.scoreboard.addObjective(string, scoreboardCriterion, text, renderType);
         });
      }

   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      nbt.put("Objectives", this.objectivesToNbt());
      nbt.put("PlayerScores", this.scoreboard.toNbt());
      nbt.put("Teams", this.teamsToNbt());
      this.writeDisplaySlotsNbt(nbt);
      return nbt;
   }

   private NbtList teamsToNbt() {
      NbtList nbtList = new NbtList();
      Collection<Team> collection = this.scoreboard.getTeams();
      Iterator var3 = collection.iterator();

      while(var3.hasNext()) {
         Team team = (Team)var3.next();
         NbtCompound nbtCompound = new NbtCompound();
         nbtCompound.putString("Name", team.getName());
         nbtCompound.putString("DisplayName", Text.Serializer.toJson(team.getDisplayName()));
         if (team.getColor().getColorIndex() >= 0) {
            nbtCompound.putString("TeamColor", team.getColor().getName());
         }

         nbtCompound.putBoolean("AllowFriendlyFire", team.isFriendlyFireAllowed());
         nbtCompound.putBoolean("SeeFriendlyInvisibles", team.shouldShowFriendlyInvisibles());
         nbtCompound.putString("MemberNamePrefix", Text.Serializer.toJson(team.getPrefix()));
         nbtCompound.putString("MemberNameSuffix", Text.Serializer.toJson(team.getSuffix()));
         nbtCompound.putString("NameTagVisibility", team.getNameTagVisibilityRule().name);
         nbtCompound.putString("DeathMessageVisibility", team.getDeathMessageVisibilityRule().name);
         nbtCompound.putString("CollisionRule", team.getCollisionRule().name);
         NbtList nbtList2 = new NbtList();
         Iterator var7 = team.getPlayerList().iterator();

         while(var7.hasNext()) {
            String string = (String)var7.next();
            nbtList2.add(NbtString.of(string));
         }

         nbtCompound.put("Players", nbtList2);
         nbtList.add(nbtCompound);
      }

      return nbtList;
   }

   private void writeDisplaySlotsNbt(NbtCompound nbt) {
      NbtCompound nbtCompound = new NbtCompound();
      boolean bl = false;

      for(int i = 0; i < 19; ++i) {
         ScoreboardObjective scoreboardObjective = this.scoreboard.getObjectiveForSlot(i);
         if (scoreboardObjective != null) {
            nbtCompound.putString("slot_" + i, scoreboardObjective.getName());
            bl = true;
         }
      }

      if (bl) {
         nbt.put("DisplaySlots", nbtCompound);
      }

   }

   private NbtList objectivesToNbt() {
      NbtList nbtList = new NbtList();
      Collection<ScoreboardObjective> collection = this.scoreboard.getObjectives();
      Iterator var3 = collection.iterator();

      while(var3.hasNext()) {
         ScoreboardObjective scoreboardObjective = (ScoreboardObjective)var3.next();
         if (scoreboardObjective.getCriterion() != null) {
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putString("Name", scoreboardObjective.getName());
            nbtCompound.putString("CriteriaName", scoreboardObjective.getCriterion().getName());
            nbtCompound.putString("DisplayName", Text.Serializer.toJson(scoreboardObjective.getDisplayName()));
            nbtCompound.putString("RenderType", scoreboardObjective.getRenderType().getName());
            nbtList.add(nbtCompound);
         }
      }

      return nbtList;
   }
}
