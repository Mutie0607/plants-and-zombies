package joshxviii.plantz.gui

import joshxviii.plantz.pazResource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.BookViewScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundEditBookPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.WritableBookContent
import java.util.Optional

//TODO create a custom component for letter backgrounds and attached item
// plus codec for saving it

class LetterScreen(
    private val itemStack: ItemStack,
    private val hand: InteractionHand
) : Screen(Component.translatable("gui.plantz.letter")) {
    companion object {
        private const val TEXT_WIDTH = 185
        private const val TEXT_HEIGHT = 133
        private const val IMAGE_WIDTH = 185
        private const val IMAGE_HEIGHT = 133
        private const val BACKGROUND_TEXTURE_SIZE = 256
        private const val MAX_PAGES = 1

        private const val TEXT_COLOR = 0xFF000000.toInt()

        private val BACKGROUND_1 = pazResource("textures/gui/letter/background1.png")
    }

    private val editable = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT) == null &&
        itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT) != null
    private val bookAccess = BookViewScreen.BookAccess.fromItem(itemStack) ?: BookViewScreen.EMPTY_ACCESS
    private val pageComponents = (0 until bookAccess.pageCount).map(bookAccess::getPage)
    private val pages = pageComponents.mapTo(mutableListOf()) { it.string }
    private var currentPage = 0
    private lateinit var pageEditor: MultiLineEditBox
    private var pageNumber = CommonComponents.EMPTY

    override fun init() {
        val left = backgroundLeft()
        val top = backgroundTop()

        if (editable) {
            if (pages.isEmpty()) pages.add("")
            pageEditor = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setTextColor(TEXT_COLOR)
                .setCursorColor(TEXT_COLOR)
                .setShowBackground(false)
                .setTextShadow(false)
                .setX((width / 2) - (TEXT_WIDTH / 2))
                .setY(4+backgroundTop())
                .build(font, TEXT_WIDTH, TEXT_HEIGHT, CommonComponents.EMPTY)
            pageEditor.setCharacterLimit(1024)
            pageEditor.setLineLimit(126 / 9)
            pageEditor.setValueListener { value -> pages[currentPage] = value }
            addRenderableWidget(pageEditor)
            updatePageContent()
        }


        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) {
                if (editable) saveChanges()
                onClose()
            }.pos((width - 200) / 2, menuControlsTop()).width(200).build()
        )
    }

    override fun setInitialFocus() {
        if (editable) setInitialFocus(pageEditor) else super.setInitialFocus()
    }

    override fun getNarrationMessage(): Component =
        CommonComponents.joinForNarration(super.getNarrationMessage(), pageNumber)

    override fun keyPressed(event: KeyEvent): Boolean {
        return super.keyPressed(event) || when (event.key()) {
            258 -> true
            else -> false
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        val collector = graphics.textRenderer()
        collector.accept(TextAlignment.RIGHT, backgroundLeft(), backgroundTop(), pageNumber)

        if (!editable) {
            val page = (pageComponents.getOrNull(currentPage) ?: CommonComponents.EMPTY).copy()
            page.withColor(TEXT_COLOR)
            page.withoutShadow()
            val lines = font.split(page, TEXT_WIDTH)
            for (index in 0 until minOf(126 / 9, lines.size)) {
                collector.accept(5+backgroundLeft(), 8+backgroundTop() + index * 9, lines[index])
            }
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractBackground(graphics, mouseX, mouseY, a)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND_1,
            backgroundLeft(),
            backgroundTop(),
            0.0F,
            0.0F,
            IMAGE_WIDTH,
            IMAGE_HEIGHT,
            BACKGROUND_TEXTURE_SIZE,
            BACKGROUND_TEXTURE_SIZE
        )
    }

    private fun updatePageContent() {
        if (editable && pages.isNotEmpty()) {
            pageEditor.setValue(pages[currentPage], true)
        }
    }

    private fun saveChanges() {
        while (pages.isNotEmpty() && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
        itemStack.set(
            DataComponents.WRITABLE_BOOK_CONTENT,
            WritableBookContent(pages.map { Filterable.passThrough(it) })
        )

        val player = Minecraft.getInstance().player ?: return
        val slot = if (hand == InteractionHand.MAIN_HAND) player.inventory.selectedSlot else 40
        Minecraft.getInstance().connection?.send(ServerboundEditBookPacket(slot, pages, Optional.empty()))
    }

    private fun backgroundLeft() = (width - IMAGE_WIDTH) / 2
    private fun backgroundTop() = 20
    private fun menuControlsTop() = backgroundTop() + IMAGE_HEIGHT + 2
}
