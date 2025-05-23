package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.Palette;
import com.gamemode.tkviewer.file_handlers.PalFileHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class SpritePanel extends JPanel {

    private List<Frame> sprites;
    private Palette palette;
    private static final int PADDING = 10; // Padding around sprites and the panel itself

    public SpritePanel(List<Frame> sprites, PalFileHandler palFileHandler) {
        this.sprites = sprites;
        if (palFileHandler != null && !palFileHandler.palettes.isEmpty()) {
            // Use the first palette by default
            this.palette = palFileHandler.palettes.get(0);
        } else {
            // Fallback to a default grayscale palette if no palette is provided
            this.palette = createDefaultPalette();
            JOptionPane.showMessageDialog(this,
                    "Palette file not loaded or is empty. Using a default grayscale palette.",
                    "Palette Warning", JOptionPane.WARNING_MESSAGE);
        }

        setBackground(Color.DARK_GRAY);
        // Calculate preferred size based on sprite content
        // This is a simplified calculation; a more robust solution might involve a scroll pane
        // or a more complex layout manager if many sprites are present.
        if (!sprites.isEmpty()) {
            int totalWidth = 0;
            int maxHeightInRow = 0;
            int currentX = PADDING;
            int currentY = PADDING;
            int panelWidth = 500; // Initial assumption for panel width for wrapping

            // Determine layout (simple wrapping)
            for (Frame sprite : sprites) {
                if (currentX + sprite.getWidth() + PADDING > panelWidth && currentX > PADDING) { // Wrap
                    currentY += maxHeightInRow + PADDING;
                    totalWidth = Math.max(totalWidth, currentX - PADDING); // currentX is end of last sprite + PADDING
                    currentX = PADDING;
                    maxHeightInRow = 0;
                }
                currentX += sprite.getWidth() + PADDING;
                maxHeightInRow = Math.max(maxHeightInRow, sprite.getHeight());
            }
            totalWidth = Math.max(totalWidth, currentX - PADDING); // For the last row
            currentY += maxHeightInRow + PADDING;


            setPreferredSize(new Dimension(totalWidth, currentY));
        } else {
            setPreferredSize(new Dimension(400, 300));
        }
    }

    private Palette createDefaultPalette() {
        java.util.List<com.gamemode.tkviewer.Color> colors = new java.util.ArrayList<>();
        for (int i = 0; i < 256; i++) {
            // Standard ARGB packing: Alpha in the most significant byte.
            // (Alpha << 24) | (Red << 16) | (Green << 8) | Blue
            // For default grayscale, R=G=B=i, Alpha=255 (opaque)
            int packedArgb = (255 << 24) | (i << 16) | (i << 8) | i;
            colors.add(new com.gamemode.tkviewer.Color(packedArgb)); // Pass the packed int (implicitly cast to long)
        }
        // PaletteMetadata can be null or basic for this default palette
        return new Palette(0, java.util.Collections.emptyList(), colors, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (sprites == null || sprites.isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString("No sprites to display.", PADDING, PADDING + 15);
            return;
        }

        if (palette == null) {
            g.setColor(Color.RED);
            g.drawString("Error: Palette not loaded.", PADDING, PADDING + 15);
            return;
        }

        int currentX = PADDING;
        int currentY = PADDING;
        int maxHeightInRow = 0;

        Graphics2D g2d = (Graphics2D) g.create();

        for (Frame sprite : sprites) {
            if (sprite.getWidth() <= 0 || sprite.getHeight() <= 0) {
                System.err.println("Skipping sprite with invalid dimensions: " + sprite.getWidth() + "x" + sprite.getHeight());
                continue;
            }

            // Check if we need to wrap to the next line
            if (currentX + sprite.getWidth() + PADDING > getWidth() && currentX > PADDING) {
                currentX = PADDING;
                currentY += maxHeightInRow + PADDING;
                maxHeightInRow = 0;
            }

            BufferedImage image = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
            byte[] pixelData = sprite.getRawPixelData().array();

            // Ensure pixelData has the expected length.
            // The rawPixelData buffer from Frame might be larger than width*height if not sliced precisely.
            // We should only read width*height bytes.
            int expectedPixelDataLength = sprite.getWidth() * sprite.getHeight();
            if (pixelData.length < expectedPixelDataLength) {
                System.err.println("Pixel data array is too short for sprite dimensions. Skipping.");
                continue;
            }


            for (int y = 0; y < sprite.getHeight(); y++) {
                for (int x = 0; x < sprite.getWidth(); x++) {
                    int pixelIndex = y * sprite.getWidth() + x;
                    int colorIndex = pixelData[pixelIndex] & 0xFF; // Convert signed byte to unsigned int

                    if (colorIndex < palette.getColors().size()) {
                        com.gamemode.tkviewer.Color tkColor = palette.getColors().get(colorIndex);
                        // Check for the transparent color (often index 0 in TK palettes)
                        // or if alpha is explicitly zero. Stencil data in Frame might be relevant here too.
                        // For now, simple alpha check from palette color.
                        if (tkColor.getAlpha() > 0) { // Only draw non-transparent pixels
                             // The Color class stores R,G,B,A as Doubles 0.0-255.0. Convert to int.
                            int red = tkColor.getRed().intValue();
                            int green = tkColor.getGreen().intValue();
                            int blue = tkColor.getBlue().intValue();
                            int alpha = tkColor.getAlpha().intValue();
                            image.setRGB(x, y, new java.awt.Color(red, green, blue, alpha).getRGB());
                        }
                    } else {
                        // Index out of bounds for palette, use a default error color (e.g., magenta)
                        image.setRGB(x, y, new java.awt.Color(255, 0, 255, 255).getRGB());
                    }
                }
            }

            g2d.drawImage(image, currentX, currentY, null);
            g2d.setColor(Color.CYAN);
            g2d.drawRect(currentX, currentY, sprite.getWidth(), sprite.getHeight()); // Border around sprite
            // You could draw sprite.getName() or index here as well
            // g2d.drawString("Frame: " + sprites.indexOf(sprite), currentX, currentY + sprite.getHeight() + 12);


            currentX += sprite.getWidth() + PADDING;
            maxHeightInRow = Math.max(maxHeightInRow, sprite.getHeight());
        }
        g2d.dispose();
    }
}
