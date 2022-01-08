package org.yschwartz.sdp.common.util;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionalUtil {
    public static <R, T> R getOrCreate(T object, Function<T, R> getMethod, BiConsumer<T, R> setMethod, Supplier<R> initMethod) {
        return Optional.ofNullable(getMethod.apply(object)).orElseGet(() -> {
            var returnValue = initMethod.get();
            setMethod.accept(object, returnValue);
            return returnValue;
        });
    }
}
