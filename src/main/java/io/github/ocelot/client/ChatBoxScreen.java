package io.github.ocelot.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.CommandSuggestionHelper;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

/**
 * <p>Custom implementation of {@link ChatScreen}.</p>
 */
public class ChatBoxScreen extends Screen
{
    private String historyBuffer = "";
    private int sentHistoryCursor = -1;
    protected TextFieldWidget inputField;
    private CommandSuggestionHelper commandSuggestionHelper;

    public ChatBoxScreen()
    {
        super(NarratorChatListener.EMPTY);
    }

    @Override
    protected void init()
    {
        this.sentHistoryCursor = this.getMinecraft().ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new TextFieldWidget(this.font, 4, this.height - 12, this.width - 4, 12, new TranslationTextComponent("chat.editBox"))
        {
            protected IFormattableTextComponent getNarrationMessage()
            {
                return super.getNarrationMessage().appendString(ChatBoxScreen.this.commandSuggestionHelper.getSuggestionMessage());
            }
        };
        this.inputField.setMaxStringLength(256);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setText("");
        this.inputField.setResponder(text ->
        {
            this.commandSuggestionHelper.shouldAutoSuggest(!text.isEmpty());
            this.commandSuggestionHelper.init();
        });
        this.children.add(this.inputField);
        this.commandSuggestionHelper = new CommandSuggestionHelper(this.getMinecraft(), this, this.inputField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestionHelper.init();
        this.setFocusedDefault(this.inputField);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height)
    {
        this.minecraft = minecraft;
        this.itemRenderer = minecraft.getItemRenderer();
        this.font = minecraft.fontRenderer;
        this.width = width;
        this.height = height;
        this.buttons.clear();
        this.children.clear();
        this.setListener(null);
        this.init();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height)
    {
        String s = this.inputField.getText();
        this.init(minecraft, width, height);
        this.inputField.setText(s);
        this.commandSuggestionHelper.init();
    }

    @Override
    public void tick()
    {
        this.inputField.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (this.commandSuggestionHelper.onKeyPressed(keyCode, scanCode, modifiers))
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
                this.getMinecraft().ingameGUI.getChatGUI().addScrollPos(this.getMinecraft().ingameGUI.getChatGUI().getLineCount() - 1);
                return true;
            }
            else if (keyCode == 267)
            {
                this.getMinecraft().ingameGUI.getChatGUI().addScrollPos(-this.getMinecraft().ingameGUI.getChatGUI().getLineCount() + 1);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            String s = this.inputField.getText().trim();
            if (!s.isEmpty())
                this.sendMessage(s);
            this.sentHistoryCursor = this.getMinecraft().ingameGUI.getChatGUI().getSentMessages().size();
            this.inputField.setText("");

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (this.commandSuggestionHelper.onScroll(MathHelper.clamp(delta, -1.0D, 1.0D)))
            return true;
        double factor = Screen.hasShiftDown() ? 1 : 7;
        return ChatWindowRenderer.getScrollHandler().mouseScrolled(2.0 * factor, -delta * factor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.commandSuggestionHelper.onClick((int) mouseX, (int) mouseY, button))
            return true;

        if (button == 0)
        {
            NewChatGui newchatgui = this.getMinecraft().ingameGUI.getChatGUI();
            if (newchatgui.func_238491_a_(mouseX, mouseY))
            {
                return true;
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
            this.inputField.setText(text);
        }
        else
        {
            this.inputField.writeText(text);
        }
    }

    private void getSentHistory(int msgPos)
    {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.getMinecraft().ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp(i, 0, j);
        if (i != this.sentHistoryCursor)
        {
            if (i == j)
            {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            }
            else
            {
                if (this.sentHistoryCursor == j)
                {
                    this.historyBuffer = this.inputField.getText();
                }

                this.inputField.setText(this.getMinecraft().ingameGUI.getChatGUI().getSentMessages().get(i));
                this.commandSuggestionHelper.shouldAutoSuggest(false);
                this.sentHistoryCursor = i;
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.setListener(this.inputField);
        this.inputField.setFocused2(true);
        fill(matrixStack, 2, this.height - 14, this.width - 2, this.height - 2, this.getMinecraft().gameSettings.getChatBackgroundColor(Integer.MIN_VALUE));
        this.inputField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.commandSuggestionHelper.drawSuggestionList(matrixStack, mouseX, mouseY);
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
