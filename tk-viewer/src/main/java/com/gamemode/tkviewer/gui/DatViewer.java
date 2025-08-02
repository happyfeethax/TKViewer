package com.gamemode.tkviewer.gui;

import javax.swing.*;
import java.awt.*;

import com.gamemode.tkviewer.file_handlers.DatFileHandler;
import com.gamemode.tkviewer.file_handlers.EpfFileHandler;
import com.gamemode.tkviewer.file_handlers.PalFileHandler;
import com.gamemode.tkviewer.render.TileRenderer;
import com.gamemode.tkviewer.resources.Resources;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class DatViewer extends JFrame {

    DatFileHandler datFileHandler;
    PalFileHandler palFileHandler;
    JList fileList;
    JLabel selectedLabel = null;
    com.gamemode.tkviewer.Frame selectedFrame = null;

    public DatViewer(File datFile) {
        super("DAT Viewer - " + datFile.getName());
        this.datFileHandler = new DatFileHandler(datFile);
        this.palFileHandler = new PalFileHandler(new DatFileHandler(Resources.getNtkDataDirectory() + File.separator + "char.dat").getFile("BODY.PAL"));

        String[] fileNames = new String[datFileHandler.files.size()];
        int i = 0;
        for (String fileName : datFileHandler.files.keySet()) {
            fileNames[i++] = fileName;
        }
        fileList = new JList(fileNames);

        JScrollPane fileListScrollPane = new JScrollPane(fileList);

        JPanel framePanel = new JPanel(new GridLayout(0, 6));
        JScrollPane frameScrollPane = new JScrollPane(framePanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileListScrollPane, frameScrollPane);
        this.add(splitPane, BorderLayout.CENTER);

        JButton copyButton = new JButton("Copy Frame");
        copyButton.addActionListener(e -> {
            if (selectedFrame != null) {
                CopiedFrame.copiedFrame = selectedFrame;
                JOptionPane.showMessageDialog(this, "Frame copied successfully!", "TKViewer", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No frame selected.", "TKViewer", JOptionPane.WARNING_MESSAGE);
            }
        });
        this.add(copyButton, BorderLayout.SOUTH);

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = (String) fileList.getSelectedValue();
                if (selectedFile.toLowerCase().endsWith(".epf")) {
                    framePanel.removeAll();
                    selectedLabel = null;
                    selectedFrame = null;
                    EpfFileHandler epfFileHandler = new EpfFileHandler(datFileHandler.getFile(selectedFile), selectedFile);
                    TileRenderer tileRenderer = new TileRenderer(java.util.Arrays.asList(epfFileHandler), palFileHandler, 0);
                    for (int j = 0; j < epfFileHandler.frameCount; j++) {
                        JLabel label = new JLabel(new ImageIcon(tileRenderer.renderTile(j)));
                        final int frameIndex = j;
                        label.addMouseListener(new java.awt.event.MouseAdapter() {
                            public void mouseClicked(java.awt.event.MouseEvent evt) {
                                if (selectedLabel != null) {
                                    selectedLabel.setBorder(null);
                                }
                                selectedLabel = label;
                                selectedLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                                selectedFrame = epfFileHandler.getFrame(frameIndex);
                            }
                        });
                        framePanel.add(label);
                    }
                    framePanel.revalidate();
                    framePanel.repaint();
                }
            }
        });

        this.setPreferredSize(new Dimension(800, 600));
        this.pack();
        this.setVisible(true);
    }
}
