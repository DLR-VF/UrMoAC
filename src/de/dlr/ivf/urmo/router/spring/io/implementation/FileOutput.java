package de.dlr.ivf.urmo.router.spring.io.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.urmo.router.spring.io.OutputDao;
import de.dlr.ivf.urmo.router.spring.model.Answer;
import de.dlr.ivf.urmo.router.spring.model.UrMoAcRequest;
import de.dlr.ivf.urmo.router.spring.model.UrMoAcResultResponse;

import java.io.IOException;

public class FileOutput implements OutputDao {

    //handles writing all events from the 'answer'-post endpoint to a file.
    private final RingBuffer<StringEvent> answerWriter;
    private final Disruptor<StringEvent> answerDisruptor;

    //handles writing all generated alternative routes send back from the 'route-request'-get endpoint to a file.
    private final RingBuffer<StringEvent> responseWriter;
    private final Disruptor<StringEvent> responseDisruptor;

    //handles writing all incoming requests at the 'route-request'-get endpoint to a file.
    private final RingBuffer<StringEvent> requestWriter;
    private final Disruptor<StringEvent> requestDisruptor;
    private final ObjectMapper objectMapper;

    public FileOutput(int ringBufferSize, ObjectMapper objectMapper) throws IOException {

        this.objectMapper = objectMapper;

        EventFactory<StringEvent> stringEventEventFactory = StringEvent::new;

        //first set up the different event handlers
        EventHandler<StringEvent> responseHandler = new FileWritingEventHandler("./response","response");
        EventHandler<StringEvent> requestHandler = new FileWritingEventHandler("./request","request");
        EventHandler<StringEvent> answerHandler = new FileWritingEventHandler("./answer", "answer");

        //initialize all disruptor instances
        this.responseDisruptor = newDisruptor(ringBufferSize, stringEventEventFactory, responseHandler);
        this.requestDisruptor = newDisruptor(ringBufferSize, stringEventEventFactory, requestHandler);
        this.answerDisruptor = newDisruptor(ringBufferSize, stringEventEventFactory, answerHandler);

        //initialize all ring buffers
        this.responseWriter = responseDisruptor.getRingBuffer();
        this.requestWriter = requestDisruptor.getRingBuffer();
        this.answerWriter = answerDisruptor.getRingBuffer();

        //start all disruptor instances
        this.responseDisruptor.start();
        this.requestDisruptor.start();
        this.answerDisruptor.start();
    }

    private Disruptor<StringEvent> newDisruptor(int ringBufferSize, EventFactory<StringEvent> stringEventEventFactory, EventHandler<StringEvent> eventHandler) {
        Disruptor<StringEvent> disruptor = new Disruptor<>(stringEventEventFactory,ringBufferSize, DaemonThreadFactory.INSTANCE, ProducerType.MULTI,new BlockingWaitStrategy());
        disruptor.handleEventsWith(eventHandler);
        return disruptor;
    }

    @Override
    public void save(Answer answer) {
        this.answerWriter.publishEvent((event, sequence, buffer) -> {
            try {
                event.setValue(objectMapper.writeValueAsString(answer));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void save(UrMoAcResultResponse urMoAcResultResponse) {
        this.responseWriter.publishEvent((event, sequence, buffer) -> {
            try {
                event.setValue(objectMapper.writeValueAsString(urMoAcResultResponse));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void save(UrMoAcRequest urMoAcRequest) {
        this.requestWriter.publishEvent((event, sequence, buffer) -> {
            try {
                event.setValue(objectMapper.writeValueAsString(urMoAcRequest));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
