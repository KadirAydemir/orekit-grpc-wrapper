package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
import tr.com.kadiraydemir.orekit.model.EclipseRequestDTO;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EclipseTestMapper {
    EclipseRequestDTO toDTO(EclipseRequest source);
}
