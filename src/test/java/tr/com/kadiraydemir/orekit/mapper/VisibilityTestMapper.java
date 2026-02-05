package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.grpc.GroundStation;
import tr.com.kadiraydemir.orekit.model.AccessIntervalsRequestDTO;
import tr.com.kadiraydemir.orekit.model.GroundStationDTO;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VisibilityTestMapper {
    AccessIntervalsRequestDTO toDTO(AccessIntervalsRequest source);
    GroundStationDTO toDTO(GroundStation source);
}
