package com.gamemode.tkviewer.resources;

public class DatEntry {
    public String fileName;
    public int dataLocation;
    public byte[] fileBytes;

    public DatEntry(String fileName, int dataLocation) {
        this.fileName = fileName;
        this.dataLocation = dataLocation;
        this.fileBytes = null; // Initially null, to be populated later
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getDataLocation() {
        return dataLocation;
    }

    public void setDataLocation(int dataLocation) {
        this.dataLocation = dataLocation;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    @Override
    public String toString() {
        return "DatEntry{" +
                "fileName='" + fileName + '\'' +
                ", dataLocation=" + dataLocation +
                ", fileBytes.length=" + (fileBytes != null ? fileBytes.length : "null") +
                '}';
    }
}
