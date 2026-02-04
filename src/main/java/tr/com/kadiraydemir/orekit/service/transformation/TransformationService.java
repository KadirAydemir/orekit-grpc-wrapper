package tr.com.kadiraydemir.orekit.service.transformation;

import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.grpc.TransformRequest;
import tr.com.kadiraydemir.orekit.grpc.TransformResponse;

/**
 * Service for performing coordinate transformations
 */
public interface TransformationService {

    /**
     * Transform coordinates between two reference frames
     * 
     * @param request the transformation request
     * @return the transformation response
     */
    TransformResponse transform(TransformRequest request);
}
