package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;

class ClassUtilsReadOnlyTest {
    @Test
    void resolvesSupportedReadOnlyContracts() {
        ReadOnlyBean bean = new ReadOnlyBean();
        assertTrue(ClassUtils.isObjectReadOnly(bean));
        assertTrue(ClassUtils.isObjectFieldReadOnly(bean, null));
    }

    @Test
    void missingOrFailingOptionalContractDefaultsToEditable() {
        assertFalse(ClassUtils.isObjectReadOnly(new Object()));
        assertFalse(ClassUtils.isObjectReadOnly(new ThrowingBean()));
        assertFalse(ClassUtils.isObjectFieldReadOnly(null, null));
    }

    public static final class ReadOnlyBean {
        public boolean isReadOnly() {
            return true;
        }

        public boolean isReadOnly(Field field) {
            return true;
        }
    }

    public static final class ThrowingBean {
        public boolean isReadOnly() {
            throw new IllegalStateException("unavailable");
        }
    }
}
