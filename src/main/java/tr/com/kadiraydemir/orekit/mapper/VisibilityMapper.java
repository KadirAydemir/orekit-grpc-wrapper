package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.model.GroundStation;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface VisibilityMapper {

    AccessIntervalsRequest toDTO(tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest source);

    GroundStation toDTO(tr.com.kadiraydemir.orekit.grpc.GroundStation source);
}
