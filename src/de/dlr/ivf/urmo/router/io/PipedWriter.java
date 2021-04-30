package de.dlr.ivf.urmo.router.io;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import de.dlr.ivf.urmo.router.output.odext.ODSingleExtendedResult;
import org.postgresql.core.BaseConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class PipedWriter implements Runnable{


    private AtomicInteger registered_result = new AtomicInteger(0);
    private AtomicInteger written_results = new AtomicInteger(0);


    private Connection connection;

    private final String copy_string;

    private final String table_name;

    private FastCopyManager copy_manager;

    private RingBuffer<WritableResultEvent> ring_buffer;

    public PipedWriter(String url, String user, String pw, String table_name){
        super();


        this.table_name = table_name;

        this.copy_string = String.format("COPY %s FROM STDIN (FORMAT TEXT, ENCODING 'UTF-8', DELIMITER ';', HEADER false)",table_name);

        try {
            this.connection = DriverManager.getConnection(url, user,pw);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void init(boolean drop_previous, int buffer_size){

        String tableDef = "(fid bigint, sid bigint, avg_distance real, avg_tt real, avg_v real, avg_num real, avg_value real, "
                + "avg_kcal real, avg_price real, avg_co2 real, avg_interchanges real, avg_access real, avg_egress real, "
                + "avg_waiting_time real, avg_init_waiting_time real, avg_pt_tt real, avg_pt_interchange_time real, modes text)";


        String table_deletion_query   = "DROP TABLE IF EXISTS " +table_name;
        String table_generation_query = "CREATE TABLE IF NOT EXISTS " + table_name + " " + tableDef + ";";

        try{

            if(drop_previous)
                this.connection.createStatement().execute(table_deletion_query);

            this.connection.createStatement().executeUpdate(table_generation_query);

            this.copy_manager = new FastCopyManager((BaseConnection) this.connection);

            EventFactory<WritableResultEvent> ef = WritableResultEvent::new;

            this.ring_buffer = RingBuffer.createMultiProducer(ef, buffer_size, new BusySpinWaitStrategy());

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void writeResults(List<ODSingleExtendedResult> results){

        long sequenceId = ring_buffer.next();

        WritableResultEvent event = ring_buffer.get(sequenceId);
        event.setWritableResults(results);

        ring_buffer.publish(sequenceId);
        written_results.incrementAndGet();

    }

    public void finish(){

        //put an empty byte array onto the ring to signal the copy manager that the transaction is over
        long sequenceId = ring_buffer.next();
        WritableResultEvent event = ring_buffer.get(sequenceId);
        event.setEmptyResult();
        ring_buffer.publish(sequenceId);
    }

    @Override
    public void run() {
        try {
            if(written_results.get() % 100000 == 0)
                System.out.println("-- Remaining capacity on the ring: "+ring_buffer.remainingCapacity()+ " | Total written results: "+written_results.get());

            System.out.println("Opening database persistence pipeline...");
            copy_manager.copyIn(copy_string,this.ring_buffer,written_results);
            System.out.println("Closing database persistence pipeline and shutting down the writer...");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
