package io.github.memorytoame.immersiveeating.fabric.additions.sound;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundJsonTest {
   private static final Gson GSON = new Gson();

   @Test
   void generatesSoundEventsForFoodKeyframes() {
      FoodDefinition definition = GSON.fromJson(
         "{\"item\":\"kaleidoscope_cookery:chorus_fried_egg\",\"sounds\":{"
            + "\"chorus_fried_egg_switchin\":\"food:chorus_fried_egg_switchin\","
            + "\"chorus_fried_egg\":\"food:chorus_fried_egg\"}}",
         FoodDefinition.class
      );

      JsonObject sounds = SoundJson.generate(List.of(definition));

      assertTrue(sounds.has("chorus_fried_egg_switchin"));
      assertEquals(
         "food:chorus_fried_egg_switchin",
         sounds.getAsJsonObject("chorus_fried_egg_switchin").getAsJsonArray("sounds").get(0).getAsString()
      );
      assertTrue(sounds.has("chorus_fried_egg"));
      assertEquals(
         "food:chorus_fried_egg",
         sounds.getAsJsonObject("chorus_fried_egg").getAsJsonArray("sounds").get(0).getAsString()
      );
   }
}
