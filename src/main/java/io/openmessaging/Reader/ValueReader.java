package io.openmessaging.Reader;

import io.openmessaging.HalfByte;
import io.openmessaging.Message;
import io.openmessaging.Tags;

import java.nio.ByteBuffer;

/**
 * Created by huzhebin on 2019/08/07.
 */
public class ValueReader {
    private long max = 0;

    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Integer.MAX_VALUE / 2);

    private Tags valueTags = new Tags(2000000);

    private int msgNum = 0;

    private HalfByte halfByte = new HalfByte((byte) 0);

    private volatile boolean init = false;

    private ThreadLocal<Integer> tag = new ThreadLocal<>();

    private ThreadLocal<Integer> offsetA = new ThreadLocal<>();

    private ThreadLocal<Integer> offsetB = new ThreadLocal<>();

    public void put(Message message) {
        long v = message.getA() - message.getT();
        if (v > max) {
            max = v;
        }
        int value = (int) v;
        if (tag.get() == null || value > tag.get() + 15 || value < tag.get()) {
            tag.set(value);
            valueTags.add(value,msgNum);
        }
        if (msgNum % 2 == 0) {
            halfByte.setRight((byte) (value - tag.get()));
        } else {
            halfByte.setLeft((byte) (value - tag.get()));
            byteBuffer.put(msgNum / 2, halfByte.getByte());
            halfByte.setByte((byte) 0);
        }
        msgNum++;
    }

    public void init() {
        byteBuffer.put(msgNum / 2, halfByte.getByte());
        tag.set(0);
        halfByte.setByte((byte) 0);
        System.out.println("value max:" + max + " valueTags size:" + valueTags.size());
        init = true;
    }

    public int get(int offset) {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    init();
                }
            }
        }
        if (offsetA.get() == null) {
            offsetA.set(0);
        }
        if (offsetB.get() == null) {
            offsetB.set(0);
        }

        if (offset < offsetA.get() || offset >= offsetB.get()) {
            int tagIndex = valueTags.offsetIndex(offset);
            tag.set(valueTags.getTag(tagIndex));
            offsetA.set(valueTags.getOffset(tagIndex));
            if (tagIndex == valueTags.size() - 1) {
                offsetB.set(msgNum);
            } else {
                offsetB.set(valueTags.getOffset(tagIndex + 1));
            }
        }
        if (offset % 2 == 0) {
            return tag.get() + HalfByte.getRight(byteBuffer.get(offset / 2));
        } else {
            return tag.get() + HalfByte.getLeft(byteBuffer.get(offset / 2));
        }
    }
}
