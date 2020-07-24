package com.lee.chat.box;

import com.lee.chat.core.SendPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileSendPacket extends SendPacket<FileInputStream> {

    private final File file;

    public FileSendPacket(File file) {
        this.file = file;
        this.length = file.length();
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    @Override
    protected FileInputStream createStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

}
