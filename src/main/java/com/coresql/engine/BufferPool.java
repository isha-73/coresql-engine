package com.coresql.engine;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BufferPool {
    private final int capacity;
    private final PageManager pageManager;
    private final Map<Integer, Page> cache;
    
    public BufferPool(PageManager pageManager, int capacity) {
        this.pageManager = pageManager;
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Page> eldest) {
                if (size() > BufferPool.this.capacity) {
                    flushPage(eldest.getValue());
                    return true;
                }
                return false;
            }
        };
    }
    
    public Page getPage(int pageId) throws IOException {
        Page page = cache.get(pageId);
        if (page == null) {
            page = pageManager.readPage(pageId);
            cache.put(pageId, page);
        }
        return page;
    }
    
    public void flushPage(Page page) {
        if (page.isDirty()) {
            try {
                pageManager.writePage(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void flushAll() {
        for (Page page : cache.values()) {
            flushPage(page);
        }
    }
}
