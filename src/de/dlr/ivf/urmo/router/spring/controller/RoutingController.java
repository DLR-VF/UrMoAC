package de.dlr.ivf.urmo.router.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import de.dlr.ivf.urmo.router.spring.io.implementation.FileOutput;
import de.dlr.ivf.urmo.router.spring.mode.ModeService;
import de.dlr.ivf.urmo.router.spring.routing.RoutingResult;
import de.dlr.ivf.urmo.router.spring.routing.RoutingService;
import de.dlr.ivf.urmo.router.spring.io.OutputDao;
import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
public class RoutingController {

    private AtomicLong requestIdSupplier = new AtomicLong(0);

    private OutputDao writer;

    @Autowired
    RoutingService routingService;

    @Autowired
    ModeService modeService;

    private final int ferryFrequencyMinute = 20;
    private ObjectMapper objectMapper;



    public RoutingController() throws FactoryException {
    }

    @GetMapping("route-request")
    public DeferredResult<UrMoAcResultResponse> handle(@RequestParam(value = "lon1") double lon1, @RequestParam(value = "lat1") double lat1,
                                                       @RequestParam(value = "lon2") double lon2, @RequestParam(value = "lat2") double lat2,
                                                       @RequestParam(value = "uuid") String uuid, @RequestParam(value = "startTime") String startTime) {

        Coordinate startCoordinate = new Coordinate(lon1, lat1);
        Coordinate endCoordinate = new Coordinate(lon2, lat2);

        DeferredResult<UrMoAcResultResponse> output = new DeferredResult<>();
        ForkJoinPool.commonPool().submit(() -> {
            long requestId = requestIdSupplier.incrementAndGet();
            LocalDateTime time = LocalDateTime.parse(startTime);

            UrMoAcRequest request = new UrMoAcRequest(uuid,requestId,startTime,lon1,lat1,lon2,lat2);
            writer.save(request);

            AlternativeMode alternativeMode = modeService.generateRandom(startCoordinate, endCoordinate);
            Optional<Map<TravelStage, RoutingResult>> alternativeRoute = routingService.generateRoute(startCoordinate, endCoordinate, alternativeMode);

            UrMoAcResultResponse response;
            if(alternativeRoute.isPresent()){
                var result = alternativeRoute.get();
                response = generateUrMoAcResponse(result, uuid, requestId, time);
            }else{
                response = newEmptyResult(uuid,requestId,time);
            }

            output.setResult(response);
            writer.save(response);
        });
        return output;
    }

    private UrMoAcResultResponse generateUrMoAcResponse(Map<TravelStage, RoutingResult> alternativeRoute,
                                                        String uuid, long requestId, LocalDateTime time) {
        RoutingResult access = alternativeRoute.get(TravelStage.ACCESS);
        RoutingResult egress = alternativeRoute.get(TravelStage.EGRESS);
        RoutingResult main = alternativeRoute.get(TravelStage.MAIN);

        int timeAddonOn = switch (main.mode()){

            case FOOT -> 0;
            case FERRY -> (ferryFrequencyMinute - (time.plusSeconds((long) access.travelTime()).getMinute()) %20) * 60;
            case CALLABUS -> ThreadLocalRandom.current().nextInt(5,21) * 60;
            case BIKESHARING -> ThreadLocalRandom.current().nextInt(5,15) * 60;
            case CARPOOLING -> ThreadLocalRandom.current().nextInt(5,16) * 60;
        };

        TravelTime travelTime = new TravelTime((int) access.travelTime(), (int) main.travelTime(), (int) egress.travelTime());
        int totalTravelTime = travelTime.getAccessTime() + travelTime.getEgressTime() + travelTime.getTravelTime() + timeAddonOn;

        double co2 = 0;
        double kcal = 0;
        double prize = 0;
        for(RoutingResult routingResult : alternativeRoute.values()){
            double dist = routingResult.distance();
            co2 += dist * routingResult.mode().getCo2PerKm() / 1000;
            kcal += travelTime.getTravelTime() * routingResult.mode().getKkcPerHour() / 3600;
            prize += dist * routingResult.mode().getPricePerKm() / 1000;
        }
        return new UrMoAcResultResponse(uuid, requestId, time.toString(), main.mode().getTranslatedName(),
                travelTime, totalTravelTime, co2, (int) kcal, prize);
    }

    @PostMapping(value = "answer", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> addAnswer(HttpServletRequest servletRequest) throws ServletException, IOException {

        //this is a workaround. Can't convert to Answer from URLENCODED_VALUE
        String body = servletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Answer answer = this.objectMapper.readValue(body, Answer.class);
        writer.save(answer);
        return ResponseEntity.ok("Answer added.");
    }

    private UrMoAcResultResponse newEmptyResult(String uuid, long requestId, LocalDateTime startTime){
        return new UrMoAcResultResponse(uuid,requestId,startTime.toString(),
                "", new TravelTime(-1,-1,-1),0,0,0,0);
    }

    @PostConstruct
    public void init() throws IOException {

        this.objectMapper = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(timeModule);
        this.writer = new FileOutput(8192, objectMapper);
    }
}