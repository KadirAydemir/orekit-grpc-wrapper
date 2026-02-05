package tr.com.kadiraydemir.orekit.service.transformation;

import tr.com.kadiraydemir.orekit.model.TransformRequestDTO;
import tr.com.kadiraydemir.orekit.model.TransformResult;

/**
 * Service for performing coordinate transformations
 */
public interface TransformationService {

    /**
     * Transform coordinates between two reference frames
     * 
     * @param request the transformation request
     * @return the transformation result
     */
    TransformResult transform(TransformRequestDTO request);
}
