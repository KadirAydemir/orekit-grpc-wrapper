package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.model.EclipseRequest;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EclipseTestMapper {
    EclipseRequest toDTO(tr.com.kadiraydemir.orekit.grpc.EclipseRequest source);
}
