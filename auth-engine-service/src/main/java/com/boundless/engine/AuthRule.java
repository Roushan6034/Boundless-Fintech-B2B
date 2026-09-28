package com.boundless.engine;

import com.boundless.model.SwipeRequest;

// The base interface for our Chain of Responsibility
public interface AuthRule {
    // Evaluates the swipe. Throws a RuntimeException if the swipe is declined.
    void evaluate(SwipeRequest request);
}
