package com.uqlism.emoji_deco.mixin;

import com.uqlism.emoji_deco.client.SuggestionState;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BookEditScreen.class)
public class MixinBookEditScreen {

    @Shadow(remap = false) private int currentPage;
    @Shadow(remap = false) private List<String> pages;
    @Shadow(remap = false) private TextFieldHelper pageEdit;
    @Shadow(remap = false) private boolean isSigning;

    @Shadow(remap = false)
    private void setCurrentPageText(String text) {}

    @Unique
    private SuggestionState runicink$ss() {
        return SuggestionState.of(this);
    }

    private String runicink$getPageText() {
        return (currentPage >= 0 && currentPage < pages.size()) ? pages.get(currentPage) : "";
    }

    @Inject(method = "charTyped", at = @At("RETURN"), remap = false)
    private void runicink$onCharTyped(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!isSigning && cir.getReturnValueZ()) {
            runicink$ss().update(runicink$getPageText(), pageEdit.getCursorPos());
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressedHead(int keyCode, int scanCode, int modifiers,
                                            CallbackInfoReturnable<Boolean> cir) {
        SuggestionState ss = runicink$ss();
        if (!ss.hasSuggestions() || isSigning) return;

        if (keyCode == 265) {           // UP
            ss.moveUp();
            cir.setReturnValue(true);
        } else if (keyCode == 264) {    // DOWN
            ss.moveDown();
            cir.setReturnValue(true);
        } else if (keyCode == 258) {    // TAB
            SuggestionState.Entry entry = ss.getSelected();
            if (entry != null) {
                String completed = ss.applyTo(runicink$getPageText(), entry);
                int newCursor = ss.pendingCursor;
                setCurrentPageText(completed);
                if (newCursor >= 0) {
                    pageEdit.setCursorPos(newCursor, false);
                }
                ss.clear();
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("RETURN"), remap = false)
    private void runicink$onKeyPressedReturn(int keyCode, int scanCode, int modifiers,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (isSigning) return;
        SuggestionState ss = runicink$ss();
        if (ss.hasSuggestions() && (keyCode == 265 || keyCode == 264 || keyCode == 258)) return;
        ss.update(runicink$getPageText(), pageEdit.getCursorPos());
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"), remap = false)
    private void runicink$onMouseClicked(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        runicink$ss().clear();
    }
}
