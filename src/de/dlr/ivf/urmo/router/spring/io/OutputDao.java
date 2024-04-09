package de.dlr.ivf.urmo.router.spring.io;

import de.dlr.ivf.urmo.router.spring.model.Answer;
import de.dlr.ivf.urmo.router.spring.model.UrMoAcRequest;
import de.dlr.ivf.urmo.router.spring.model.UrMoAcResultResponse;

public interface OutputDao {
    void save(Answer answer);
    void save(UrMoAcResultResponse urMoAcResultResponse);

    void save(UrMoAcRequest urMoAcRequest);
}
