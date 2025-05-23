package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.Palette; // Added
import com.gamemode.tkviewer.file_handlers.PalFileHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // Added
import java.awt.event.ActionListener; // Added
import java.util.LinkedHashMap; // Added for ordered map
import java.util.List;
import java.util.Map;

public class SpriteViewerFrame extends JFrame {

    private SpritePanel spritePanel;
    private JScrollPane scrollPane;
    private JComboBox<String> paletteComboBox;
    private List<Frame> sprites;
    // Store Palette objects directly, mapped by a unique display name
    private Map<String, Palette> displayablePalettes; 

    public SpriteViewerFrame(String title, List<Frame> sprites, Map<String, PalFileHandler> extractedPalettesMap) {
        super(title);
        this.sprites = sprites;
        this.displayablePalettes = new LinkedHashMap<>();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Attempt to use the application's global icon
        Image clientIcon = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("client_icon.png"));
        if (clientIcon != null) {
            setIconImage(clientIcon);
        }

        // Populate displayablePalettes and determine initial palette
        Palette initialPalette = null;
        String initialPaletteDisplayName = null;

        if (extractedPalettesMap != null && !extractedPalettesMap.isEmpty()) {
            for (Map.Entry<String, PalFileHandler> entry : extractedPalettesMap.entrySet()) {
                String palFileName = entry.getKey();
                PalFileHandler palFileHandler = entry.getValue();
                if (palFileHandler != null && palFileHandler.palettes != null) {
                    for (int i = 0; i < palFileHandler.palettes.size(); i++) {
                        Palette p = palFileHandler.palettes.get(i);
                        String displayName = palFileName;
                        if (palFileHandler.palettes.size() > 1) {
                            displayName += " - Index " + i;
                        }
                        this.displayablePalettes.put(displayName, p);
                        if (initialPalette == null) { // Select the very first palette encountered as initial
                            initialPalette = p;
                            initialPaletteDisplayName = displayName;
                        }
                    }
                }
            }
        }

        if (initialPalette != null) {
            System.out.println("SpriteViewerFrame: Using initial palette: " + initialPaletteDisplayName);
        } else {
            System.out.println("SpriteViewerFrame: No palettes found in DAT or they were empty. SpritePanel will use default grayscale.");
            // SpritePanel's constructor will handle initialPalette being null by creating a default one.
        }

        // Create SpritePanel with the initial palette
        spritePanel = new SpritePanel(this.sprites, initialPalette);
        scrollPane = new JScrollPane(spritePanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // Create and configure JComboBox for palette selection
        paletteComboBox = new JComboBox<>();
        if (!this.displayablePalettes.isEmpty()) {
            for (String displayName : this.displayablePalettes.keySet()) {
                paletteComboBox.addItem(displayName);
            }
            if (initialPaletteDisplayName != null) {
                paletteComboBox.setSelectedItem(initialPaletteDisplayName);
            }
            paletteComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedName = (String) paletteComboBox.getSelectedItem();
                    if (selectedName != null) {
                        Palette selectedPalette = displayablePalettes.get(selectedName);
                        spritePanel.setPalette(selectedPalette);
                        System.out.println("SpriteViewerFrame: Switched to palette: " + selectedName);
                    }
                }
            });
        } else {
            paletteComboBox.addItem("No Palettes Available");
            paletteComboBox.setEnabled(false);
        }

        // Layout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Palette:"));
        topPanel.add(paletteComboBox);

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // getKeyByValue helper is no longer needed here as we use displayablePalettes map directly
}
