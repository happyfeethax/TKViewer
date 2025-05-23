package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.file_handlers.PalFileHandler;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SpriteViewerFrame extends JFrame {

    private SpritePanel spritePanel;
    private JScrollPane scrollPane;

    public SpriteViewerFrame(String title, List<Frame> sprites, PalFileHandler palFileHandler) {
        super(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Attempt to use the application's global icon
        Image clientIcon = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("client_icon.png"));
        if (clientIcon != null) {
            setIconImage(clientIcon);
        }

        spritePanel = new SpritePanel(sprites, palFileHandler);
        scrollPane = new JScrollPane(spritePanel);
        scrollPane.setPreferredSize(new Dimension(800, 600)); // Default size for the scrollable area
        // Adjust scroll speed
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);


        getContentPane().add(scrollPane, BorderLayout.CENTER);

        pack(); // Pack after adding components to respect preferred sizes
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }
}
