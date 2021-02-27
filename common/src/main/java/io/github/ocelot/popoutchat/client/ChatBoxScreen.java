package io.github.ocelot.popoutchat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.popoutchat.mixin.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * <p>Custom implementation of {@link ChatScreen}.</p>
 */
public class ChatBoxScreen extends Screen
{
    private String historyBuffer = "";
    private int sentHistoryCursor = -1;
    protected EditBox inputField;
    private CommandSuggestions commandSuggestionHelper;

    public ChatBoxScreen()
    {
        super(NarratorChatListener.NO_TITLE);
    }

    @Override
    protected void init()
    {
        this.sentHistoryCursor = Objects.requireNonNull(this.minecraft).gui.getChat().getRecentChat().size();
        this.inputField = new EditBox(this.font, 4, this.height - 12, this.width - 4, 12, new TranslatableComponent("chat.editBox"))
        {
            @Override
            protected MutableComponent createNarrationMessage()
            {
                return super.createNarrationMessage().append(ChatBoxScreen.this.commandSuggestionHelper.getNarrationMessage());
            }
        };
        this.inputField.setMaxLength(256);
        this.inputField.setBordered(false);
        this.inputField.setValue("");
        this.inputField.setResponder(text ->
        {
            this.commandSuggestionHelper.setAllowSuggestions(!text.isEmpty());
            this.commandSuggestionHelper.updateCommandInfo();
        });
        this.children.add(this.inputField);
        this.commandSuggestionHelper = new CommandSuggestions(Objects.requireNonNull(this.minecraft), this, this.inputField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestionHelper.updateCommandInfo();
        this.setInitialFocus(this.inputField);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height)
    {
        String s = this.inputField.getValue();
        this.init(minecraft, width, height);
        this.inputField.setValue(s);
        this.commandSuggestionHelper.updateCommandInfo();
    }

    @Override
    public void tick()
    {
        this.inputField.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        ChatComponent chat = Objects.requireNonNull(this.minecraft).gui.getChat();
        if (this.commandSuggestionHelper.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        }
        else if (super.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        }
        else if (keyCode == 256)
        {
            return true;
        }
        else if (keyCode != 257 && keyCode != 335)
        {
            if (keyCode == 265)
            {
                this.getSentHistory(-1);
                return true;
            }
            else if (keyCode == 264)
            {
                this.getSentHistory(1);
                return true;
            }
            else if (keyCode == 266)
            {
                chat.scrollChat(chat.getLinesPerPage() - 1);
                return true;
            }
            else if (keyCode == 267)
            {
                chat.scrollChat(-chat.getLinesPerPage() + 1);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            String s = this.inputField.getValue().trim();
            if (!s.isEmpty())
                this.sendMessage(s);
            this.sentHistoryCursor = chat.getRecentChat().size();
            this.inputField.setValue("");

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (this.commandSuggestionHelper.mouseScrolled(Mth.clamp(delta, -1.0D, 1.0D)))
            return true;
        double factor = Screen.hasShiftDown() ? 1 : 7;
        return ChatWindowRenderer.getScrollHandler().mouseScrolled(2.0 * factor, -delta * factor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.commandSuggestionHelper.mouseClicked((int) mouseX, (int) mouseY, button))
            return true;

        if (button == 0)
        {
            ChatComponent chat = Objects.requireNonNull(this.minecraft).gui.getChat();
            if (!((ChatComponentAccessor)chat).getChatQueue().isEmpty())
            {
                double d0 = mouseX - 2.0D;
                double d1 = (double) Objects.requireNonNull(this.minecraft).getWindow().getGuiScaledHeight() - mouseY - 40.0D;
                if (d0 <= (double) Mth.floor((double) ChatWindow.getScaledWidth() / Objects.requireNonNull(this.minecraft).options.chatScale) && d1 < 0.0D && d1 > (double) Mth.floor(-9.0D * Objects.requireNonNull(this.minecraft).options.chatScale))
                {
                    chat.addMessage(((ChatComponentAccessor) chat).getChatQueue().remove());
                    ((ChatComponentAccessor) chat).setLastMessage(System.currentTimeMillis());
                    return true;
                }
            }

            Style style = ChatWindowRenderer.getHoveredComponent(mouseX, mouseY, 1.0F);
            if (style != null && this.handleComponentClicked(style))
            {
                return true;
            }
        }

        return this.inputField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void insertText(String text, boolean overwrite)
    {
        if (overwrite)
        {
            this.inputField.setValue(text);
        }
        else
        {
            this.inputField.insertText(text);
        }
    }

    private void getSentHistory(int msgPos)
    {
        int i = this.sentHistoryCursor + msgPos;
        int j = Objects.requireNonNull(this.minecraft).gui.getChat().getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.sentHistoryCursor)
        {
            if (i == j)
            {
                this.sentHistoryCursor = j;
                this.inputField.setValue(this.historyBuffer);
            }
            else
            {
                if (this.sentHistoryCursor == j)
                {
                    this.historyBuffer = this.inputField.getValue();
                }

                this.inputField.setValue(Objects.requireNonNull(this.minecraft).gui.getChat().getRecentChat().get(i));
                this.commandSuggestionHelper.setAllowSuggestions(false);
                this.sentHistoryCursor = i;
            }
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.setFocused(this.inputField);
        this.inputField.setFocus(true);
        fill(matrixStack, 2, this.height - 14, this.width - 2, this.height - 2, Objects.requireNonNull(this.minecraft).options.getBackgroundColor(Integer.MIN_VALUE));
        this.inputField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.commandSuggestionHelper.render(matrixStack, mouseX, mouseY);
        Style style = ChatWindowRenderer.getHoveredComponent(mouseX, mouseY, partialTicks); // TODO custom implementation
        if (style != null && style.getHoverEvent() != null)
        {
            this.renderComponentHoverEffect(matrixStack, style, mouseX, mouseY);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }
}
