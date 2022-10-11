package de.dlr.ivf.urmo.router.spring.controller;

import de.dlr.ivf.urmo.router.spring.model.UrMoAcResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ForkJoinPool;

@RestController
public class RoutingController {

    @GetMapping("/route-request")
    public DeferredResult<UrMoAcResultResponse> handle(@RequestParam(value = "lon1") double lon1, @RequestParam(value = "lat1") double lat1, @RequestParam(value = "lon2") double lon2, @RequestParam(value = "lat2") double lat2){

        DeferredResult<UrMoAcResultResponse> result = new DeferredResult<>();
        ForkJoinPool.commonPool().submit(() -> result.setResult(new UrMoAcResultResponse("ferry", 1000,100, 10)));

        return result;
    }

}
