package de.dlr.ivf.urmo;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import de.dlr.ivf.urmo.router.io.UrmoPipedWriter;
import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IntraTazComputer implements IDGiver {

    private AtomicLong id = new AtomicLong(0);
    private CommandLine options;
    private BlockingQueue<Integer> filter_queue;
    private int thread_count;
    private int total_taz_count;

    /// @brief Allowed modes
    long modes = -1;
    /// @brief Initial mode
    long initMode = -1;

    int epsg = -1;
    private DBNet net;


    public IntraTazComputer(CommandLine options){
        this.options = options;

    }


    public static void main(String... args){

        CommandLine options = UrMoAccessibilityComputer.getCMDOptions(args);

        IntraTazComputer computer = new IntraTazComputer(options);

        if(computer.init())
            computer.run();
        else
            System.out.println("An error occurred during initialization");

        System.out.println("Done...");

    }

    private int getNumThreads(){

        try {
            return options.hasOption("threads") ? ((Long) options.getParsedOptionValue("threads")).intValue() : 1;
        } catch (ParseException e) {
            e.printStackTrace();
            return 1;
        }
    }

    @Override
    public long getNextRunningID() {
        return this.id.incrementAndGet();
    }

    private void run(){

        //generate the R-Tree
        //NearestEdgeFinder nef = new NearestEdgeFinder(net, initMode);


        Supplier<Integer> taz_supplier = () -> filter_queue.poll();

        //init some variables from options
        int time = getTime();
        int maxNumber = getMaxNumber();
        double maxTT = getMaxTT();
        double maxDistance = getMaxDistance();
        double maxVar = getMaxVar();
        boolean shortestOnly = isShortest();

        //set up and start the results writer
        UrmoPipedWriter writer = initWriter(options);
        Thread writer_thread = new Thread(writer);
        writer_thread.start();

        //construct and start the worker threads
        System.out.println("Initializing worker threads...");
        List<UrmoWorker> workers = IntStream.range(0,this.thread_count)
                                            .mapToObj(i -> new UrmoWorker("Rechenknecht #"+i,writer,taz_supplier,new NearestEdgeFinder(net, initMode),options,epsg,this,time,initMode,modes,maxNumber,maxTT,maxDistance,maxVar,shortestOnly))
                                            .collect(Collectors.toList());

        Collection<Thread> worker_threads = workers.stream()
                                                   .map(Thread::new)
                                                   .collect(Collectors.toList());
        LocalDateTime start = LocalDateTime.now();

        worker_threads.forEach(Thread::start);

        worker_threads.forEach(worker -> {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Computations done in: "+ Duration.between(start,LocalDateTime.now()).toHours());

        //all workers are done, signal last element to writer and wait for it to finish
        writer.finish();
        try {
            writer_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Call it a day");
    }

    private boolean isShortest() {
        return options.hasOption("shortest");
    }

    private double getMaxVar() {
        try {
            return options.hasOption("max-variable-sum") ? ((Double) options.getParsedOptionValue("max-variable-sum")).doubleValue() : -1;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getMaxDistance() {
        try {
            return options.hasOption("max-distance") ? ((Double) options.getParsedOptionValue("max-distance")).doubleValue() : -1;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getMaxTT() {
        try {
            return options.hasOption("max-tt") ? ((Double) options.getParsedOptionValue("max-tt")).doubleValue() : -1;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getMaxNumber() {
        try {
            return options.hasOption("max-number") ? ((Long) options.getParsedOptionValue("max-number")).intValue() : -1;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getTime(){
        try {
            return ((Long) options.getParsedOptionValue("time")).intValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private UrmoPipedWriter initWriter(CommandLine options) {

        try {
            String[] r = Utils.checkDefinition(options.getOptionValue("ext-nm-output", ""), "ext-nm-output");

            if(!r[0].equals("db"))
                throw new RuntimeException("This version only supports extended output to database...");

            UrmoPipedWriter writer = new UrmoPipedWriter(r[1],r[3],r[4],r[2]);
            writer.init(options.hasOption("dropprevious"), 1<<19);

            return writer;

        } catch (IOException e) {
            throw new RuntimeException("Could not initialize the output writer... ",e.getCause());
        }

    }

    private boolean init(){
            boolean proceed = true;
            List<Integer> filters = initFilters();


            proceed &= filters.size() > 0;

            this.filter_queue = filters.stream().collect(Collectors.toCollection(LinkedBlockingQueue::new));

            //load modes
            proceed &= initMode();

            //check epsg
            proceed &= initEPSG();


            //load net
            proceed &= loadNet();

            this.thread_count = Math.max(getNumThreads() - 1,1);


        return proceed;
    }

    private List<Integer> initFilters() {

        List<Integer> filters = new ArrayList<>();
        String[] r = new String[0];
        try {
            r = Utils.checkDefinition(options.getOptionValue("from", ""), "from");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (r[0].equals("db")) {

            String url = r[1];
            String table = r[2];
            String user = r[3];
            String pw = r[4];

            //load filters
            String query = "SELECT wq_gid, COUNT(*) cnt FROM " + table + " WHERE geocode_key = 'GEOCODES_2017' GROUP BY wq_gid ORDER BY cnt desc";

            try (Connection connection = DriverManager.getConnection(url, user, pw);
                 ResultSet rs = connection.prepareStatement(query).executeQuery()) {

                while (rs.next())
                    filters.add(rs.getInt("wq_gid"));

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return filters;
    }

    private boolean initEPSG(){

        if(options.hasOption("epsg")) {
            try {
                epsg = ((Long) options.getParsedOptionValue("epsg")).intValue();
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            if(epsg==-1) {
                System.out.println("Could not find a valid UTM-zone. Quitting");
                return false;
            }
        }

        return true;
    }

    private boolean loadNet(){

        System.out.println("Reading the road network");
        try {
            net = NetLoader.loadNet(this, options.getOptionValue("net", ""), epsg, modes);
        } catch (com.vividsolutions.jts.io.ParseException | IOException | SQLException e) {
            e.printStackTrace();
            return false;
        }

        System.out.println(" " + net.getNumEdges() + " edges loaded (" + net.getNodes().size() + " nodes)");
        net.pruneForModes(modes); // TODO (implement, add message)
        if(!options.hasOption("subnets")) {
            System.out.println("Checking for connectivity...");
            net.dismissUnconnectedEdges(false);
            System.out.println(" " + net.getNumEdges() + " remaining after removing unconnected ones.");
        }

        return true;
    }

    private boolean initMode(){

        Modes.init();
        Vector<Mode> modesV = UrMoAccessibilityComputer.getModes(options.getOptionValue("mode", "<unknown>"));
        if (modesV == null)
            return false;

        modes = Modes.getCombinedModeIDs(modesV);
        initMode = modesV.get(0).id;
        return true;
    }

    public int getRemainingCount(){
        return filter_queue.size();
    }

    public int getTotalTazCount(){
        return this.total_taz_count;
    }
}
