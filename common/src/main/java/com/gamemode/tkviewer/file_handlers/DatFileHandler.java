package com.gamemode.tkviewer.file_handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Assuming com.gamemode.tkviewer.Frame will be resolved during compilation
// If not, this will need adjustment.
import com.gamemode.tkviewer.Frame;
import com.gamemode.tkviewer.resources.Sprite; // Keep for now, might use later

public class DatFileHandler extends FileHandler {
    boolean isBaram;
    public String filePath;
    public long fileCount;
    public Map<String, ByteBuffer> files = new LinkedHashMap<>();
    private Map<String, PalFileHandler> extractedPalettes;

    public DatFileHandler(Path filepath) {
        this(filepath.toFile(), false);
    }

    public DatFileHandler(String filepath) {
        this(new File(filepath), false);
    }

    public DatFileHandler(String filepath, boolean isBaram) {
        this(new File(filepath), isBaram);
    }

    public DatFileHandler(File file) {
        this(file, false);
    }

    public DatFileHandler(File file, boolean isBaram) {
        super(file);
        this.isBaram = isBaram;
        this.filePath = this.file.getPath();
        this.extractedPalettes = new LinkedHashMap<>(); // Initialize the new map
        this.fileCount = this.readInt(true, true) - 1;
        for (int i = 0; i < this.fileCount; i++) {
            long dataLocation = this.readInt(true, true);
            int totalRead = isBaram ? 32 : 13;
            int readLength = lengthUntilZero();
            String fileName = this.readString(readLength, true);
            if (readLength < totalRead) {
                this.seek(totalRead - readLength, false);
            }
            long nextFileLocation = this.filePosition;
            // Read the start of the next file's TOC entry to determine current file's size
            // This is how the original DatFileHandler seems to operate.
            long nextDataLocationForSizeCalc = this.readInt(true, true);
            long fileSize = nextDataLocationForSizeCalc - dataLocation;

            this.seek(dataLocation, true);
            ByteBuffer fileData = this.readBytes(fileSize, true);
            files.put(fileName, fileData); // Store raw data for all files

            // Check for PAL files
            if (fileName.toLowerCase().endsWith(".pal")) {
                System.out.println("Found PAL file in DAT: " + fileName);
                ByteBuffer palDataBuffer = fileData.duplicate();
                palDataBuffer.rewind();
                try {
                    PalFileHandler palHandler = new PalFileHandler(palDataBuffer);
                    this.extractedPalettes.put(fileName, palHandler);
                    System.out.println("Successfully parsed and stored PAL file: " + fileName);
                } catch (Exception e) {
                    System.err.println("Error parsing PAL file '" + fileName + "' from DAT: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            this.seek(nextFileLocation, true);
        }

        this.close();
    }

    public void writeDatFile(Path outputPath) {
        FileWriter fileOutputStream;
        fileOutputStream = new FileWriter(outputPath);

        // Write File Count ( + 1 )
        fileOutputStream.writeInt(Math.toIntExact(this.fileCount + 1), false);

        // Write Table of Contents
        long filePointer = 8 + (this.fileCount * 17) + 13; // Header (File Count + TOC)
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            // Data Location
            fileOutputStream.writeInt(Math.toIntExact(filePointer), false);

            // File Name (Pad 13 bytes)
            byte[] fileName = new byte[13];
            byte[] fileNameBytes = entry.getKey().getBytes();
            for (byte b = 0; b < fileName.length; b++) {
                if (b < fileNameBytes.length) {
                    fileName[b] = fileNameBytes[b];
                } else {
                    fileName[b] = 0;
                }
            }
            fileOutputStream.write(fileName);

            // Update File Pointer
            filePointer += entry.getValue().array().length;
        }
        fileOutputStream.writeInt(Math.toIntExact(filePointer), false);
        fileOutputStream.write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        // Dump Binary Data
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            fileOutputStream.write(entry.getValue().array());
        }

        // Close File
        fileOutputStream.close();
    }

    public void printDatFiles() {
        System.out.println(this.file.getName());
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            System.out.println("  " + entry.getKey());
        }
    }

    public void exportFiles(String outputDirectory) {
        outputDirectory = outputDirectory.replaceAll("\\\\", "/");
        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.exists()) {
            boolean result = outputDirectoryFile.mkdirs();
            if (!result) {
                System.out.println("Unable to create output directory");
            }
        }
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            File outputFile = new File(outputDirectoryFile, entry.getKey().trim());
            if (!outputFile.exists()) {
                try {
                    boolean result = outputFile.createNewFile();
                    if (!result) {
                        System.out.println("Unable to create file: " + entry.getKey());
                    }
                } catch (IOException ioe) {
                    System.out.println("Unable to create file: " + ioe);
                }
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                fileOutputStream.write(entry.getValue().array());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException ioe) {
                System.out.println("Unable to open file: " + ioe);
            }

        }
    }

    public ByteBuffer getFile(String key) {
        return this.getFile(key, true);
    }

    public ByteBuffer getFile(String key, boolean caseInsensitive) {
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            if (caseInsensitive && entry.getKey().toLowerCase().equals(key.toLowerCase())) {
                return entry.getValue();
            } else if (!caseInsensitive && entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }

        // Original code had an empty if block here, which is unusual.
        // Preserving it for now, but it might be a point of review.
        if (caseInsensitive) {

        }

        return null;
    }

    /**
     * Extracts sprite data from embedded EPF files within this DAT archive.
     * Each frame within an EPF file is treated as a separate sprite.
     *
     * @return A list of Frame objects, where each Frame represents a sprite.
     *         Returns an empty list if no EPF files are found or if they contain no frames.
     */
    public List<Frame> extractSprites() {
        List<Frame> sprites = new ArrayList<>();
        for (Map.Entry<String, ByteBuffer> entry : this.files.entrySet()) {
            String fileName = entry.getKey();
            if (fileName.toLowerCase().endsWith(".epf")) {
                ByteBuffer epfData = entry.getValue().duplicate(); // Use duplicate to avoid altering original buffer state
                
                // The EpfFileHandler constructor expects the buffer to be at the beginning.
                epfData.rewind();

                EpfFileHandler epfHandler = new EpfFileHandler(epfData, fileName, false); // false for loadAllFrames initially
                epfHandler.loadAllFrames(); // Load all frames to populate frameCount and frame data

                for (int i = 0; i < epfHandler.frameCount; i++) {
                    Frame frame = epfHandler.getFrame(i);
                    if (frame != null) {
                        // Optionally, we could convert/wrap 'frame' into a 'Sprite' object here
                        // if the 'Sprite' class offers a more suitable interface for the caller.
                        // For now, returning the Frame object directly as it contains necessary data.
                        sprites.add(frame);
                    }
                }
            }
        }
        return sprites;
    }

    private int lengthUntilZero() {
        long currentPosition = this.filePosition;
        int length = 0;
        while (true) {
            byte b = (byte) this.readSignedByte();
            if (b != 0) {
                length++;
            } else {
                break;
            }
        }

        this.seek(currentPosition, true);
        return length;
    }

    @Override
    public ByteBuffer toByteBuffer() {
        // Not implemented
        return null;
    }

    /**
     * Returns the map of PAL files that were automatically parsed from this DAT archive.
     * The keys are the filenames as they appeared in the DAT file.
     * The values are the {@link PalFileHandler} instances for each parsed PAL file.
     *
     * @return A map of extracted {@link PalFileHandler}s.
     */
    public Map<String, PalFileHandler> getExtractedPalettes() {
        return this.extractedPalettes;
    }
}
