package de.dlr.ivf.urmo.router.io;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FastCopyManager extends CopyManager {
    public FastCopyManager(BaseConnection connection) throws SQLException {
        super(connection);
    }

    public long copyIn(final String sql, RingBuffer<WritableResultEvent> ring_buffer, AtomicInteger counter)
            throws SQLException {

        AtomicBoolean keep_running = new AtomicBoolean(true);
        CopyIn cp = copyIn(sql);

        final EventPoller<WritableResultEvent> poller = ring_buffer.newPoller();
        ring_buffer.addGatingSequences(poller.getSequence());

        final EventPoller.Handler<WritableResultEvent> handler = (event, sequence, endOfBatch) -> {

            byte[] writable_result = event.getWritableResult();

            if (writable_result.length == 0){
                keep_running.set(false);
            }else{
                counter.getAndIncrement();
            }
            cp.writeToCopy(writable_result, 0, writable_result.length);
            event.clear();
            return true;
        };

        try {
            while (keep_running.get()) {
                poller.poll(handler);
            }
            return cp.endCopy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // see to it that we do not leave the connection locked
            if (cp.isActive()) {
                cp.cancelCopy();
            }

        }
        return cp.endCopy();
    }
}
