package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.text.SuggestionState;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {

    // SRG field names from joined.tsrg
    @Shadow(remap = false) private String[] f_244359_;   // messages
    @Shadow(remap = false) private int f_244562_;        // currentRow

    // SRG: m_276998_ = setMessage(String)
    @Shadow(remap = false)
    protected abstract void m_276998_(String message);

    // Width limit bypass — lambda$init$4(String)
    @Inject(method = "m_279811_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$noWidthLimit(String text, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    // SRG: m_5534_ = charTyped(char, int)
    @Inject(method = "m_5534_", at = @At("RETURN"), remap = false)
    private void runicink$onCharTyped(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        SuggestionState.update(f_244359_[f_244562_]);
    }

    // SRG: m_7933_ = keyPressed(int, int, int)
    // HEAD: intercept UP/DOWN/TAB to navigate/apply suggestions
    @Inject(method = "m_7933_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressedHead(int keyCode, int scanCode, int modifiers,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!SuggestionState.hasSuggestions()) return;

        if (keyCode == 265) {           // UP
            SuggestionState.moveUp();
            cir.setReturnValue(true);
        } else if (keyCode == 264) {    // DOWN
            SuggestionState.moveDown();
            cir.setReturnValue(true);
        } else if (keyCode == 258) {    // TAB
            SuggestionState.Entry entry = SuggestionState.getSelected();
            if (entry != null) {
                m_276998_(SuggestionState.applyTo(f_244359_[f_244562_], entry));
                SuggestionState.clear();
            }
            cir.setReturnValue(true);
        }
    }

    // RETURN: refresh suggestions after key presses that change text (backspace, row change, etc.)
    @Inject(method = "m_7933_", at = @At("RETURN"), remap = false)
    private void runicink$onKeyPressedReturn(int keyCode, int scanCode, int modifiers,
                                              CallbackInfoReturnable<Boolean> cir) {
        // Guard: don't reset selectedIndex if HEAD already handled suggestion navigation
        if (SuggestionState.hasSuggestions() && (keyCode == 265 || keyCode == 264 || keyCode == 258)) return;
        SuggestionState.update(f_244359_[f_244562_]);
    }
}
