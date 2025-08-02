package com.gamemode.tkviewer.gui;

import com.gamemode.tkviewer.EffectImage;
import com.gamemode.tkviewer.Mob;
import com.gamemode.tkviewer.Part;
import com.gamemode.tkviewer.render.*;
import com.gamemode.tkviewer.render.Renderer;
import com.gamemode.tkviewer.resources.Resources;
import com.gamemode.tkviewer.file_handlers.DatFileHandler;
import com.gamemode.tkviewer.file_handlers.EpfFileHandler;
import com.gamemode.tkviewer.utilities.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import javax.swing.*;

public class ViewFrame extends JFrame implements ActionListener {

    List<Renderer> renderers;
    String singular;
    String plural;
    DatFileHandler datFileHandler;
    File datFile;

    Image clientIcon;
    JPanel imagePanel;
    JList list;
    Integer itemCount;
    List<Integer> selectedFrames;

    JButton exportButton;
    JButton replaceButton;
    JButton saveButton;
    JButton pasteButton;
    JRadioButton framesButton;
    JRadioButton animationsButton;

    public ViewFrame(String title, String singular, String plural, DatFileHandler datFileHandler, File datFile) {
        // Configure Frame
        this.setTitle(title);
        this.singular = singular;
        this.plural = plural;
        this.datFileHandler = datFileHandler;
        this.datFile = datFile;
        this.setLayout(new FlowLayout());
        this.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        this.clientIcon = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(Resources.CLIENT_ICON));
        this.setIconImage(this.clientIcon);
        this.setSize(800, 600);
        this.setResizable(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public ViewFrame(String title, String singular, String plural, EffectRenderer effectRenderer, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, datFileHandler, datFile);
        this.renderers = Arrays.asList(effectRenderer);
        this.configure(false);
    }

    public ViewFrame(String title, String singular, String plural, MobRenderer mobRenderer, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, datFileHandler, datFile);
        this.renderers = Arrays.asList(mobRenderer);
        this.configure(false);
    }

    public ViewFrame(String title, String singular, String plural, PartRenderer partRenderer, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, datFileHandler, datFile);
        this.renderers = Arrays.asList(partRenderer);
        this.configure(false);
    }

    public ViewFrame(String title, String singular, String plural, TileRenderer tileRenderer, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, tileRenderer, false, datFileHandler, datFile);
    }

    public ViewFrame(String title, String singular, String plural, ArrayList<TileRenderer> tileRenderers, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, datFileHandler, datFile);
        this.renderers = new ArrayList<Renderer>();
        this.renderers.addAll(tileRenderers);
        this.configure(false);
    }

    public ViewFrame(String title, String singular, String plural, TileRenderer tileRenderer, boolean useEpfCount, DatFileHandler datFileHandler, File datFile) {
        this(title, singular, plural, datFileHandler, datFile);
        this.renderers = Arrays.asList(tileRenderer);
        this.configure(useEpfCount);
    }

    public void configure(boolean useEpfCount) {
        this.setLayout(new BorderLayout());
        imagePanel = new JPanel();
        imagePanel.setBackground(Color.GRAY);
        imagePanel.setPreferredSize(new Dimension(600, 520));

        selectedFrames = new ArrayList<Integer>();

        itemCount = 0;
        for (Renderer renderer : this.renderers) {
            itemCount += renderer.getCount(useEpfCount);
        }
        String[] items = new String[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = this.singular + " " + i;
        }
        list = new JList(items);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    clearSelectedFrames();
                    int idx = list.getSelectedIndex();
                    if (renderers.get(0) instanceof MobRenderer && !framesButton.isSelected()) {
                        renderMobAnimations(idx);
                    } else if (renderers.get(0) instanceof EffectRenderer && !framesButton.isSelected()) {
                        renderEffectAnimations(idx);
                    } else if (renderers.get(0) instanceof PartRenderer && !framesButton.isSelected()) {
                        renderPartAnimations(idx);
                    } else {
                        renderFrames(idx);
                    }
                }
            }
        });

        JScrollPane scroller = new JScrollPane(list);
        scroller.setPreferredSize(new Dimension(150, 520));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBorder(new LineBorder(Color.BLACK));
        statusPanel.setPreferredSize(new Dimension(this.getWidth(), 36));

        exportButton = new JButton("Export Frames");
        exportButton.addActionListener(this);

        framesButton = new JRadioButton("Frames");
        animationsButton = new JRadioButton("Animations");
        animationsButton.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(framesButton);
        group.add(animationsButton);

        framesButton.addActionListener(this);
        animationsButton.addActionListener(this);

        replaceButton = new JButton("Replace Frames");
        replaceButton.addActionListener(this);

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        saveButton.setEnabled(false);

        pasteButton = new JButton("Paste Frame");
        pasteButton.addActionListener(this);
        pasteButton.setEnabled(false);
        new Timer(100, (e) -> {
            pasteButton.setEnabled(CopiedFrame.copiedFrame != null);
        }).start();

        statusPanel.add(exportButton);
        statusPanel.add(replaceButton);
        statusPanel.add(saveButton);
        statusPanel.add(pasteButton);

        statusPanel.add(framesButton);
        statusPanel.add(animationsButton);

        this.add(scroller, BorderLayout.WEST);
        this.add(imagePanel, BorderLayout.CENTER);
        this.add(statusPanel, BorderLayout.SOUTH);

        this.setVisible(true);

        // Add Menu
        JMenuBar imageMenuBar = new JMenuBar();
        this.setJMenuBar(imageMenuBar);

        // File > Close
        JMenuItem closeMenuItem = new JMenuItem("Close");
        closeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // File
        JMenu imageFileMenu = new JMenu("File");
        imageFileMenu.add(closeMenuItem);
        imageMenuBar.add(imageFileMenu);
    }

    private void clearImagePanel() {
        imagePanel.removeAll();
        imagePanel.revalidate();
        imagePanel.repaint();
    }

    private void clearSelectedFrames() {
        selectedFrames.clear();
        for (Component component : imagePanel.getComponents()) {
            if (component instanceof JLabel) {
                ((JLabel) component).setBorder(null);
            }
        }
    }

    public void pasteFrames(int index) {
        if (selectedFrames.size() == 0) {
            JOptionPane.showMessageDialog(this, "No frames selected to paste into.", "TKViewer", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (CopiedFrame.copiedFrame == null) {
            JOptionPane.showMessageDialog(this, "No frame copied.", "TKViewer", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int rendererIndex = determineRendererIndex(index);
            for (int selectedFrame : selectedFrames) {
                int frameIndex = renderers.get(rendererIndex).getFrameIndex(index, selectedFrame);
                renderers.get(rendererIndex).replaceFrame(index, frameIndex, CopiedFrame.copiedFrame);
            }
            renderFrames(index);
            saveButton.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error pasting frames: " + e.getMessage(), "TKViewer", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveFrames() {
        try {
            // Create a backup of the original file
            Path backupPath = new File(this.datFile.getParentFile(), this.datFile.getName() + ".bak").toPath();
            Files.copy(this.datFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

            // Update the dat file handler with the modified epf files
            for (Renderer renderer : this.renderers) {
                if (renderer instanceof PartRenderer) {
                    PartRenderer partRenderer = (PartRenderer) renderer;
                    for (EpfFileHandler epf : partRenderer.partEpfs) {
                        this.datFileHandler.files.put(epf.filePath, epf.toByteBuffer());
                    }
                } else if (renderer instanceof MobRenderer) {
                    MobRenderer mobRenderer = (MobRenderer) renderer;
                    for (EpfFileHandler epf : mobRenderer.mobEpfs) {
                        this.datFileHandler.files.put(epf.filePath, epf.toByteBuffer());
                    }
                }
            }

            // Write the updated dat file
            this.datFileHandler.writeDatFile(this.datFile.toPath());

            // Disable the save button and show a success message
            saveButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Frames saved successfully!", "TKViewer", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving frames: " + e.getMessage(), "TKViewer", JOptionPane.ERROR_MESSAGE);
        }
    }

    public int determineEpfIndex(int index) {
        int currentIndex = index;
        int currentRendererIndex = 0;
        while (true) {
            if (currentIndex >= this.renderers.get(currentRendererIndex).getCount()) {
                currentIndex -= this.renderers.get(currentRendererIndex).getCount();
                currentRendererIndex++;
            } else {
                break;
            }
        }

        return currentIndex;
    }

    public int determineRendererIndex(int index) {
        int currentIndex = index;
        int currentRendererIndex = 0;
        while (true) {
            if (currentIndex >= this.renderers.get(currentRendererIndex).getCount()) {
                currentIndex -= this.renderers.get(currentRendererIndex).getCount();
                currentRendererIndex++;
            } else {
                break;
            }
        }

        return currentRendererIndex;
    }

    public void renderEffectAnimations(int index) {
        this.renderEffectAnimations(determineEpfIndex(index), determineRendererIndex(index));
    }

    public void renderEffectAnimations(int index, int rendererIndex) {
        clearImagePanel();

        // Get Effect Images
        List<EffectImage> images = ((EffectRenderer) renderers.get(rendererIndex)).renderEffect(index);

        // Create GIF in Temp Directory
        if (!new File(Resources.EFFECT_ANIMATION_DIRECTORY).exists()) {
            new File(Resources.EFFECT_ANIMATION_DIRECTORY).mkdirs();
        }
        String gifPath = (Resources.EFFECT_ANIMATION_DIRECTORY + File.separator + "effect-" + index + "-" + rendererIndex + ".gif");
        if (!new File(gifPath).exists()) {
            FileUtils.exportGifFromImages(images, gifPath);
        }

        // Add GIF to imagePanel
        if (new File(gifPath).exists()) {
            Icon gifIcon = new ImageIcon(gifPath);
            JLabel jLabel = new JLabel(gifIcon);
            imagePanel.add(jLabel);
        } else {
            System.err.println("Couldn't find file: " + gifPath);
        }

        revalidate();
    }

    public void renderMobAnimations(int index) {
        this.renderMobAnimations(determineEpfIndex(index), determineRendererIndex(index));
    }

    public void renderMobAnimations(int index, int rendererIndex) {
        clearImagePanel();

        // Create Part Animations in Temp Directory
        File outputDirectory = new File(Resources.MOB_ANIMATION_DIRECTORY + File.separator + index);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        MobRenderer mobRenderer = ((MobRenderer) renderers.get(rendererIndex));

        List<String> gifPaths = new ArrayList<String>();
        Mob mob = mobRenderer.mobDna.mobs.get(index);
        for (int i = 0; i < mob.getChunks().size(); i++) {
            List<EffectImage> chunkImages = mobRenderer.renderAnimation(index, i);
            if (chunkImages.size() != 0) {
                String gifPath = outputDirectory + File.separator + singular + "-" + index + "-" + i + "-" + rendererIndex + ".gif";
                FileUtils.exportGifFromImages(chunkImages, gifPath);
                gifPaths.add(gifPath);
            }
        }

        for (int i = 0; i < gifPaths.size(); i++) {
            // Add GIF to imagePanel
            String gifPath = gifPaths.get(i);
            if (new File(gifPath).exists()) {
                Icon gifIcon = new ImageIcon(gifPath);
                JLabel jLabel = new JLabel(gifIcon);
                jLabel.setToolTipText(String.valueOf(i));
                imagePanel.add(jLabel);
            } else {
                System.err.println("Couldn't find file: " + gifPath);
            }
        }

        revalidate();
    }

    public void renderPartAnimations(int index) {
        this.renderPartAnimations(determineEpfIndex(index), determineRendererIndex(index));
    }

    public void renderPartAnimations(int index, int rendererIndex) {
        clearImagePanel();

        // Create Part Animations in Temp Directory
        File outputDirectory = new File(Resources.PART_ANIMATION_DIRECTORY + File.separator + plural);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        PartRenderer partRenderer = ((PartRenderer) renderers.get(rendererIndex));

        List<String> gifPaths = new ArrayList<String>();
        Part part = partRenderer.partDsc.parts.get(index);
        for (int i = 0; i < part.getChunks().size(); i++) {
            List<EffectImage> chunkImages = partRenderer.renderAnimation(index, i);
            if (chunkImages.size() != 0) {
                String gifPath = outputDirectory + File.separator + singular + "-" + index + "-" + i + "-" + rendererIndex + ".gif";
                FileUtils.exportGifFromImages(chunkImages, gifPath);
                gifPaths.add(gifPath);
            }
        }

        for (int i = 0; i < gifPaths.size(); i++) {
            // Add GIF to imagePanel
            String gifPath = gifPaths.get(i);
            if (new File(gifPath).exists()) {
                Icon gifIcon = new ImageIcon(gifPath);
                JLabel jLabel = new JLabel(gifIcon);
                jLabel.setToolTipText(String.valueOf(i));
                imagePanel.add(jLabel);
            } else {
                System.err.println("Couldn't find file: " + gifPath);
            }
        }

        revalidate();
    }

    public void renderFrames(int index) {
        this.renderFrames(determineEpfIndex(index), determineRendererIndex(index));
    }

    public void renderFrames(int index, int rendererIndex) {
        clearImagePanel();

        Image[] images = renderers.get(rendererIndex).getFrames(index);
        for (int i = 0; i < images.length; i++) {
            final int frameIndex = renderers.get(rendererIndex).getFrameIndex(index, i);
            JLabel jLabel = new JLabel(new ImageIcon(images[i]));
            jLabel.setToolTipText(String.valueOf(i));
            final int finalI = i;
            jLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JDialog loadingNotification = new JDialog(ViewFrame.this, new StringBuilder("TKViewer" + " - " + singular + "[" + index + ":" + frameIndex + "]").toString(), false);
                        loadingNotification.setIconImage(clientIcon);
                        JTextPane info = new JTextPane();
                        info.setContentType("text/html");
                        info.setText(renderers.get(rendererIndex).getInfo(frameIndex));
                        info.setEditable(false);
                        info.setFont(new Font("Consolas", Font.BOLD, 12));
                        loadingNotification.add(info);
                        loadingNotification.setSize(new Dimension(480, 320));
                        loadingNotification.setResizable(false);
                        loadingNotification.setLocationRelativeTo(ViewFrame.this);
                        loadingNotification.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        loadingNotification.setVisible(true);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        if (selectedFrames.contains(finalI)) {
                            selectedFrames.remove(Integer.valueOf(finalI));
                            jLabel.setBorder(null);
                        } else {
                            selectedFrames.add(finalI);
                            jLabel.setBorder(new LineBorder(Color.RED, 2));
                        }
                    }
                }
            });
            imagePanel.add(jLabel);
        }

        revalidate();
    }

    public void exportFrames(int index) {
        this.exportFrames(determineEpfIndex(index), determineRendererIndex(index));
    }

    public void exportFrames(int index, int rendererIndex) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Choose export directory");
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Image[] images = renderers.get(rendererIndex).getFrames(index);
            for (int i = 0; i < images.length; i++) {
                final int frameIndex = renderers.get(rendererIndex).getFrameIndex(index, i);
                FileUtils.writeBufferedImageToFile(((BufferedImage) images[i]), Paths.get(fileChooser.getSelectedFile().toString(), singular + "-" + index + "-" + frameIndex + ".png").toString());
            }

            JOptionPane.showMessageDialog(this, "Frames exported successfully!", "TKViewer", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void replaceFrames(int index) {
        if (selectedFrames.size() == 0) {
            JOptionPane.showMessageDialog(this, "No frames selected to replace.", "TKViewer", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Choose replacement image");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage replacementImage = javax.imageio.ImageIO.read(selectedFile);
                int rendererIndex = determineRendererIndex(index);
                for (int selectedFrame : selectedFrames) {
                    int frameIndex = renderers.get(rendererIndex).getFrameIndex(index, selectedFrame);
                    System.out.println("Replacing frame " + frameIndex + " in item " + index);
                    com.gamemode.tkviewer.Frame newFrame;
                    if (renderers.get(rendererIndex) instanceof PartRenderer) {
                        PartRenderer partRenderer = (PartRenderer) renderers.get(rendererIndex);
                        newFrame = new com.gamemode.tkviewer.Frame(replacementImage, partRenderer.partPal.palettes.get((int)partRenderer.partDsc.parts.get(index).getPaletteId()));
                        System.out.println("New frame dimensions: " + newFrame.getWidth() + "x" + newFrame.getHeight());
                    } else if (renderers.get(rendererIndex) instanceof MobRenderer) {
                        MobRenderer mobRenderer = (MobRenderer) renderers.get(rendererIndex);
                        newFrame = new com.gamemode.tkviewer.Frame(replacementImage, mobRenderer.mobPal.palettes.get((int)mobRenderer.mobDna.mobs.get(index).getPaletteId()));
                        System.out.println("New frame dimensions: " + newFrame.getWidth() + "x" + newFrame.getHeight());
                    } else {
                        // Should not happen
                        return;
                    }
                    renderers.get(rendererIndex).replaceFrame(index, frameIndex, newFrame);
                }
                renderFrames(index);
                list.setSelectedIndex(index);
                imagePanel.revalidate();
                imagePanel.repaint();
                System.out.println("Rendered frames after replacement.");
                saveButton.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error replacing frames: " + e.getMessage(), "TKViewer", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        int listIndex = list.getSelectedIndex();

        if (listIndex == -1) {
            // Don't render anything until a list selection is made
            return;
        }

        if (ae.getSource() == this.framesButton) {
            this.renderFrames(listIndex);
        } else if (ae.getSource() == this.animationsButton) {
            if (renderers.get(0) instanceof EffectRenderer) {
                this.renderEffectAnimations(listIndex);
            } else if (renderers.get(0) instanceof PartRenderer) {
                this.renderPartAnimations(listIndex);
            }
        } else if (ae.getSource() == this.exportButton) {
            this.exportFrames(listIndex);
        } else if (ae.getSource() == this.replaceButton) {
            this.replaceFrames(listIndex);
        } else if (ae.getSource() == this.saveButton) {
            this.saveFrames();
        } else if (ae.getSource() == this.pasteButton) {
            this.pasteFrames(listIndex);
        }
    }
}
