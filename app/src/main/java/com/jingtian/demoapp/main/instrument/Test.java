package com.jingtian.demoapp.main.instrument;


import androidx.annotation.NonNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Test.TestAnnotation(value = {1, 2, 3, 4}, info = "class.Test", elementType = ElementType.TYPE)
public class Test {
    private int value;

    Test() {
        value = 0;
    }

    @Deprecated
    @Test.TestAnnotation(value = {2, 3, 4, 5, 6}, info = "method.setValue", elementType = ElementType.METHOD)
    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface TestAnnotation {
        int[] value();
        String info();

        ElementType elementType();
    }
}
