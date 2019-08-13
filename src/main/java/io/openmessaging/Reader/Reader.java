package io.openmessaging.Reader;

import io.openmessaging.Context;
import io.openmessaging.DataContext;
import io.openmessaging.Message;
import io.openmessaging.MessagePool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by huzhebin on 2019/08/07.
 */
public class Reader {
    private TimeReader timeReader;

    private ValueReader valueReader;

    private DataReader dataReader;

    private long msgNum;

    private ThreadLocal<Context> timeContextThreadLocal = new ThreadLocal<>();

    private ThreadLocal<Context> valueContextThreadLocal = new ThreadLocal<>();

    private ThreadLocal<DataContext> dataContextThreadLocal = new ThreadLocal<>();

    AtomicLong one = new AtomicLong();

    AtomicLong two = new AtomicLong();

    public Reader() {
        timeReader = new TimeReader();
        valueReader = new ValueReader();
        dataReader = new DataReader();
    }

    public void put(Message message) {
        timeReader.put(message);
        valueReader.put(message);
        dataReader.put(message);
        msgNum++;
    }

    public List<Message> get(long aMin, long aMax, long tMin, long tMax, MessagePool messagePool) {
        List<Message> result = new ArrayList<>();
        if (timeContextThreadLocal.get() == null) {
            timeContextThreadLocal.set(new Context());
        }
        Context timeContext = timeContextThreadLocal.get();
        if (valueContextThreadLocal.get() == null) {
            valueContextThreadLocal.set(new Context());
        }
        Context valueContext = valueContextThreadLocal.get();
        if (dataContextThreadLocal.get() == null) {
            dataContextThreadLocal.set(new DataContext());
        }
        DataContext dataContext = dataContextThreadLocal.get();
        int offsetA = timeReader.getOffset((int) tMin);
        while (offsetA < msgNum) {
            long time = timeReader.get(offsetA, timeContext);
            if (time > tMax) {
                return result;
            }
            long value = valueReader.get(offsetA, valueContext);
            if (value > aMax || value < aMin) {
                offsetA++;
                continue;
            }
            Message message = messagePool.get();
            message.setT(time);
            message.setA(value);
            dataReader.getData(offsetA, message, dataContext);
            result.add(message);
            offsetA++;
        }
        return result;
    }

    public long avg(long aMin, long aMax, long tMin, long tMax) {
        Context valueContext = valueContextThreadLocal.get();
        if (valueContext == null) {
            valueContext = new Context();
            valueContextThreadLocal.set(valueContext);
        }
        //long start = System.nanoTime();
        int offsetA = timeReader.getOffset((int) tMin);
        int offsetB = timeReader.getOffset((int) tMax + 1);
        //long mid = System.nanoTime();
        return valueReader.avg(offsetA, offsetB, aMin, aMax, valueContext);
        //long end = System.nanoTime();
        //System.out.println("one:" + one.addAndGet(mid - start) + " two:" + two.addAndGet(end - mid));
    }
}
