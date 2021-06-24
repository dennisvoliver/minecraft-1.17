package net.minecraft.client.gui.screen.multiplayer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SocialInteractionsPlayerListWidget extends ElementListWidget<SocialInteractionsPlayerListEntry> {
   private final SocialInteractionsScreen parent;
   private final List<SocialInteractionsPlayerListEntry> players = Lists.newArrayList();
   @Nullable
   private String currentSearch;

   public SocialInteractionsPlayerListWidget(SocialInteractionsScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
      super(client, width, height, top, bottom, itemHeight);
      this.parent = parent;
      this.setRenderBackground(false);
      this.setRenderHorizontalShadows(false);
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      double d = this.client.getWindow().getScaleFactor();
      RenderSystem.enableScissor((int)((double)this.getRowLeft() * d), (int)((double)(this.height - this.bottom) * d), (int)((double)(this.getScrollbarPositionX() + 6) * d), (int)((double)(this.height - (this.height - this.bottom) - this.top - 4) * d));
      super.render(matrices, mouseX, mouseY, delta);
      RenderSystem.disableScissor();
   }

   public void update(Collection<UUID> uuids, double scrollAmount) {
      this.players.clear();
      Iterator var4 = uuids.iterator();

      while(var4.hasNext()) {
         UUID uUID = (UUID)var4.next();
         PlayerListEntry playerListEntry = this.client.player.networkHandler.getPlayerListEntry(uUID);
         if (playerListEntry != null) {
            List var10000 = this.players;
            MinecraftClient var10003 = this.client;
            SocialInteractionsScreen var10004 = this.parent;
            UUID var10005 = playerListEntry.getProfile().getId();
            String var10006 = playerListEntry.getProfile().getName();
            Objects.requireNonNull(playerListEntry);
            var10000.add(new SocialInteractionsPlayerListEntry(var10003, var10004, var10005, var10006, playerListEntry::getSkinTexture));
         }
      }

      this.filterPlayers();
      this.players.sort((player1, player2) -> {
         return player1.getName().compareToIgnoreCase(player2.getName());
      });
      this.replaceEntries(this.players);
      this.setScrollAmount(scrollAmount);
   }

   private void filterPlayers() {
      if (this.currentSearch != null) {
         this.players.removeIf((player) -> {
            return !player.getName().toLowerCase(Locale.ROOT).contains(this.currentSearch);
         });
         this.replaceEntries(this.players);
      }

   }

   public void setCurrentSearch(String currentSearch) {
      this.currentSearch = currentSearch;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void setPlayerOnline(PlayerListEntry player, SocialInteractionsScreen.Tab tab) {
      UUID uUID = player.getProfile().getId();
      Iterator var4 = this.players.iterator();

      while(var4.hasNext()) {
         SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry = (SocialInteractionsPlayerListEntry)var4.next();
         if (socialInteractionsPlayerListEntry.getUuid().equals(uUID)) {
            socialInteractionsPlayerListEntry.setOffline(false);
            return;
         }
      }

      if ((tab == SocialInteractionsScreen.Tab.ALL || this.client.getSocialInteractionsManager().isPlayerMuted(uUID)) && (Strings.isNullOrEmpty(this.currentSearch) || player.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.currentSearch))) {
         MinecraftClient var10002 = this.client;
         SocialInteractionsScreen var10003 = this.parent;
         UUID var10004 = player.getProfile().getId();
         String var10005 = player.getProfile().getName();
         Objects.requireNonNull(player);
         SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry2 = new SocialInteractionsPlayerListEntry(var10002, var10003, var10004, var10005, player::getSkinTexture);
         this.addEntry(socialInteractionsPlayerListEntry2);
         this.players.add(socialInteractionsPlayerListEntry2);
      }

   }

   public void setPlayerOffline(UUID uuid) {
      Iterator var2 = this.players.iterator();

      SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry;
      do {
         if (!var2.hasNext()) {
            return;
         }

         socialInteractionsPlayerListEntry = (SocialInteractionsPlayerListEntry)var2.next();
      } while(!socialInteractionsPlayerListEntry.getUuid().equals(uuid));

      socialInteractionsPlayerListEntry.setOffline(true);
   }
}
