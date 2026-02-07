package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.grpc.EclipseInterval;
import tr.com.kadiraydemir.orekit.grpc.EclipseResponse;
import tr.com.kadiraydemir.orekit.model.EclipseIntervalResult;
import tr.com.kadiraydemir.orekit.model.EclipseRequest;
import tr.com.kadiraydemir.orekit.model.EclipseResult;

import java.util.List;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface EclipseMapper {

    EclipseRequest toDTO(tr.com.kadiraydemir.orekit.grpc.EclipseRequest source);

    @Mapping(target = "intervalsList", source = "intervals")
    EclipseResponse map(EclipseResult source);

    EclipseInterval map(EclipseIntervalResult source);

    List<EclipseInterval> mapIntervalList(List<EclipseIntervalResult> source);
}
