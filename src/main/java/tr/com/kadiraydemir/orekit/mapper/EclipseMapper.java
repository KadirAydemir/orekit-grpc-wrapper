package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.model.EclipseRequest;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface EclipseMapper {

    EclipseRequest toDTO(tr.com.kadiraydemir.orekit.grpc.EclipseRequest source);
}
