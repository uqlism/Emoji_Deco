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

    @Shadow(remap = false) private int f_98069_;              // currentPage
    @Shadow(remap = false) private List<String> f_98070_;     // pages
    @Shadow(remap = false) private TextFieldHelper f_98072_;  // pageEdit
    @Shadow(remap = false) private boolean f_98067_;          // isSigning

    @Unique
    private SuggestionState runicink$ss() {
        return SuggestionState.of(this);
    }

    private String runicink$getPageText() {
        int p = f_98069_;
        return (p >= 0 && p < f_98070_.size()) ? f_98070_.get(p) : "";
    }

    // m_5534_ = charTyped(char, int)
    @Inject(method = "m_5534_", at = @At("RETURN"), remap = false)
    private void runicink$onCharTyped(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!f_98067_ && cir.getReturnValueZ()) {
            runicink$ss().update(runicink$getPageText(), f_98072_.getCursorPos());
        }
    }

    // m_7933_ = keyPressed(int, int, int)
    @Inject(method = "m_7933_", at = @At("HEAD"), cancellable = true, remap = false)
    private void runicink$onKeyPressedHead(int keyCode, int scanCode, int modifiers,
                                            CallbackInfoReturnable<Boolean> cir) {
        SuggestionState ss = runicink$ss();
        if (!ss.hasSuggestions() || f_98067_) return;

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
                // m_98158_ = setCurrentPageText(String) — private, use reflection
                try {
                    java.lang.reflect.Method m = BookEditScreen.class.getDeclaredMethod("m_98158_", String.class);
                    m.setAccessible(true);
                    m.invoke((BookEditScreen) (Object) this, completed);
                } catch (Exception ignored) {}
                if (newCursor >= 0) {
                    f_98072_.setCursorPos(newCursor, false);
                }
                ss.clear();
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "m_7933_", at = @At("RETURN"), remap = false)
    private void runicink$onKeyPressedReturn(int keyCode, int scanCode, int modifiers,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (f_98067_) return;
        SuggestionState ss = runicink$ss();
        if (ss.hasSuggestions() && (keyCode == 265 || keyCode == 264 || keyCode == 258)) return;
        ss.update(runicink$getPageText(), f_98072_.getCursorPos());
    }

    // m_6375_ = mouseClicked — clear suggestions on any click (e.g. clicking the Sign button)
    @Inject(method = "m_6375_", at = @At("RETURN"), remap = false)
    private void runicink$onMouseClicked(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        runicink$ss().clear();
    }
}
