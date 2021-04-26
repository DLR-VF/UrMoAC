package de.dlr.ivf.urmo;

import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IntraTazComputer implements IDGiver {

    private long id;
    private CommandLine options;
    private BlockingQueue<Integer> filter_queue;
    private int thread_count;

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
        return ++this.id;
    }

    private void run(){



        if(options.hasOption("dropprevious")) {
            try {
                String[] r = Utils.checkDefinition(options.getOptionValue("ext-nm-output", ""), "ext-nm-output");

                Connection con = DriverManager.getConnection(r[1],r[3],r[4]);

                con.createStatement().execute("DROP TABLE IF EXISTS " +r[2]);
                con.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }


        }
        List<UrMoAccessibilityComputer> workers = IntStream.range(0,this.thread_count)
                                                           .mapToObj(i -> new UrMoAccessibilityComputer(this.net, options, modes, initMode, epsg))
                                                           .collect(Collectors.toList());

        Collection<Thread> worker_threads = workers.stream().map(worker -> new Thread(() -> worker.run(() -> this.filter_queue.poll()))).collect(Collectors.toList());
        worker_threads.forEach(Thread::start);

        worker_threads.forEach(worker -> {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

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

            this.thread_count = getNumThreads();


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
}
