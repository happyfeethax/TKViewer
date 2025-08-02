package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.file_handlers.DatFileHandler;
import com.gamemode.tkviewer.file_handlers.EpfFileHandler;
import com.gamemode.tkviewer.file_handlers.PalFileHandler;
import com.gamemode.tkviewer.render.MobRenderer;
import com.gamemode.tkviewer.resources.Resources;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class MonDatViewer extends JFrame {

    DatFileHandler monDat;
    JList epfList;

    public MonDatViewer(DatFileHandler monDat) {
        super("mon.dat Viewer");
        this.monDat = monDat;

        java.util.List<String> epfFiles = new java.util.ArrayList<>();
        for (String fileName : monDat.files.keySet()) {
            if (fileName.toLowerCase().endsWith(".epf")) {
                epfFiles.add(fileName);
            }
        }
        epfList = new JList(epfFiles.toArray());

        JScrollPane scrollPane = new JScrollPane(epfList);
        this.add(scrollPane);

        epfList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedEpf = (String) epfList.getSelectedValue();
                EpfFileHandler epfFileHandler = new EpfFileHandler(monDat.getFile(selectedEpf), selectedEpf);
                PalFileHandler palFileHandler = new PalFileHandler(monDat.getFile("monster.pal"));
                MobRenderer mobRenderer = new MobRenderer(java.util.Arrays.asList(epfFileHandler), palFileHandler, 0);
                new ViewFrame(selectedEpf, "Frame", "Frames", mobRenderer, monDat, new File(Resources.getNtkDataDirectory() + File.separator + "mon.dat"));
            }
        });

        this.setPreferredSize(new Dimension(320, 480));
        this.pack();
        this.setVisible(true);
    }
}
