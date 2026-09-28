package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class MccRule implements AuthRule {
    
    // 7995 = Betting/Casino, 5813 = Drinking Places/Bars
    private final Set<String> BLOCKED_MCCS = Set.of("7995", "5813");

    @Override
    public void evaluate(SwipeRequest request) {
        if (BLOCKED_MCCS.contains(request.getMcc())) {
            throw new RuntimeException("DECLINED: Restricted Merchant Category (MCC: " + request.getMcc() + ")");
        }
    }
}
