package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IntegratorTestMapper {
    @ValueMapping(source = "UNRECOGNIZED", target = "DORMAND_PRINCE_853")
    tr.com.kadiraydemir.orekit.model.IntegratorType map(IntegratorType source);
}
