package io.github.mortuusars.envelope.integration.jei.extensions;

import io.github.mortuusars.envelope.world.item.crafting.ComponentTransferringRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComponentTransferringRecipeExtension implements ICraftingCategoryExtension<ComponentTransferringRecipe> {
    @Override
    public void setRecipe(RecipeHolder<ComponentTransferringRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
        ComponentTransferringRecipe recipe = holder.value();

        List<List<ItemStack>> inputs = new ArrayList<>();

        inputs.add(Arrays.stream(recipe.getSourceIngredient().getItems()).toList());
        for (Ingredient ingredient : recipe.getIngredients()) {
            inputs.add(Arrays.stream(ingredient.getItems()).toList());
        }

        craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, List.of(recipe.getResult()));
    }
}
