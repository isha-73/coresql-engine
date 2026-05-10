package com.coresql.engine;

import com.coresql.ast.ColumnDefinition;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RowSerializer {

    public static byte[] serialize(List<String> values, List<ColumnDefinition> schema) {
        // Calculate size first
        int size = 0;
        for (int i = 0; i < schema.size(); i++) {
            String type = schema.get(i).type;
            String val = values.get(i);
            if (type.equals("INT") || type.equals("INTEGER")) {
                size += 4;
            } else {
                byte[] strBytes = val.getBytes(StandardCharsets.UTF_8);
                size += 2 + strBytes.length;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (int i = 0; i < schema.size(); i++) {
            String type = schema.get(i).type;
            String val = values.get(i);
            if (type.equals("INT") || type.equals("INTEGER")) {
                buffer.putInt(Integer.parseInt(val));
            } else {
                byte[] strBytes = val.getBytes(StandardCharsets.UTF_8);
                buffer.putShort((short) strBytes.length);
                buffer.put(strBytes);
            }
        }
        return buffer.array();
    }

    public static List<String> deserialize(byte[] data, List<ColumnDefinition> schema) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < schema.size(); i++) {
            String type = schema.get(i).type;
            if (type.equals("INT") || type.equals("INTEGER")) {
                values.add(String.valueOf(buffer.getInt()));
            } else {
                short len = buffer.getShort();
                byte[] strBytes = new byte[len];
                buffer.get(strBytes);
                values.add(new String(strBytes, StandardCharsets.UTF_8));
            }
        }
        return values;
    }
}
