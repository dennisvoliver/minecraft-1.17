package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class ObjectiveDisplayNameFix extends DataFix {
   public ObjectiveDisplayNameFix(Schema outputSchema, boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(TypeReferences.OBJECTIVE);
      return this.fixTypeEverywhereTyped("ObjectiveDisplayNameFix", type, (typed) -> {
         return typed.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.update("DisplayName", (dynamic2) -> {
               DataResult var10000 = dynamic2.asString().map((string) -> {
                  return Text.Serializer.toJson(new LiteralText(string));
               });
               Objects.requireNonNull(dynamic);
               return (Dynamic)DataFixUtils.orElse(var10000.map(dynamic::createString).result(), dynamic2);
            });
         });
      });
   }
}
