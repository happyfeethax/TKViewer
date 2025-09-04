package com.gamemode.tkviewer.file_handlers;

import com.gamemode.tkviewer.resources.Resources;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.InflaterInputStream;

public abstract class FileHandler {

    public long filePosition = 0;
    public File file;
    public ByteBuffer bytes;
    public Boolean decode;
    public RandomAccessFile fileInputStream;

    public boolean eof() {
        if (this.file != null) {
            try {
                return (this.fileInputStream.getFilePointer() >= this.fileInputStream.length());
            } catch (IOException ioe) {
                System.out.println("Error getting file pointer: " + ioe);
                return true;
            }
        } else if (this.bytes != null) {
            return !this.bytes.hasRemaining();
        }

        return true;
    }

    public abstract ByteBuffer toByteBuffer();
    public int getLength() {
        if (this.file != null) {
            return (int)this.file.length();
        } else if (this.bytes != null) {
            return this.bytes.capacity();
        } else {
            return -1;
        }
    }

    public FileHandler(String filePath) {
        this(new File(filePath));
    }

    public FileHandler(File file) {
        this.file = file;
        try {
            this.fileInputStream = new RandomAccessFile(this.file, "r");
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e);
        }
    }

    public FileHandler(ByteBuffer bytes) {
        // Ability to read a file as a ByteBuffer stream
        this.bytes = bytes;
        this.decode = false;
    }

    public FileHandler(ByteBuffer bytes, boolean decode) {
        // Ability to read a file as a ByteBuffer stream
        this.bytes = bytes;
        this.decode = decode;
    }

    public void seek(long position, boolean absolute) {
        if (this.file != null) {
            this.seekFile(position, absolute);
        } else if (this.bytes != null) {
            this.seekBytes(position, absolute);
        }
    }

    public void seekFile(long position, boolean absolute) {
        try {
            if (absolute) {
                this.filePosition = position;
            } else {
                this.filePosition += position;
            }
            this.fileInputStream.seek(this.filePosition);
        } catch (IOException ioe) {
            System.out.println("Unable to seek in file: " + ioe);
        }
    }

    public void seekBytes(long position, boolean absolute) {
        if (absolute) {
            this.filePosition = position;
        } else {
            this.filePosition += position;
        }
        this.bytes.position((int)this.filePosition);
    }

    public ByteBuffer read(boolean littleEndian) {
        ByteBuffer byteBuffer = null;

        if (this.file != null) {
            byteBuffer = this.readFile(littleEndian);
        } else if (this.bytes != null) {
            byteBuffer = this.readBytes(littleEndian);
        }

        return byteBuffer;
    }

    public ByteBuffer readFile(boolean littleEndian) {
        ByteBuffer byteBuffer = null;
        try {
            long fileLength = (int) this.fileInputStream.length() - this.filePosition;
            byte[] content = new byte[(int) fileLength];
            fileInputStream.readFully(content, 0, (int) fileLength);
            this.filePosition += fileLength + this.filePosition;
            byteBuffer = ByteBuffer.wrap(content);
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        return byteBuffer;
    }

    public ByteBuffer readBytes(boolean littleEndian) {
        this.filePosition = bytes.capacity();
        return bytes;
    }

    // Note: Primarily for reading CMP files, probably not needed in ByteBuffer form yet
    public ByteBuffer readCompressed(boolean littleEndian) {
        ByteArrayOutputStream uncompressedOutputStream = new ByteArrayOutputStream();

        try {
            long fileLength = (int) this.fileInputStream.length() - this.filePosition;
            byte[] content = new byte[(int) fileLength];
            fileInputStream.readFully(content, 0, (int) fileLength);
            this.filePosition += fileLength + this.filePosition;

            InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(content));
            try {
                byte[] buffer = new byte[2048];
                int len;
                while((len = inflaterInputStream.read(buffer)) > 0) {
                    uncompressedOutputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(uncompressedOutputStream.toByteArray());

        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        return byteBuffer;
    }

    public Integer readUnsignedByte() {
        int unsignedByte = 0x00;

        if (this.file != null) {
            unsignedByte = this.readUnsignedByteFile();
        } else if (this.bytes != null) {
            unsignedByte = this.readUnsignedByteBytes();
        }

        return unsignedByte;
    }

    public Integer readUnsignedByteFile() {
        int unsignedByte = 0x00;

        try {
            unsignedByte = fileInputStream.readUnsignedByte();
            this.filePosition++;
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        return unsignedByte;
    }

    public Integer readUnsignedByteBytes() {
        int unsignedByte = bytes.get();
        this.filePosition++;
        if (unsignedByte < 0) {
            // ByteBuffer returns signed integers, correct for Byte size (256)
            unsignedByte = Resources.MAX_BYTE_SIZE + unsignedByte;
        }

        return unsignedByte;
    }

    public int readSignedByte() {
        int signedByte = 0x00;

        if (this.file != null) {
            signedByte = this.readSignedByteFile();
        } else if (this.bytes != null) {
            signedByte = this.readSignedByteBytes();
        }

        return signedByte;
    }

    public int readSignedByteFile() {
        byte b = 0x00;

        try {
            b = fileInputStream.readByte();
            this.filePosition++;
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        return b;
    }

    public int readSignedByteBytes() {
        // either this one or unsigned byte is gonna be right
        int signedByte = bytes.get();
        this.filePosition++;

        return signedByte;
    }

    public ByteBuffer readBytes(long length, boolean littleEndian) {
        ByteBuffer byteBuffer = null;

        if (this.file != null) {
            byteBuffer = this.readBytesFile(length, littleEndian);
        } else if (this.bytes != null) {
            byteBuffer = this.readBytesBytes(length, littleEndian);
        }

        return byteBuffer;
    }

    public ByteBuffer readBytesFile(long length, boolean littleEndian) {
        byte[] content = new byte[(int) length];
        try {
            fileInputStream.readFully(content, 0, (int) length);
            this.filePosition += length;
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(content);

        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        return byteBuffer;
    }

    public ByteBuffer readBytesBytes(long length, boolean littleEndian) {
        byte[] rtnBytes = new byte[(int)length];
        bytes.get(rtnBytes);
        this.filePosition += length;

        ByteBuffer byteBuffer = ByteBuffer.wrap(rtnBytes);

        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        return byteBuffer;
    }

    public Long readInt(boolean littleEndian, boolean unsigned) {
        Long returnInt = null;

        if (this.file != null) {
            returnInt = this.readIntFile(littleEndian, unsigned);
        } else if (this.bytes != null) {
            returnInt = this.readIntBytes(littleEndian, unsigned);
        }

        return returnInt;
    }

    public Long readIntFile(boolean littleEndian, boolean unsigned) {
        byte[] content = new byte[4];
        try {
            fileInputStream.readFully(content, 0, 4);
            this.filePosition += 4;
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(content);
        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        if (unsigned) {
            return (((long) byteBuffer.getInt() & 0xFFFFFFFFL));
        } else {
            return (long)byteBuffer.getInt();
        }
    }

    public Long readIntBytes(boolean littleEndian, boolean unsigned) {
        // TODO: check sign
        if (littleEndian && this.bytes.order() != ByteOrder.LITTLE_ENDIAN) {
            this.bytes.order(ByteOrder.LITTLE_ENDIAN);
        } else if (!littleEndian && this.bytes.order() != ByteOrder.BIG_ENDIAN) {
            this.bytes.order(ByteOrder.BIG_ENDIAN);
        }
        Long returnInt = (long)this.bytes.getInt();
        this.filePosition += 4;
        return returnInt;
    }

    public Integer readShort(boolean littleEndian, boolean unsigned) {
        Integer returnShort = null;

        if (this.file != null) {
            returnShort = this.readShortFile(littleEndian, unsigned);
        } else if (this.bytes != null) {
            returnShort = this.readShortBytes(littleEndian, unsigned);
        }

        return returnShort;
    }

    public Integer readShortFile(boolean littleEndian, boolean unsigned) {
        byte[] content = new byte[2];
        try {
            fileInputStream.readFully(content, 0, 2);
            this.filePosition += 2;
        } catch (IOException ioe) {
            System.out.println("Unable to read from file: " + ioe);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(content);
        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        if (unsigned) {
            return (((int) byteBuffer.getShort() & 0xFFFF));
        } else {
            return (int) byteBuffer.getShort();
        }
    }

    public Integer readShortBytes(boolean littleEndian, boolean unsigned) {
        // TODO: check sign
        if (littleEndian && this.bytes.order() != ByteOrder.LITTLE_ENDIAN) {
            this.bytes.order(ByteOrder.LITTLE_ENDIAN);
        } else if (!littleEndian && this.bytes.order() != ByteOrder.BIG_ENDIAN) {
            this.bytes.order(ByteOrder.BIG_ENDIAN);
        }

        Integer returnShort = (int)this.bytes.getShort();

        this.filePosition += 2;

        if (unsigned) {
            return (((int) returnShort & 0xFFFF));
        }

        return returnShort;
    }

    public String readString(int length, boolean littleEndian) {
        return readString(length, littleEndian, false);
    }

    public String readString(int length, boolean littleEndian, boolean utf16) {
        // Double the length if it's UTF-16
        if (utf16) {
            length *= 2;
        }

        // Uses this.readBytes, no need to check for this.file/this.bytes
        ByteBuffer byteBuffer = this.readBytes(length, littleEndian);
        if (littleEndian) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        StringBuilder stringBuilder = new StringBuilder();
        byte[] bytes = byteBuffer.array();
        for (int i = 0; i < bytes.length; i++) {
            if (utf16 && (i % 2) != 0) {
                continue;
            }
            stringBuilder.append((char)bytes[i]);
        }

        return stringBuilder.toString();
    }

    public void close() {
        if (this.file != null) {
            try {
                this.fileInputStream.close();
            } catch (IOException ioe) {
                System.out.println("Unable to close file: " + ioe);
            }
        } else if (this.bytes != null) {
            // Reset ByteBuffer position on file close
            this.bytes.position(0);
        }
    }

    public long getPosition() {
        if (this.file != null) {
            return this.filePosition;
        } else if (this.bytes != null) {
            return this.bytes.position();
        } else {
            return -1;
        }
    }

    public void writeToFile(String outputFilePath) {
        FileWriter fileOutputStream;
        fileOutputStream = new FileWriter(new File(outputFilePath));
        ByteBuffer byteBuffer = this.toByteBuffer();
        fileOutputStream.write(byteBuffer.array());
        fileOutputStream.close();
    }
}
