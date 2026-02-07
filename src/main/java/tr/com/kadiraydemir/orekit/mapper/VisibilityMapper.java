package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.grpc.AccessInterval;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsResponse;
import tr.com.kadiraydemir.orekit.model.AccessIntervalResult;
import tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.model.GroundStation;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;

import java.util.List;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface VisibilityMapper {

    AccessIntervalsRequest toDTO(tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest source);

    GroundStation toDTO(tr.com.kadiraydemir.orekit.grpc.GroundStation source);

    @Mapping(target = "intervalsList", source = "intervals")
    AccessIntervalsResponse map(VisibilityResult source);

    AccessInterval map(AccessIntervalResult source);

    List<AccessInterval> mapIntervalList(List<AccessIntervalResult> source);
}
