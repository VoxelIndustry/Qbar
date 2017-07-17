package net.qbar.common.ore;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

@Getter
@Builder
public class QBarOre
{
    private String                  name;
    @Singular
    private Map<QBarMineral, Float> minerals;

    public SludgeData toSludge()
    {
        return SludgeData.builder().ores(minerals).build();
    }
}
