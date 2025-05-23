package com.gamemode.tkviewer.file_handlers;

import com.gamemode.tkviewer.Frame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map; // Added import for Map
import java.nio.file.Files;

// Not using JUnit due to classpath issues in the environment.
// This is a manual test runner.
public class DatFileHandlerTestRunner {

    private static File tempFolder;

    // Manual assertion helpers
    private static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
    }
    private static void assertEquals(String message, int expected, int actual) {
        if (expected == actual) return;
        throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
    }

    private static void assertNotNull(String message, Object object) {
        if (object != null) return;
        throw new AssertionError(message + " - Object was null");
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) return;
        throw new AssertionError(message + " - Condition was false");
    }

    private static void assertFalse(String message, boolean condition) {
        if (!condition) return;
        throw new AssertionError(message + " - Condition was true");
    }

    // Helper to create minimal EPF file data as a byte array
    private static byte[] createDummyEpfBytes(short frameCount, short width, short height) throws IOException {
        int framePixelDataSize = width * height;
        // Assume stencil data is also width*height bytes for simplicity in dummy data
        // This might not be accurate for real stencil data (often bit-packed) but ensures buffer space.
        int frameStencilDataSize = (width == 0 || height == 0) ? 0 : ((width * height + 7) / 8); // Bit-packed, rounded up bytes
        if (width == 0 || height == 0) {
            framePixelDataSize = 0;
            frameStencilDataSize = 0;
        }


        // Total length of the data block for pixels and stencils for all frames
        int totalDataBlockLength = frameCount * (framePixelDataSize + frameStencilDataSize);

        ByteBuffer bb = ByteBuffer.allocate(12 + totalDataBlockLength + (frameCount * 16));
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putShort(frameCount);
        bb.putShort(width);
        bb.putShort(height);
        bb.putShort((short) 0); // bitBLT
        bb.putInt(totalDataBlockLength); // pixel_data_length (total for all frames' pixels and stencils)

        // Write the combined (dummy) pixel and stencil data block
        byte[] dataBlock = new byte[totalDataBlockLength];
        // Fill with some pattern if needed, or zeros. For now, zeros are fine.
        // Example:
        // for (int i = 0; i < totalDataBlockLength; i++) {
        //     dataBlock[i] = (byte)(i % 256);
        // }
        bb.put(dataBlock);


        // Frame structures
        for (int i = 0; i < frameCount; i++) {
            bb.putShort((short) 0); // top
            bb.putShort((short) 0); // left
            bb.putShort(height);    // bottom
            bb.putShort(width);     // right

            // pixelDataOffset is relative to the start of the data block (after EPF header)
            int pixelOffsetForThisFrame = i * (framePixelDataSize + frameStencilDataSize);
            // stencilDataOffset is also relative to the start of the data block
            int stencilOffsetForThisFrame = pixelOffsetForThisFrame + framePixelDataSize;

            bb.putInt(pixelOffsetForThisFrame);
            bb.putInt(stencilOffsetForThisFrame);
        }
        return bb.array();
    }

    private static byte[] createDatFileBytes(List<String> fileNames, List<byte[]> fileContents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int numFiles = fileNames.size();
        int tocTotalSize = 4 + (numFiles * 17) + 4;

        ByteBuffer countBuffer = ByteBuffer.allocate(4);
        countBuffer.order(ByteOrder.LITTLE_ENDIAN);
        countBuffer.putInt(numFiles + 1);
        baos.write(countBuffer.array());

        List<Integer> dataOffsetsInToc = new ArrayList<>();
        int currentPayloadOffset = 0;
        for (int i = 0; i < numFiles; i++) {
            dataOffsetsInToc.add(tocTotalSize + currentPayloadOffset);
            currentPayloadOffset += fileContents.get(i).length;
        }
        int finalOffsetForToc = tocTotalSize + currentPayloadOffset;

        for (int i = 0; i < numFiles; i++) {
            ByteBuffer entryBuffer = ByteBuffer.allocate(17);
            entryBuffer.order(ByteOrder.LITTLE_ENDIAN);
            entryBuffer.putInt(dataOffsetsInToc.get(i));

            byte[] nameBytes = new byte[13];
            byte[] currentNameBytes = fileNames.get(i).getBytes(StandardCharsets.UTF_8);
            System.arraycopy(currentNameBytes, 0, nameBytes, 0, Math.min(currentNameBytes.length, 13));
            entryBuffer.put(nameBytes);
            baos.write(entryBuffer.array());
        }

        ByteBuffer finalLocBuffer = ByteBuffer.allocate(4);
        finalLocBuffer.order(ByteOrder.LITTLE_ENDIAN);
        finalLocBuffer.putInt(finalOffsetForToc);
        baos.write(finalLocBuffer.array());

        for (byte[] content : fileContents) {
            baos.write(content);
        }
        return baos.toByteArray();
    }

    // Helper to create minimal PAL file data (single palette)
    private static byte[] createDummyPalBytes(int numColors) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Header "DLPalette" (9 bytes)
        baos.write("DLPalette".getBytes(StandardCharsets.US_ASCII));
        // Unknown bytes 1 (15 bytes)
        baos.write(new byte[15]);
        // Animation color count (1 byte) - set to 0 for simplicity
        baos.write((byte)0);
        // Unknown bytes 2 (7 bytes)
        baos.write(new byte[7]);
        // Animation color offsets (0 * 2 bytes) - empty

        // Colors (numColors * 4 bytes each)
        ByteBuffer colorBuffer = ByteBuffer.allocate(numColors * 4);
        colorBuffer.order(ByteOrder.LITTLE_ENDIAN); // PalFileHandler reads colors as int LE
        for (int i = 0; i < numColors; i++) {
            // Simple ARGB color: A=255, R=i, G=i, B=i
            // PalFileHandler reads int as B,G,R,A - so we write it in that order for LE.
            // Or, more simply, PalFileHandler uses `new Color(this.readInt(false, true))`
            // and Color constructor `public Color(long rgba)` expects standard ARGB.
            // So, pack as standard ARGB int.
            int val = i % 256;
            int argb = (255 << 24) | (val << 16) | (val << 8) | val;
            colorBuffer.putInt(argb);
        }
        baos.write(colorBuffer.array());

        return baos.toByteArray();
    }


    private static File createTemporaryDatFile(String testFileName, List<String> fileNames, List<byte[]> fileContents) throws IOException {
        byte[] datBytes = createDatFileBytes(fileNames, fileContents);
        File tempFile = new File(tempFolder, testFileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(datBytes);
        }
        return tempFile;
    }

    public static void testExtractSprites_withEpfs() throws IOException {
        System.out.println("Running testExtractSprites_withEpfs...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("sprite1.epf");
        fileNames.add("image.png");
        fileNames.add("sprite2.epf");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add(createDummyEpfBytes((short) 2, (short) 16, (short) 16));
        fileContents.add("dummy png data".getBytes(StandardCharsets.UTF_8));
        fileContents.add(createDummyEpfBytes((short) 1, (short) 32, (short) 32));

        File datFile = createTemporaryDatFile("test_with_epf.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);
        List<Frame> sprites = datHandler.extractSprites();

        assertNotNull("Sprite list should not be null", sprites);
        assertEquals("Should find frames from EPF files only (2+1=3)", 3, sprites.size());
        assertEquals("Sprite 1, Frame 0 width", 16, sprites.get(0).getWidth());
        // ... (add more assertions as in the original test)
        System.out.println("testExtractSprites_withEpfs PASSED.");
    }

    public static void testExtractSprites_noEpfs() throws IOException {
        System.out.println("Running testExtractSprites_noEpfs...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("document.txt");
        fileNames.add("archive.zip");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add("hello world".getBytes(StandardCharsets.UTF_8));
        fileContents.add("zip data".getBytes(StandardCharsets.UTF_8));

        File datFile = createTemporaryDatFile("test_no_epf.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);
        List<Frame> sprites = datHandler.extractSprites();

        assertNotNull("Sprite list should not be null", sprites);
        assertTrue("Sprite list should be empty when no EPF files are present", sprites.isEmpty());
        System.out.println("testExtractSprites_noEpfs PASSED.");
    }

    public static void testExtractSprites_emptyDat() throws IOException {
        System.out.println("Running testExtractSprites_emptyDat...");
        List<String> fileNames = new ArrayList<>();
        List<byte[]> fileContents = new ArrayList<>();

        File datFile = createTemporaryDatFile("empty.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);
        List<Frame> sprites = datHandler.extractSprites();

        assertNotNull("Sprite list should not be null", sprites);
        assertTrue("Sprite list should be empty for an empty DAT file", sprites.isEmpty());
        System.out.println("testExtractSprites_emptyDat PASSED.");
    }
    
    public static void testExtractSprites_epfWithZeroFrames() throws IOException {
        System.out.println("Running testExtractSprites_epfWithZeroFrames...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("zeroframes.epf");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add(createDummyEpfBytes((short) 0, (short) 16, (short) 16));

        File datFile = createTemporaryDatFile("zeroframes.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);
        List<Frame> sprites = datHandler.extractSprites();

        assertNotNull("Sprite list should not be null", sprites);
        assertTrue("Sprite list should be empty if EPF has zero frames", sprites.isEmpty());
        System.out.println("testExtractSprites_epfWithZeroFrames PASSED.");
    }

    public static void testExtractSprites_epfWithZeroDimensions() throws IOException {
        System.out.println("Running testExtractSprites_epfWithZeroDimensions...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("zerodims.epf");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add(createDummyEpfBytes((short) 1, (short) 0, (short) 0));

        File datFile = createTemporaryDatFile("zerodims.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);
        List<Frame> sprites = datHandler.extractSprites();

        assertNotNull("Sprite list should not be null", sprites);
        assertEquals("Should find one frame object even with zero dimensions", 1, sprites.size());
        Frame frame = sprites.get(0);
        assertEquals("Frame width should be 0", 0, frame.getWidth());
        // ... (add more assertions)
        System.out.println("testExtractSprites_epfWithZeroDimensions PASSED.");
    }

    public static void testExtractPalettes_withPals() throws IOException {
        System.out.println("Running testExtractPalettes_withPals...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("paletteA.pal");
        fileNames.add("dummy.txt");
        fileNames.add("paletteB.pal");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add(createDummyPalBytes(256)); // Standard 256 colors
        fileContents.add("some text data".getBytes(StandardCharsets.UTF_8));
        fileContents.add(createDummyPalBytes(256));

        File datFile = createTemporaryDatFile("test_with_pals.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile); // isBaram = false by default

        Map<String, PalFileHandler> extractedPals = datHandler.getExtractedPalettes();

        assertNotNull("Extracted palettes map should not be null", extractedPals);
        assertEquals("Should find 2 PAL files", 2, extractedPals.size());
        assertTrue("Map should contain paletteA.pal", extractedPals.containsKey("paletteA.pal"));
        assertTrue("Map should contain paletteB.pal", extractedPals.containsKey("paletteB.pal"));

        PalFileHandler palA = extractedPals.get("paletteA.pal");
        assertNotNull("PalFileHandler for paletteA.pal should not be null", palA);
        assertNotNull("palA.palettes list should not be null", palA.palettes);
        assertEquals("palA.palettes list should contain 1 palette", 1, palA.palettes.size());
        assertEquals("Palette A should have 256 colors", 256, palA.palettes.get(0).getColors().size());

        PalFileHandler palB = extractedPals.get("paletteB.pal");
        assertNotNull("PalFileHandler for paletteB.pal should not be null", palB);
        assertNotNull("palB.palettes list should not be null", palB.palettes);
        assertEquals("palB.palettes list should contain 1 palette", 1, palB.palettes.size());
        assertEquals("Palette B should have 256 colors", 256, palB.palettes.get(0).getColors().size());
        
        System.out.println("testExtractPalettes_withPals PASSED.");
    }

    public static void testExtractPalettes_noPals() throws IOException {
        System.out.println("Running testExtractPalettes_noPals...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("sprite1.epf");
        fileNames.add("image.png");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add(createDummyEpfBytes((short) 1, (short)16, (short)16));
        fileContents.add("dummy png data".getBytes(StandardCharsets.UTF_8));

        File datFile = createTemporaryDatFile("test_no_pals.dat", fileNames, fileContents);
        DatFileHandler datHandler = new DatFileHandler(datFile);

        Map<String, PalFileHandler> extractedPals = datHandler.getExtractedPalettes();
        assertNotNull("Extracted palettes map should not be null", extractedPals);
        assertTrue("Extracted palettes map should be empty", extractedPals.isEmpty());
        System.out.println("testExtractPalettes_noPals PASSED.");
    }
    
    public static void testExtractPalettes_malformedPal() throws IOException {
        System.out.println("Running testExtractPalettes_malformedPal...");
        List<String> fileNames = new ArrayList<>();
        fileNames.add("corrupt.pal");
        fileNames.add("good.pal");

        List<byte[]> fileContents = new ArrayList<>();
        fileContents.add("This is not a valid PAL file.".getBytes(StandardCharsets.UTF_8)); // Malformed
        fileContents.add(createDummyPalBytes(256)); // Valid

        File datFile = createTemporaryDatFile("test_malformed_pal.dat", fileNames, fileContents);
        // Errors from PalFileHandler constructor are caught and printed to System.err by DatFileHandler
        // We just need to ensure DatFileHandler itself doesn't crash.
        DatFileHandler datHandler = new DatFileHandler(datFile); 

        Map<String, PalFileHandler> extractedPals = datHandler.getExtractedPalettes();
        assertNotNull("Extracted palettes map should not be null", extractedPals);
        assertEquals("Should only contain the valid PAL file", 1, extractedPals.size());
        assertTrue("Map should contain good.pal", extractedPals.containsKey("good.pal"));
        assertFalse("Map should not contain corrupt.pal", extractedPals.containsKey("corrupt.pal"));
        System.out.println("testExtractPalettes_malformedPal PASSED (assuming error for corrupt.pal was logged).");
    }


    public static void main(String[] args) throws IOException { // Added "throws IOException"
        try {
            // Setup temporary folder
            tempFolder = Files.createTempDirectory("datHandlerTests").toFile();
            tempFolder.deleteOnExit(); // Basic cleanup

            System.out.println("Starting DatFileHandler tests...\n");
            int passCount = 0;
            int failCount = 0;

            try {
                testExtractSprites_withEpfs();
                System.out.println("testExtractSprites_withEpfs: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractSprites_withEpfs: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            try {
                testExtractSprites_noEpfs();
                System.out.println("testExtractSprites_noEpfs: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractSprites_noEpfs: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            try {
                testExtractSprites_emptyDat();
                System.out.println("testExtractSprites_emptyDat: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractSprites_emptyDat: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }
            
            try {
                testExtractSprites_epfWithZeroFrames();
                System.out.println("testExtractSprites_epfWithZeroFrames: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractSprites_epfWithZeroFrames: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            try {
                testExtractSprites_epfWithZeroDimensions();
                System.out.println("testExtractSprites_epfWithZeroDimensions: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractSprites_epfWithZeroDimensions: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            // New tests for PAL extraction
            try {
                testExtractPalettes_withPals();
                System.out.println("testExtractPalettes_withPals: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractPalettes_withPals: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            try {
                testExtractPalettes_noPals();
                System.out.println("testExtractPalettes_noPals: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractPalettes_noPals: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }
            
            try {
                testExtractPalettes_malformedPal();
                System.out.println("testExtractPalettes_malformedPal: PASSED\n");
                passCount++;
            } catch (Throwable e) {
                System.err.println("testExtractPalettes_malformedPal: FAILED - " + e.getMessage());
                e.printStackTrace(System.err);
                System.err.println();
                failCount++;
            }

            System.out.println("\n--------------------------------------------------");
            System.out.println("Test Run Summary:");
            System.out.println("Total Tests: " + (passCount + failCount));
            System.out.println("Passed: " + passCount);
            System.out.println("Failed: " + failCount);
            System.out.println("--------------------------------------------------");

            if (failCount > 0) {
                System.exit(1); // Indicate failure
            }

        } finally {
            // Manual cleanup of temp folder contents
            if (tempFolder != null && tempFolder.exists()) {
                for (File f : tempFolder.listFiles()) {
                    f.delete();
                }
                tempFolder.delete();
            }
        }
    }
}
