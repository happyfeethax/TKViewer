package com.gamemode.tkviewer.utilities;

import com.gamemode.tkviewer.EffectImage;
import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.file_handlers.DatFileHandler;
import com.gamemode.tkviewer.file_handlers.EpfFileHandler;
import com.gamemode.tkviewer.file_handlers.PalFileHandler;
import com.gamemode.tkviewer.render.*;
import com.gamemode.tkviewer.PivotData;
import com.gamemode.tkviewer.resources.Resources;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RenderUtils {
    /**
     * Private constructor to prevent instantiation of static utility class
     */
    private RenderUtils() {}

    public static PivotData getPivotData(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int left = width / 2;
        int top = height / 2;

        return new PivotData(left, top, width, height);
    }

    public static PivotData getPivotData(List<Frame> frames) {
        // Determine Canvas Size
        int left, top, right, bottom;
        left = top = 10000;
        right = bottom = -10000;
        for (Frame frame : frames) {
            if (frame == null) {
                continue;
            }

            if (frame.getLeft() < left) {
                left = frame.getLeft();
            }
            if (frame.getTop() < top) {
                top = frame.getTop();
            }
            if (frame.getRight() > right) {
                right = frame.getRight();
            }
            if (frame.getBottom() > bottom) {
                bottom = frame.getBottom();
            }
        }

        int width = right-left;
        int height = bottom-top;

        return new PivotData(Math.abs(left), Math.abs(top), width, height);
    }

    public static List<EffectImage> aggregateAnimations (List < List <EffectImage>> effImages){
        List<Frame> allFrames = new ArrayList<>();
        for (List<EffectImage> subListImages : effImages) {
            allFrames.addAll(subListImages.stream().map(EffectImage::getFrame).collect(Collectors.toList()));
        }
        PivotData pivotData = RenderUtils.getPivotData(allFrames);
        int maxWidth = pivotData.getCanvasWidth();
        int maxHeight = pivotData.getCanvasHeight();

        // Correct Images according to maxWidth and maxHeight
        for (int i = 0; i < effImages.size(); i++) {
            for (int j = 0; j < effImages.get(i).size(); j++) {
                EffectImage effImage = effImages.get(i).get(j);
                effImage.setImage(resizeImage(effImage.getImage(), maxWidth, maxHeight, pivotData,
                        effImage.getFrame(), effImage.getPivotData()));
            }
        }

        List<EffectImage> mergedImages = effImages.get(0);
        for (int i = 1; i < effImages.size(); i++) {
            mergedImages = mergeEffectImages(mergedImages, effImages.get(i));
        }

        return mergedImages;
    }

    /**
     * Draws images2 on top of images1 - images all must be equal size!
     */
    public static List<EffectImage> mergeEffectImages(List<EffectImage> images1, List<EffectImage> images2) {
        List<EffectImage> returnEffectImages = new ArrayList<EffectImage>();

        int count = Math.max(images1.size(), images2.size());
        int width = Math.max(images1.get(0).getImage().getWidth(), images2.get(0).getImage().getWidth());
        int height = Math.max(images1.get(0).getImage().getHeight(), images2.get(0).getImage().getHeight());
        for (int i = 0; i < count; i++) {
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D graphicsObject = newImage.createGraphics();
            graphicsObject.drawImage(images1.get(i % images1.size()).getImage(),null,0,0);
            graphicsObject.drawImage(images2.get(i % images2.size()).getImage(),null,0,0);

            returnEffectImages.add(new EffectImage(newImage, images1.get(i % images1.size()).getDelay(), null, null));
        }

        return returnEffectImages;
    }

    public static BufferedImage resizeImage(BufferedImage image, int newWidth, int newHeight, PivotData pivotData,
                                            Frame frame, PivotData framePivotData) {
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphicsObject = newImage.createGraphics();
        int frameLeft = (pivotData.getPivotX() - framePivotData.getPivotX());
        int frameTop = (pivotData.getPivotY() - framePivotData.getPivotY());
        graphicsObject.drawImage(image, null, frameLeft, frameTop);

        return newImage;
    }

    public static PartRenderer createRenderer(String fileSubstring, String dataDirectory){
        return new PartRenderer(fileSubstring, dataDirectory);
    }
    public static PartRenderer createBaramBodyRenderer () {
        return new PartRenderer("Body", Resources.BARAM_DATA_DIRECTORY);
    }
    public static PartRenderer createBaramClassicBodyRenderer () {
        return new PartRenderer("C_Body", Resources.BARAM_DATA_DIRECTORY);
    }
    public static PartRenderer createBodyRenderer (DatFileHandler charDat) { return new PartRenderer("Body", charDat, false); }
    public static PartRenderer createBowRenderer (DatFileHandler charDat) {
        return new PartRenderer("Bow", charDat, false);
    }
    public static PartRenderer createCoatRenderer (DatFileHandler charDat) {
        return new PartRenderer("Coat", charDat, false);
    }
    public static EffectRenderer createEffectRenderer () {
        return new EffectRenderer();
    }
    public static PartRenderer createFaceRenderer (DatFileHandler charDat) {
        return new PartRenderer("Face", charDat, false);
    }
    public static PartRenderer createFaceDecRenderer (DatFileHandler charDat) { return new PartRenderer("FaceDec", charDat, false); }
    public static PartRenderer createFanRenderer (DatFileHandler charDat) {
        return new PartRenderer("Fan", charDat, false);
    }
    public static PartRenderer createHairRenderer (DatFileHandler charDat) { return new PartRenderer("Hair", charDat, false);}
    public static PartRenderer createHelmetRenderer (DatFileHandler charDat) {
        return new PartRenderer("Helmet", charDat, false);
    }
    public static TileRenderer createItemRenderer () {
        String ntkDataDirectory = Resources.getNtkDataDirectory();
        DatFileHandler charDat = new DatFileHandler(ntkDataDirectory + File.separator + "char.dat");
        DatFileHandler miscDat = new DatFileHandler(ntkDataDirectory + File.separator + "misc.dat");

        EpfFileHandler itemEpf = new EpfFileHandler(miscDat.getFile("ITEM.EPF"),"ITEM.EPF");
        PalFileHandler itemPal = new PalFileHandler(charDat.getFile("ITEM.PAL"));

        return new TileRenderer(new ArrayList<EpfFileHandler>(Arrays.asList(itemEpf)), itemPal, 0);
    }
    public static TileRenderer createLegendResourceRenderer () {
        String ntkDataDirectory = Resources.getNtkDataDirectory();
        DatFileHandler charDat = new DatFileHandler(ntkDataDirectory + File.separator + "char.dat");
        DatFileHandler miscDat = new DatFileHandler(ntkDataDirectory + File.separator + "misc.dat");

        EpfFileHandler epf = new EpfFileHandler(miscDat.getFile("SYMBOLS.EPF"),"SYMBOLS.EPF");
        PalFileHandler pal = new PalFileHandler(charDat.getFile("ITEM.PAL"));

        return new TileRenderer(new ArrayList<EpfFileHandler>(Arrays.asList(epf)), pal, 0);
    }
    public static TileRenderer createSanResourceRenderer () {
        String ntkDataDirectory = Resources.getNtkDataDirectory();
        DatFileHandler charDat = new DatFileHandler(ntkDataDirectory + File.separator + "char.dat");
        DatFileHandler bintDat = new DatFileHandler(ntkDataDirectory + File.separator + "bint2.dat");

        EpfFileHandler epf = new EpfFileHandler(bintDat.getFile("STAR.EPF"),"STAR.EPF");
        PalFileHandler pal = new PalFileHandler(charDat.getFile("ITEM.PAL"));

        return new TileRenderer(new ArrayList<EpfFileHandler>(Arrays.asList(epf)), pal, 0);
    }
    public static PartRenderer createMantleRenderer (DatFileHandler charDat) {
        return new PartRenderer("Mantle", charDat, false);
    }
    public static MapRenderer createMapRenderer () { return new MapRenderer(); }
    public static ArrayList<TileRenderer> createMiniMapResourceRenderers () {
        ArrayList<TileRenderer> miniMapResourceRenderers = new ArrayList<TileRenderer>();
        String[] mmrExts = {"PLAYER", "SYMBOL", "TITLE"};
        DatFileHandler mnmDat = new DatFileHandler(Resources.getNtkDataDirectory() + File.separator + "mnm.dat");
        for (String mmrExt : mmrExts) {
            EpfFileHandler epf = new EpfFileHandler(mnmDat.getFile("MN" + mmrExt + ".epf"),"MN" + mmrExt + ".epf");
            PalFileHandler pal = new PalFileHandler(mnmDat.getFile("MN" + mmrExt + ".pal"));

            miniMapResourceRenderers.add(new TileRenderer(new ArrayList<EpfFileHandler>(Arrays.asList(epf)), pal, 0));
        }

        return miniMapResourceRenderers;
    }
    public static MobRenderer createMobRenderer () {
        return new MobRenderer();
    }
    public static PartRenderer createSpearRenderer (DatFileHandler charDat) {
        return new PartRenderer("Spear", charDat, false);
    }
    public static PartRenderer createShieldRenderer (DatFileHandler charDat) { return new PartRenderer("Shield", charDat, false); }
    public static PartRenderer createShoeRenderer (DatFileHandler charDat) {
        return new PartRenderer("Shoes", charDat, false);
    }
    public static PartRenderer createSwordRenderer (DatFileHandler charDat) {
        return new PartRenderer("Sword", charDat, false);
    }
    public static ArrayList<TileRenderer> createWorldMapRenderers () {
        ArrayList<TileRenderer> worldMapRenderers = new ArrayList<TileRenderer>();
        String[] wmExts = {"", "2", "3", "4", "kru"};
        DatFileHandler wmDat = new DatFileHandler(Resources.getNtkDataDirectory() + File.separator + "wm.dat");
        for (String wmExt : wmExts) {
            EpfFileHandler epf = new EpfFileHandler(wmDat.getFile("WM" + wmExt + ".epf"),"WM" + wmExt + ".epf");
            PalFileHandler pal = new PalFileHandler(wmDat.getFile("WM" + wmExt + ".pal"));

            worldMapRenderers.add(new TileRenderer(new ArrayList<EpfFileHandler>(Arrays.asList(epf)), pal, 0));
        }

        return worldMapRenderers;
    }
}
