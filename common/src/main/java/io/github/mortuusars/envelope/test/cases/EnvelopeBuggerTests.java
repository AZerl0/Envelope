package io.github.mortuusars.envelope.test.cases;

import io.github.mortuusars.mortaar.bugger.test.BuggerTests;

public class EnvelopeBuggerTests {
    public static BuggerTests create() {
        return new BuggerTests()
              .addFrom(new StackIngredientTests())
              .addFrom(new CourierDeliveryTests())
              .addFrom(new MailCraftingRecipeTests())
              .addFrom(new MailCraftingTests());
    }
}
