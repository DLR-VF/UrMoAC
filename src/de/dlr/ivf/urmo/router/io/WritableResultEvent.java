package de.dlr.ivf.urmo.router.io;

import de.dlr.ivf.urmo.router.output.odext.ODSingleExtendedResult;

import java.util.List;
import java.util.stream.Collectors;

public class WritableResultEvent {

    public WritableResultEvent(){}

    private byte[] result_byte_array;

    public byte[] getWritableResult(){
        return this.result_byte_array;
    }

    public void setWritableResults(List<ODSingleExtendedResult> results){

        this.result_byte_array = results.stream().map(ODSingleExtendedResult::asCSV).collect(Collectors.joining()).getBytes();

    }

    public void clear(){
        this.result_byte_array = null;
    }

    public void setEmptyResult(){
        this.result_byte_array = new byte[0];
    }
}
