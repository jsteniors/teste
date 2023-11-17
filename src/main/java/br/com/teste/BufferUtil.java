package br.com.teste;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferUtil {

    public static ByteBuffer concat(String ...buffers) {
        var bufs = Arrays.stream(buffers)
                .map(String::getBytes)
                .map(ByteBuffer::wrap)
                .toArray(ByteBuffer[]::new);
        return concat(bufs);
    }

    public static ByteBuffer concat(ByteBuffer ...buffers) {
        int overAllCapacity = 0;
        for (int i = 0; i < buffers.length; i++)
            overAllCapacity += buffers[i].limit() - buffers[i].position();
        //padding
        overAllCapacity += buffers[0].limit() - buffers[0].position();
        ByteBuffer all = ByteBuffer.allocateDirect(overAllCapacity);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer curr = buffers[i];
            all.put(curr);
        }

        all.flip();
        return all;
    }
}
