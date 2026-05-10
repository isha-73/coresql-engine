package com.coresql.engine;

import com.coresql.ast.ColumnDefinition;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PageManager {
    public static final int HEADER_SIZE = 128;
    public static final int MAGIC_NUMBER = 0xC0DEC0DE;
    
    private final String tableName;
    private final File file;
    private RandomAccessFile raf;
    
    public PageManager(String tableName) {
        this.tableName = tableName;
        String tablesDirectory = "tables/";
        File dir = new File(tablesDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.file = new File(tablesDirectory + tableName + ".db");
    }
    
    public boolean exists() {
        return file.exists();
    }
    
    public void open() throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
    }
    
    public void close() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void initializeFile(List<ColumnDefinition> columns) throws IOException {
        open();
        raf.setLength(0); // clear if exists
        
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.putInt(MAGIC_NUMBER);
        header.putShort((short) 1); // version
        header.putInt(Page.PAGE_SIZE);
        header.putShort((short) columns.size()); // num_columns
        
        // schema block
        for (ColumnDefinition col : columns) {
            String colDef = col.name + ":" + col.type + ";";
            byte[] bytes = colDef.getBytes(StandardCharsets.UTF_8);
            header.put(bytes);
        }
        
        raf.seek(0);
        raf.write(header.array());
    }
    
    public List<ColumnDefinition> readSchema() throws IOException {
        if (raf == null) open();
        raf.seek(0);
        byte[] headerBytes = new byte[HEADER_SIZE];
        raf.readFully(headerBytes);
        
        ByteBuffer header = ByteBuffer.wrap(headerBytes);
        int magic = header.getInt();
        if (magic != MAGIC_NUMBER) {
            throw new IOException("Invalid file format");
        }
        
        header.getShort(); // version
        header.getInt(); // page size
        int numColumns = header.getShort();
        
        List<ColumnDefinition> columns = new ArrayList<>();
        // read remaining bytes until we parsed numColumns
        StringBuilder current = new StringBuilder();
        int colsParsed = 0;
        while (header.hasRemaining() && colsParsed < numColumns) {
            char c = (char) header.get();
            if (c == ';') {
                String[] parts = current.toString().split(":");
                columns.add(new ColumnDefinition(parts[0], parts[1]));
                current.setLength(0);
                colsParsed++;
            } else {
                current.append(c);
            }
        }
        
        return columns;
    }
    
    public Page readPage(int pageId) throws IOException {
        if (raf == null) open();
        long offset = HEADER_SIZE + (long) pageId * Page.PAGE_SIZE;
        raf.seek(offset);
        
        byte[] pageData = new byte[Page.PAGE_SIZE];
        int bytesRead = raf.read(pageData);
        if (bytesRead <= 0) {
            return new Page(pageId);
        } else if (bytesRead < Page.PAGE_SIZE) {
            throw new IOException("Corrupted page: " + pageId);
        }
        return new Page(pageData);
    }
    
    public void writePage(Page page) throws IOException {
        if (raf == null) open();
        long offset = HEADER_SIZE + (long) page.getPageId() * Page.PAGE_SIZE;
        raf.seek(offset);
        raf.write(page.getData());
        page.setDirty(false);
    }
    
    public int getNumPages() throws IOException {
        if (raf == null) open();
        long length = raf.length();
        if (length <= HEADER_SIZE) return 0;
        return (int) ((length - HEADER_SIZE) / Page.PAGE_SIZE);
    }
}
