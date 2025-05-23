package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.file_handlers.*;
import com.gamemode.tkviewer.render.*;
import com.gamemode.tkviewer.render.Renderer; // Already present
import com.gamemode.tkviewer.resources.Resources;
import com.gamemode.tkviewer.utilities.FileUtils;
import com.gamemode.tkviewer.utilities.RenderUtils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List; // Import java.util.List

public class TKViewerGUI extends JFrame implements ActionListener {

    Image clientIcon;

    JMenuBar menuBar;

    JMenu fileMenu = new JMenu("File");
    JMenu openMenu = new JMenu("Open");
    JMenuItem openMapMenuItem = new JMenuItem("Map File (*.cmp | *.map)");
    JMenuItem openDatSpritesMenuItem = new JMenuItem("DAT File (Sprites) (*.dat)");
    JMenuItem exitMenuItem = new JMenuItem("Exit");

    JMenu editMenu = new JMenu("Edit");
    JMenuItem editClearCacheMenuItem = new JMenuItem("Clear Cache");
    JMenuItem editBrowseCacheMenuItem = new JMenuItem("Browse Cache");

    JMenu viewMenu = new JMenu("View");

    JMenu characterMenu = new JMenu("Character");
    JMenuItem viewBodyMenuItem = new JMenuItem("Bodies");
    JMenuItem viewFaceMenuItem = new JMenuItem("Faces");
    JMenuItem viewFaceDecMenuItem = new JMenuItem("Face Decorations");
    JMenuItem viewHairMenuItem = new JMenuItem("Hair");
    JMenuItem viewShoesMenuItem = new JMenuItem("Shoes");

    JMenu armorMenu = new JMenu("Armor");
    JMenuItem viewCoatMenuItem = new JMenuItem("Coats");
    JMenuItem viewHelmetMenuItem = new JMenuItem("Helmets");
    JMenuItem viewMantleMenuItem = new JMenuItem("Mantles");
    JMenuItem viewShieldMenuItem = new JMenuItem("Shields");

    JMenu weaponsMenu = new JMenu("Weapons");
    JMenuItem viewBowMenuItem = new JMenuItem("Bows");
    JMenuItem viewFanMenuItem = new JMenuItem("Fans");
    JMenuItem viewSwordMenuItem = new JMenuItem("Swords");
    JMenuItem viewSpearMenuItem = new JMenuItem("Spears");

    JMenu miscMenu = new JMenu("Misc");
    JMenuItem viewEffectMenuItem = new JMenuItem("Effects");
    JMenuItem viewItemMenuItem = new JMenuItem("Items");
    JMenuItem viewLegendResourceMenuItem = new JMenuItem("Legend Resources");
    JMenuItem viewSanResourceMenuItem = new JMenuItem("San Resources");
    JMenuItem viewMobMenuItem = new JMenuItem("Mobs");
    JMenuItem viewMusicMenuItem = new JMenuItem("Music");
    JMenuItem viewMiniMapResourceMenuItem = new JMenuItem("Mini Map Resources");
    JMenuItem viewWorldMapMenuItem = new JMenuItem("World Maps");

    // Renderers
    MapRenderer mapRenderer;
    PartRenderer bodyRenderer;
    PartRenderer bowRenderer;
    PartRenderer coatRenderer;
    EffectRenderer effectRenderer;
    PartRenderer faceRenderer;
    PartRenderer faceDecRenderer;
    PartRenderer fanRenderer;
    PartRenderer hairRenderer;
    PartRenderer helmetRenderer;
    TileRenderer itemRenderer;
    TileRenderer legendResourceRenderer;
    TileRenderer sanResourceRenderer;
    PartRenderer mantleRenderer;
    ArrayList<TileRenderer> miniMapResourceRenderers;
    MobRenderer mobRenderer;
    PartRenderer spearRenderer;
    PartRenderer shoeRenderer;
    PartRenderer shieldRenderer;
    PartRenderer swordRenderer;
    ArrayList<TileRenderer> worldMapRenderers;

    BufferedImage map;
    Image scaledMap;

    File saveFile;

    Dimension MAX_IMAGE_DIMENSIONS = new Dimension(800, 600);

    public TKViewerGUI(String title) {
        super(title);
        this.setPreferredSize(new Dimension(640, 480));
        this.clientIcon = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("client_icon.png"));
        this.setIconImage(this.clientIcon);

        initMenu();
    }

    public void initMenu() {
        // Add Menu
        menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        // File > Open > Map File
        openMapMenuItem.addActionListener(this);
        openMenu.add(openMapMenuItem);

        // File > Open > DAT File (Sprites)
        openDatSpritesMenuItem.addActionListener(this);
        openMenu.add(openDatSpritesMenuItem);

        // File > Exit
        exitMenuItem.addActionListener(this);

        // File
        fileMenu.add(openMenu);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        // Edit > Clear Cache
        editClearCacheMenuItem.addActionListener(this);
        editClearCacheMenuItem.setToolTipText("Clears TKViewer extracted data and cached animations");

        // Edit > Browse Cache
        editBrowseCacheMenuItem.addActionListener(this);

        // Edit
        editMenu.add(editClearCacheMenuItem);
        editMenu.add(editBrowseCacheMenuItem);
        menuBar.add(editMenu);

        // View > Character

        // View > Character > Bodies
        viewBodyMenuItem.addActionListener(this);
        characterMenu.add(viewBodyMenuItem);

        // View > Character > Faces
        viewFaceMenuItem.addActionListener(this);
        characterMenu.add(viewFaceMenuItem);

        // View > Character > Face Decorations
        viewFaceDecMenuItem.addActionListener(this);
        characterMenu.add(viewFaceDecMenuItem);

        // View >  Character > Hair
        viewHairMenuItem.addActionListener(this);
        characterMenu.add(viewHairMenuItem);

        // View >  Character > Shoes
        viewShoesMenuItem.addActionListener(this);
        characterMenu.add(viewShoesMenuItem);

        viewMenu.add(characterMenu);

        // View > Armor

        // View > Armor > Coats
        viewCoatMenuItem.addActionListener(this);
        armorMenu.add(viewCoatMenuItem);

        // View > Armor > Helmet
        viewHelmetMenuItem.addActionListener(this);
        armorMenu.add(viewHelmetMenuItem);

        // View > Armor > Mantles
        viewMantleMenuItem.addActionListener(this);
        armorMenu.add(viewMantleMenuItem);

        // View > Armor > Shields
        viewShieldMenuItem.addActionListener(this);
        armorMenu.add(viewShieldMenuItem);

        viewMenu.add(armorMenu);

        // View > Weapons

        // View > Weapons > Bows
        viewBowMenuItem.addActionListener(this);
        weaponsMenu.add(viewBowMenuItem);

        // View > Weapons > Fans
        viewFanMenuItem.addActionListener(this);
        weaponsMenu.add(viewFanMenuItem);

        // View > Weapons > Spears
        viewSpearMenuItem.addActionListener(this);
        weaponsMenu.add(viewSpearMenuItem);

        // View > Weapons > Swords
        viewSwordMenuItem.addActionListener(this);
        weaponsMenu.add(viewSwordMenuItem);

        viewMenu.add(weaponsMenu);

        // View > Misc

        // View > Misc >  Effects
        viewEffectMenuItem.addActionListener(this);
        miscMenu.add(viewEffectMenuItem);

        // View > Misc >  Items
        viewItemMenuItem.addActionListener(this);
        miscMenu.add(viewItemMenuItem);

        // View > Misc > Legend Resources
        viewLegendResourceMenuItem.addActionListener(this);
        miscMenu.add(viewLegendResourceMenuItem);

        // View > Misc > San Resources
        viewSanResourceMenuItem.addActionListener(this);
        miscMenu.add(viewSanResourceMenuItem);

        // View > Misc > Mini Map Resources
        viewMiniMapResourceMenuItem.addActionListener(this);
        miscMenu.add(viewMiniMapResourceMenuItem);

        // View > Misc > Mobs
        viewMobMenuItem.addActionListener(this);
        miscMenu.add(viewMobMenuItem);

        // View > Misc >  Music
        viewMusicMenuItem.addActionListener(this);
        miscMenu.add(viewMusicMenuItem);

        // View > Misc >  World Maps
        viewWorldMapMenuItem.addActionListener(this);
        miscMenu.add(viewWorldMapMenuItem);

        viewMenu.add(miscMenu);

        menuBar.add(viewMenu);

        // Drag-and-Drog Area
        String text
                = "<html><div align=center style=\"height: 100%; font-family: Arial; font-size: 18px; font-weight: bold;\">"
                + "Drop map here";

        DefaultTableModel model = new DefaultTableModel(0, 1);
        model.addRow(new String[]{text});

        JTable table = new JTable(model);
        ((JLabel)table.getDefaultRenderer(Object.class)).setHorizontalAlignment(JLabel.CENTER);
        ((JLabel)table.getDefaultRenderer(Object.class)).setVerticalAlignment(JLabel.CENTER);
        table.setDragEnabled(true);
        table.setEnabled(false);
        table.setRowHeight(430);
        table.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>)
                            evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {

                        String fileExtension = FilenameUtils.getExtension(file.getAbsolutePath());

                        // Render Map
                        if (fileExtension.equals("cmp") || fileExtension.equals("map")) {
                            openMap(file);
                            // Exit after first map (don't do bulk open)
                            break;
                        } else {
                            System.out.println("Cannot open file: " + file.getName());
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        this.add(table);
    }

    public Dimension getScaledDimensions(Dimension currentDimensions, Dimension maxDimensions) {
        double widthScale = maxDimensions.getWidth() / currentDimensions.getWidth();
        double heightScale = maxDimensions.getHeight() / currentDimensions.getHeight();

        // If Image is already under maxDimensions
        if (widthScale >= 1 && heightScale >= 1) {
            return currentDimensions;
        }

        double scale = Math.min(widthScale, heightScale);

        return new Dimension((int)(currentDimensions.getWidth() * scale), (int)(currentDimensions.getHeight() * scale));
    }

    public void saveMap(File outputFile) {
        try {
            ImageIO.write(map, "png", outputFile);
        } catch (IOException ioe) {
            System.out.println("Error writing");
        }
    }

    public void showLoadingDialog(String message, Resources.GUI_LOADING_FUNCTION loadingFunction) {
        JDialog loadingNotification = new JDialog(this, "TKViewer", true);
        loadingNotification.setTitle("TKViewer");
        loadingNotification.setIconImage(this.clientIcon);

        loadingNotification.add(new JLabel(message, SwingConstants.CENTER));
        loadingNotification.setSize(new Dimension(300, 75));
        loadingNotification.setResizable(false);
        loadingNotification.setLocationRelativeTo(this);
        SwingWorker loadingWorker = new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                switch(loadingFunction) {
                    case CLEAR_CACHE:
                        clearCache();
                        break;
                    case BODIES:
                        if (bodyRenderer == null) {
                            bodyRenderer = RenderUtils.createBodyRenderer();
                        }
                        break;
                    case BOWS:
                        if (bowRenderer == null) {
                            bowRenderer = RenderUtils.createBowRenderer();
                        }
                        break;
                    case COATS:
                        if (coatRenderer == null) {
                            coatRenderer = RenderUtils.createCoatRenderer();
                        }
                        break;
                    case EFFECTS:
                        if (effectRenderer == null) {
                            effectRenderer = RenderUtils.createEffectRenderer();
                        }
                        break;
                    case FACES:
                        if (faceRenderer == null) {
                            faceRenderer = RenderUtils.createFaceRenderer();
                        }
                        break;
                    case FACE_DEC:
                        if (faceDecRenderer == null) {
                            faceDecRenderer = RenderUtils.createFaceDecRenderer();
                        }
                        break;
                    case FANS:
                        if (fanRenderer == null) {
                            fanRenderer = RenderUtils.createFanRenderer();
                        }
                        break;
                    case HAIR:
                        if (hairRenderer == null) {
                            hairRenderer = RenderUtils.createHairRenderer();
                        }
                        break;
                    case HELMETS:
                        if (helmetRenderer == null) {
                            helmetRenderer = RenderUtils.createHelmetRenderer();
                        }
                        break;
                    case ITEMS:
                        if (itemRenderer == null) {
                            itemRenderer = RenderUtils.createItemRenderer();
                        }
                        break;
                    case LEGEND_RESOURCES:
                        if (legendResourceRenderer == null) {
                            legendResourceRenderer = RenderUtils.createLegendResourceRenderer();
                        }
                        break;
                    case SAN_RESOURCES:
                        if (sanResourceRenderer == null) {
                            sanResourceRenderer = RenderUtils.createSanResourceRenderer();
                        }
                        break;
                    case MANTLES:
                        if (mantleRenderer == null) {
                            mantleRenderer = RenderUtils.createMantleRenderer();
                        }
                        break;
                    case MAPS:
                        if (mapRenderer == null) {
                            mapRenderer = RenderUtils.createMapRenderer();
                        }
                        break;
                    case MOBS:
                        if (mobRenderer == null) {
                            mobRenderer = RenderUtils.createMobRenderer();
                        }
                        break;
                    case MINI_MAP_RESOURCES:
                        if (miniMapResourceRenderers == null) {
                            miniMapResourceRenderers = RenderUtils.createMiniMapResourceRenderers();
                        }
                        break;
                    case SPEARS:
                        if (spearRenderer == null) {
                            spearRenderer = RenderUtils.createSpearRenderer();
                        }
                        break;
                    case SHIELDS:
                        if (shieldRenderer == null) {
                            shieldRenderer = RenderUtils.createShieldRenderer();
                        }
                        break;
                    case SHOES:
                        if (shoeRenderer == null) {
                            shoeRenderer = RenderUtils.createShoeRenderer();
                        }
                        break;
                    case SWORDS:
                        if (swordRenderer == null) {
                            swordRenderer = RenderUtils.createSwordRenderer();
                        }
                        break;
                    case WORLD_MAPS:
                        if (worldMapRenderers == null) {
                            worldMapRenderers = RenderUtils.createWorldMapRenderers();
                        }
                        break;
                }

                return true;
            }

            @Override
            protected void done() {
                loadingNotification.setVisible(false);
                loadingNotification.dispose();
            }
        };
        loadingWorker.execute();
        loadingNotification.setVisible(true);
    }

    public void closeWindow(JFrame frame) {
        frame.dispose();
    }

    public void openMap(File mapFile) {
        // Initialize Map Data if needed
        if (this.mapRenderer == null) {
            showLoadingDialog("Loading map resources, please wait...", Resources.GUI_LOADING_FUNCTION.MAPS);
        }

        String fileExtension = FilenameUtils.getExtension(mapFile.getAbsolutePath());

        // Render Map
        if (fileExtension.equals("cmp") || fileExtension.equals("map")) {
            JDialog renderloadingNotification = new JDialog(this, "TKViewer", true);
            renderloadingNotification.setTitle("TKViewer");
            renderloadingNotification.setIconImage(this.clientIcon);

            renderloadingNotification.add(new JLabel("Rendering " + mapFile.getName() + ", please wait...", SwingConstants.CENTER));
            renderloadingNotification.setSize(new Dimension(300, 75));
            renderloadingNotification.setResizable(false);
            renderloadingNotification.setLocationRelativeTo(this);
            SwingWorker renderLoadingWorker = new SwingWorker<Boolean, Integer>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    map = null;
                    if (fileExtension.equals("cmp")) {
                        map = mapRenderer.renderMap(new CmpFileHandler(mapFile));
                    } else if (fileExtension.equals("map")) {
                        map = mapRenderer.renderMap(new MapFileHandler(mapFile));
                    }

                    return true;
                }

                @Override
                protected void done() {
                    renderloadingNotification.setVisible(false);
                    renderloadingNotification.dispose();
                }
            };
            renderLoadingWorker.execute();
            renderloadingNotification.setVisible(true);

            // Display Map
            String frameTitle = mapFile.getName();
            String mapId = mapFile.getName().replace("TK", "").replace(".cmp", "");
            Path mnmPath = Paths.get(System.getProperty("user.home"),
                    "Documents", "NexusTK", "MiniMapImages", mapId + ".mnm");
            if (mnmPath.toFile().exists()) {
                MnmFileHandler mnmFile = new MnmFileHandler(mnmPath.toFile());
                frameTitle += " :: " + mnmFile.mapName;
                System.out.println(frameTitle);
            }
            JFrame mapFrame = new JFrame(frameTitle);
            mapFrame.setIconImage(clientIcon);
            Dimension currentDimensions = new Dimension(map.getWidth(), map.getHeight());
            Dimension scaledDimensions = getScaledDimensions(currentDimensions, Toolkit.getDefaultToolkit().getScreenSize());
            scaledMap = null;
            if (!currentDimensions.equals(scaledDimensions)) {
                scaledMap = map.getScaledInstance((int) scaledDimensions.getWidth(), (int) scaledDimensions.getHeight(), Image.SCALE_SMOOTH);
            }
            JLabel label = new JLabel(new ImageIcon((scaledMap != null) ? scaledMap : map));
            mapFrame.setSize(this.MAX_IMAGE_DIMENSIONS);
            mapFrame.add(label);
            mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            mapFrame.pack();
            mapFrame.setVisible(true);

            // Add Menu
            JMenuBar imageMenuBar = new JMenuBar();
            mapFrame.setJMenuBar(imageMenuBar);

            // File > Save (Full Quality)
            JMenuItem saveMenuItem = new JMenuItem("Save (Full Quality)");
            saveMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser saveFileChooser = new JFileChooser();
                    saveFileChooser.setDialogTitle("Save Full Quality Map");
                    saveFileChooser.setSelectedFile(new File(new File(System.getProperty("user.home"), "Desktop"),
                            FilenameUtils.getBaseName(mapFile.getName()) + ".png"));
                    int result = saveFileChooser.showDialog(mapFrame, "Save");
                    if (result == JFileChooser.APPROVE_OPTION) {
                        // Get Map File
                        saveFile = saveFileChooser.getSelectedFile();
                        if (FilenameUtils.getExtension(saveFile.getName()).equalsIgnoreCase("xml")) {
                            // filename is OK as-is
                        } else {
                            saveFile = new File(saveFile.getParentFile(), FilenameUtils.getBaseName(saveFile.getName()) + ".png");
                        }

                        JDialog saveMapLoadingNotification = new JDialog(mapFrame, "TKViewer", true);
                        saveMapLoadingNotification.setTitle("TKViewer");
                        saveMapLoadingNotification.setIconImage(clientIcon);

                        saveMapLoadingNotification.add(new JLabel("Saving " + FilenameUtils.getBaseName(saveFile.getName()) + ".png " + ", please wait...", SwingConstants.CENTER));
                        saveMapLoadingNotification.setSize(new Dimension(300, 75));
                        saveMapLoadingNotification.setResizable(false);
                        saveMapLoadingNotification.setLocationRelativeTo(mapFrame);
                        SwingWorker saveLoadingWorker = new SwingWorker<Boolean, Integer>() {
                            @Override
                            protected Boolean doInBackground() throws Exception {
                                saveMap(saveFile);

                                return true;
                            }

                            @Override
                            protected void done() {
                                saveMapLoadingNotification.setVisible(false);
                                saveMapLoadingNotification.dispose();
                            }
                        };
                        saveLoadingWorker.execute();
                        saveMapLoadingNotification.setVisible(true);

                        JOptionPane.showMessageDialog(mapFrame, "Map saved successfully!", "TKViewer", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });

            // File > Close
            JMenuItem closeMenuItem = new JMenuItem("Close");
            closeMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scaledMap = null;
                    closeWindow(mapFrame);
                }
            });

            // File
            JMenu imageFileMenu = new JMenu("File");
            imageFileMenu.add(saveMenuItem);
            imageFileMenu.add(closeMenuItem);
            imageMenuBar.add(imageFileMenu);
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        // Open Map
        if (ae.getSource() == this.openMapMenuItem) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a NexusTK map");

            Path nexusMaps = Paths.get(System.getProperty("user.home"), "Documents", "NexusTK", "Maps");
            fileChooser.setCurrentDirectory(nexusMaps.toFile());
            fileChooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter mapFilter = new FileNameExtensionFilter("Maps (*.cmp;*.map)", "cmp", "map");
            fileChooser.addChoosableFileFilter(mapFilter);

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                this.openMap(fileChooser.getSelectedFile());
            }
        } else if (ae.getSource() == this.openDatSpritesMenuItem) {
            handleOpenDatSprites();
        } else if (ae.getSource() == this.editClearCacheMenuItem) {
            // Ensure Frame[] below refers to java.awt.Frame, not com.gamemode.tkviewer.Frame
            java.awt.Frame[] frames = JFrame.getFrames();

            for (int i = 0; i < frames.length; i++) {
                java.awt.Frame frame = frames[i];
                if (!(frame instanceof TKViewerGUI) && frame.isDisplayable()) {
                    JOptionPane.showMessageDialog(this, "Please close all other TKViewer windows to clear TKViewer cache.", "Cannot clear TKViewer cache", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            showLoadingDialog("Clearing TKViewer cache, please wait...", Resources.GUI_LOADING_FUNCTION.CLEAR_CACHE);
        } else if (ae.getSource() == this.editBrowseCacheMenuItem) {
            if (!new File(Resources.TKVIEWER_DIRECTORY).exists()) {
                new File(Resources.TKVIEWER_DIRECTORY).mkdirs();
            }

            try {
                Desktop.getDesktop().open(new File(Resources.TKVIEWER_DIRECTORY));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if (ae.getSource() == this.viewBodyMenuItem) {
            // Initialize Body Data if needed
            if (this.bodyRenderer == null) {
                showLoadingDialog("Loading body resources, please wait...", Resources.GUI_LOADING_FUNCTION.BODIES);
            }

            new ViewFrame("Bodies", "Body", "Bodies", this.bodyRenderer);
        } else if (ae.getSource() == this.viewBowMenuItem) {
            // Initialize Bow Data if needed
            if (this.bowRenderer == null) {
                showLoadingDialog("Loading bow resources, please wait...", Resources.GUI_LOADING_FUNCTION.BOWS);
            }

            new ViewFrame("Bows", "Bow", "Bows", this.bowRenderer);
        } else if (ae.getSource() == this.viewCoatMenuItem) {
            // Initialize Coat Data if needed
            if (this.coatRenderer == null) {
                showLoadingDialog("Loading coat resources, please wait...", Resources.GUI_LOADING_FUNCTION.COATS);
            }

            new ViewFrame("Coats", "Coat", "Coats", this.coatRenderer);
        } else if (ae.getSource() == this.viewEffectMenuItem) {
            // Initialize Effect Data if needed
            if (this.effectRenderer == null) {
                showLoadingDialog("Loading effect resources, please wait...", Resources.GUI_LOADING_FUNCTION.EFFECTS);
            }

            new ViewFrame("Effects", "Effect", "Effects", this.effectRenderer);
        } else if (ae.getSource() == this.viewFaceMenuItem) {
            // Initialize Face Data if needed
            if (this.faceRenderer == null) {
                showLoadingDialog("Loading face resources, please wait...", Resources.GUI_LOADING_FUNCTION.FACES);
            }

            new ViewFrame("Faces", "Face", "Faces", this.faceRenderer);
        } else if (ae.getSource() == this.viewFaceDecMenuItem) {
            // Initialize Face Dec Data if needed
            if (this.faceDecRenderer == null) {
                showLoadingDialog("Loading face decoration resources, please wait...", Resources.GUI_LOADING_FUNCTION.FACE_DEC);
            }

            new ViewFrame("Face Decorations", "Face Decoration", "Face Decorations", this.faceDecRenderer);
        } else if (ae.getSource() == this.viewFanMenuItem) {
            // Initialize Fan Data if needed
            if (this.fanRenderer == null) {
                showLoadingDialog("Loading fan resources, please wait...", Resources.GUI_LOADING_FUNCTION.FANS);
            }

            new ViewFrame("Fans", "Fan", "Fans", this.fanRenderer);
        } else if (ae.getSource() == this.viewHairMenuItem) {
            // Initialize Hair Data if needed
            if (this.hairRenderer == null) {
                showLoadingDialog("Loading hair resources, please wait...", Resources.GUI_LOADING_FUNCTION.HAIR);
            }

            new ViewFrame("Hair", "Hair", "Hair", this.hairRenderer);
        } else if (ae.getSource() == this.viewHelmetMenuItem) {
            // Initialize Hair Data if needed
            if (this.helmetRenderer == null) {
                showLoadingDialog("Loading helmet resources, please wait...", Resources.GUI_LOADING_FUNCTION.HELMETS);
            }

            new ViewFrame("Helmets", "Helmet", "Helmets", this.helmetRenderer);
        } else if (ae.getSource() == this.viewItemMenuItem) {
            // Initialize Item Data if needed
            if (this.itemRenderer == null) {
                showLoadingDialog("Loading item resources, please wait...", Resources.GUI_LOADING_FUNCTION.ITEMS);
            }

            new ViewFrame("Items", "Item", "Items", this.itemRenderer);
        } else if (ae.getSource() == this.viewLegendResourceMenuItem) {
          // Initialize Legend Resource Data if needed
          if (this.legendResourceRenderer == null) {
              showLoadingDialog("Loading legend resources, please wait...", Resources.GUI_LOADING_FUNCTION.LEGEND_RESOURCES);
          }

          new ViewFrame("Legend Resources", "Legend Resource", "Legend Resources", this.legendResourceRenderer);
        } else if (ae.getSource() == this.viewSanResourceMenuItem) {
          // Initialize San Resource Data if needed
          if (this.sanResourceRenderer == null) {
              showLoadingDialog("Loading san resources, please wait...", Resources.GUI_LOADING_FUNCTION.SAN_RESOURCES);
          }

          new ViewFrame("San Resources", "San Resource", "San Resources", this.sanResourceRenderer);
        } else if (ae.getSource() == this.viewMantleMenuItem) {
            // Initialize Mantle Data if needed
            if (this.mantleRenderer == null) {
                showLoadingDialog("Loading mantle resources, please wait...", Resources.GUI_LOADING_FUNCTION.MANTLES);
            }

            new ViewFrame("Mantles", "Mantle", "Mantles", this.mantleRenderer);
        } else if (ae.getSource() == this.viewMiniMapResourceMenuItem) {
            // Initialize Mini Map Resource Data if needed
            if (this.miniMapResourceRenderers == null) {
                showLoadingDialog("Loading mini map resources, please wait...", Resources.GUI_LOADING_FUNCTION.MINI_MAP_RESOURCES);
            }

            new ViewFrame("Mini Map Resources", "Mini Map Resource", "Mini Map Resources", this.miniMapResourceRenderers);
        } else if (ae.getSource() == this.viewMobMenuItem) {
            // Initialize Mob Data if needed
            if (this.mobRenderer == null) {
                showLoadingDialog("Loading mob resources, please wait...", Resources.GUI_LOADING_FUNCTION.MOBS);
            }

            new ViewFrame("Mobs", "Mob", "Mobs", this.mobRenderer);
        } else if (ae.getSource() == this.viewMusicMenuItem) {
            // Initialize Music Data if needed
//            if (this.mobRenderer == null) {
//                showLoadingDialog("Loading mob resources, please wait...", Resources.GUI_LOADING_FUNCTION.MOBS);
//            }

            //new ViewFrame("Music", "Music", "Music", null);
        } else if (ae.getSource() == this.viewSpearMenuItem) {
            // Initialize Spear Data if needed
            if (this.spearRenderer == null) {
                showLoadingDialog("Loading spear resources, please wait...", Resources.GUI_LOADING_FUNCTION.SPEARS);
            }

            new ViewFrame("Spears", "Spear", "Spears", this.spearRenderer);
        } else if (ae.getSource() == this.viewShieldMenuItem) {
            // Initialize Shield Data if needed
            if (this.shieldRenderer == null) {
                showLoadingDialog("Loading shield resources, please wait...", Resources.GUI_LOADING_FUNCTION.SHIELDS);
            }

            new ViewFrame("Shields", "Shield", "Shields", this.shieldRenderer);
        } else if (ae.getSource() == this.viewShoesMenuItem) {
            // Initialize Shoes Data if needed
            if (this.shoeRenderer == null) {
                showLoadingDialog("Loading shoes resources, please wait...", Resources.GUI_LOADING_FUNCTION.SHOES);
            }

            new ViewFrame("Shoes", "Shoe", "Shoes", this.shoeRenderer);
        } else if (ae.getSource() == this.viewSwordMenuItem) {
            // Initialize Sword Data if needed
            if (this.swordRenderer == null) {
                showLoadingDialog("Loading sword resources, please wait...", Resources.GUI_LOADING_FUNCTION.SWORDS);
            }

            new ViewFrame("Swords", "Sword", "Swords", this.swordRenderer);
        } else if (ae.getSource() == this.viewWorldMapMenuItem) {
            // Initialize World Map Data if needed
            if (this.worldMapRenderers == null) {
                showLoadingDialog("Loading world map resources, please wait...", Resources.GUI_LOADING_FUNCTION.WORLD_MAPS);
            }

            new ViewFrame("World Maps", "World Map", "World Maps", this.worldMapRenderers);
        } else if (ae.getSource() == this.exitMenuItem) {
            System.exit(0);
        }
    }

    private void handleOpenDatSprites() {
        JFileChooser datFileChooser = new JFileChooser();
        datFileChooser.setDialogTitle("Select a DAT file");
        Path nexusDataPath = Paths.get(System.getProperty("user.home"), "Documents", "NexusTK"); // A common base path
        if (nexusDataPath.toFile().exists()) {
            datFileChooser.setCurrentDirectory(nexusDataPath.toFile());
        } else {
            datFileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }
        datFileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter datFilter = new FileNameExtensionFilter("DAT files (*.dat)", "dat");
        datFileChooser.addChoosableFileFilter(datFilter);

        int datResult = datFileChooser.showOpenDialog(this);
        if (datResult == JFileChooser.APPROVE_OPTION) {
            File selectedDatFile = datFileChooser.getSelectedFile();

            try {
                DatFileHandler datFileHandler = new DatFileHandler(selectedDatFile);
                java.util.List<com.gamemode.tkviewer.Frame> sprites = datFileHandler.extractSprites();
                Map<String, PalFileHandler> extractedPalettes = datFileHandler.getExtractedPalettes();

                if (sprites.isEmpty() && extractedPalettes.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No sprites (EPF files) or PAL files found in the selected DAT file.",
                            "No Content Found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                
                // If sprites are empty but palettes are present, could offer a palette viewer later.
                // For now, focus on sprites.
                if (sprites.isEmpty() && !extractedPalettes.isEmpty()) {
                     JOptionPane.showMessageDialog(this,
                            "No sprites (EPF files) found. Palettes were found but sprite viewer needs sprites.",
                            "No Sprites Found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }


                // Create and show the sprite viewer frame
                // SpriteViewerFrame will now handle palette selection from the extractedPalettes map
                String frameTitle = "Sprites from " + selectedDatFile.getName();
                if (extractedPalettes.isEmpty()) {
                    frameTitle += " (no palettes found in DAT)";
                } else {
                    frameTitle += " (using palettes from DAT)";
                }
                new SpriteViewerFrame(frameTitle, sprites, extractedPalettes);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error loading DAT file: " + e.getMessage(),
                        "File Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void clearCache() {
        // Garbage Collect Renderers
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (bodyRenderer != null) {
            bodyRenderer.dispose();
        }
        if (bowRenderer != null) {
            bowRenderer.dispose();
        }
        if (coatRenderer != null) {
            coatRenderer.dispose();
        }
        if (effectRenderer != null) {
            effectRenderer.dispose();
        }
        if (faceRenderer != null) {
            faceRenderer.dispose();
        }
        if (faceDecRenderer != null) {
            faceDecRenderer.dispose();
        }
        if (fanRenderer != null) {
            fanRenderer.dispose();
        }
        if (hairRenderer != null) {
            hairRenderer.dispose();
        }
        if (helmetRenderer != null) {
            helmetRenderer.dispose();
        }
        if (itemRenderer != null) {
            itemRenderer.dispose();
        }
        if (mantleRenderer != null) {
            mantleRenderer.dispose();
        }
        if (mobRenderer != null) {
            mobRenderer.dispose();
        }
        if (spearRenderer != null) {
            spearRenderer.dispose();
        }
        if (shoeRenderer != null) {
            shoeRenderer.dispose();
        }
        if (shieldRenderer != null) {
            shieldRenderer.dispose();
        }
        if (swordRenderer != null) {
            swordRenderer.dispose();
        }
        if (worldMapRenderers != null) {
            for (Renderer renderer : worldMapRenderers) {
                renderer.dispose();
            }
        }

        File cacheDirectory = new File(Resources.TKVIEWER_DIRECTORY);
        if (cacheDirectory.exists()) {
            boolean result = FileUtils.deleteDirectory(Resources.TKVIEWER_DIRECTORY);

            if (!result) {
                JOptionPane.showMessageDialog(this, "Unable to clear TKViewer cache, please close any applications\n holding resources open, restart TKViewer, and try again", "Unable to clear TKViewer cache", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "TKViewer cache cleared successfully!", "Cleared TKViewer Cache Successfully", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "The TKViewer cache is already cleared.", "Empty TKViewer Cache", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
