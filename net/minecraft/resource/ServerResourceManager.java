package net.minecraft.resource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.condition.LootConditionManager;
import net.minecraft.loot.function.LootFunctionManager;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.tag.TagManager;
import net.minecraft.tag.TagManagerLoader;
import net.minecraft.util.Unit;
import net.minecraft.util.registry.DynamicRegistryManager;

public class ServerResourceManager implements AutoCloseable {
   private static final CompletableFuture<Unit> COMPLETED_UNIT;
   private final ReloadableResourceManager resourceManager;
   private final CommandManager commandManager;
   private final RecipeManager recipeManager;
   private final TagManagerLoader registryTagManager;
   private final LootConditionManager lootConditionManager;
   private final LootManager lootManager;
   private final LootFunctionManager lootFunctionManager;
   private final ServerAdvancementLoader serverAdvancementLoader;
   private final FunctionLoader functionLoader;

   public ServerResourceManager(DynamicRegistryManager registryManager, CommandManager.RegistrationEnvironment commandEnvironment, int functionPermissionLevel) {
      this.resourceManager = new ReloadableResourceManagerImpl(ResourceType.SERVER_DATA);
      this.recipeManager = new RecipeManager();
      this.lootConditionManager = new LootConditionManager();
      this.lootManager = new LootManager(this.lootConditionManager);
      this.lootFunctionManager = new LootFunctionManager(this.lootConditionManager, this.lootManager);
      this.serverAdvancementLoader = new ServerAdvancementLoader(this.lootConditionManager);
      this.registryTagManager = new TagManagerLoader(registryManager);
      this.commandManager = new CommandManager(commandEnvironment);
      this.functionLoader = new FunctionLoader(functionPermissionLevel, this.commandManager.getDispatcher());
      this.resourceManager.registerReloader(this.registryTagManager);
      this.resourceManager.registerReloader(this.lootConditionManager);
      this.resourceManager.registerReloader(this.recipeManager);
      this.resourceManager.registerReloader(this.lootManager);
      this.resourceManager.registerReloader(this.lootFunctionManager);
      this.resourceManager.registerReloader(this.functionLoader);
      this.resourceManager.registerReloader(this.serverAdvancementLoader);
   }

   public FunctionLoader getFunctionLoader() {
      return this.functionLoader;
   }

   public LootConditionManager getLootConditionManager() {
      return this.lootConditionManager;
   }

   public LootManager getLootManager() {
      return this.lootManager;
   }

   public LootFunctionManager getLootFunctionManager() {
      return this.lootFunctionManager;
   }

   public TagManager getRegistryTagManager() {
      return this.registryTagManager.getTagManager();
   }

   public RecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public ServerAdvancementLoader getServerAdvancementLoader() {
      return this.serverAdvancementLoader;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public static CompletableFuture<ServerResourceManager> reload(List<ResourcePack> packs, DynamicRegistryManager registryManager, CommandManager.RegistrationEnvironment commandEnvironment, int functionPermissionLevel, Executor prepareExecutor, Executor applyExecutor) {
      ServerResourceManager serverResourceManager = new ServerResourceManager(registryManager, commandEnvironment, functionPermissionLevel);
      CompletableFuture<Unit> completableFuture = serverResourceManager.resourceManager.reload(prepareExecutor, applyExecutor, packs, COMPLETED_UNIT);
      return completableFuture.whenComplete((unit, throwable) -> {
         if (throwable != null) {
            serverResourceManager.close();
         }

      }).thenApply((unit) -> {
         return serverResourceManager;
      });
   }

   public void loadRegistryTags() {
      this.registryTagManager.getTagManager().apply();
   }

   public void close() {
      this.resourceManager.close();
   }

   static {
      COMPLETED_UNIT = CompletableFuture.completedFuture(Unit.INSTANCE);
   }
}
