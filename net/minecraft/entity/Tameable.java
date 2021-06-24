package net.minecraft.entity;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface Tameable {
   @Nullable
   UUID getOwnerUuid();

   @Nullable
   Entity getOwner();
}
