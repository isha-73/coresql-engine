package com.coresql.engine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Page {
    public static final int PAGE_SIZE = 4096;
    public static final int HEADER_SIZE = 16;
    
    private final byte[] data;
    private final ByteBuffer buffer;
    private boolean isDirty = false;
    
    public Page(int pageId) {
        this.data = new byte[PAGE_SIZE];
        this.buffer = ByteBuffer.wrap(this.data);
        setPageId(pageId);
        setNumSlots(0);
        setFreeSpacePointer(PAGE_SIZE);
    }
    
    public Page(byte[] data) {
        if (data.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Page data must be exactly " + PAGE_SIZE + " bytes.");
        }
        this.data = data;
        this.buffer = ByteBuffer.wrap(this.data);
    }
    
    public byte[] getData() {
        return data;
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }
    
    public int getPageId() {
        return buffer.getInt(0);
    }
    
    public void setPageId(int pageId) {
        buffer.putInt(0, pageId);
    }
    
    public int getNumSlots() {
        return buffer.getShort(4);
    }
    
    public void setNumSlots(int numSlots) {
        buffer.putShort(4, (short) numSlots);
    }
    
    public int getFreeSpacePointer() {
        return buffer.getShort(6);
    }
    
    public void setFreeSpacePointer(int pointer) {
        buffer.putShort(6, (short) pointer);
    }
    
    public int getFreeSpace() {
        return getFreeSpacePointer() - (HEADER_SIZE + getNumSlots() * 4);
    }
    
    public boolean insertRow(byte[] rowData) {
        if (rowData.length + 4 > getFreeSpace()) {
            return false; // Not enough space
        }
        
        int numSlots = getNumSlots();
        int freeSpacePointer = getFreeSpacePointer();
        
        // Allocate space for row
        int newFreeSpacePointer = freeSpacePointer - rowData.length;
        
        // Write row data
        System.arraycopy(rowData, 0, data, newFreeSpacePointer, rowData.length);
        
        // Update free space pointer
        setFreeSpacePointer(newFreeSpacePointer);
        
        // Write slot (offset, length)
        int slotOffset = HEADER_SIZE + (numSlots * 4);
        buffer.putShort(slotOffset, (short) newFreeSpacePointer);
        buffer.putShort(slotOffset + 2, (short) rowData.length);
        
        // Update num slots
        setNumSlots(numSlots + 1);
        setDirty(true);
        
        return true;
    }
    
    public List<byte[]> readRows() {
        int numSlots = getNumSlots();
        List<byte[]> rows = new ArrayList<>();
        
        for (int i = 0; i < numSlots; i++) {
            int slotOffset = HEADER_SIZE + (i * 4);
            int offset = buffer.getShort(slotOffset);
            int length = buffer.getShort(slotOffset + 2);
            
            byte[] rowData = new byte[length];
            System.arraycopy(data, offset, rowData, 0, length);
            rows.add(rowData);
        }
        
        return rows;
    }
}
